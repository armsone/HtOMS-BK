package com.htoms.brief.update

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.htoms.brief.BuildConfig
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

enum class UpdatePhase {
    IDLE, CHECKING, CURRENT, AVAILABLE, DOWNLOADING, VERIFYING, READY, PERMISSION_REQUIRED, ERROR
}

data class AppUpdateState(
    val phase: UpdatePhase = UpdatePhase.IDLE,
    val message: String = "업데이트를 확인할 수 있습니다.",
    val release: UpdateRelease? = null,
    val progressPercent: Int? = null,
    val automaticallyDownloads: Boolean = true
)

class AppUpdateManager(
    context: Context,
    private val scope: CoroutineScope
) {
    private val appContext = context.applicationContext
    private val downloadManager = appContext.getSystemService(DownloadManager::class.java)
    private val preferences = appContext.getSharedPreferences("app_updates", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }
    private val _state = MutableStateFlow(
        AppUpdateState(automaticallyDownloads = preferences.getBoolean(AUTO_DOWNLOAD_KEY, true))
    )
    val state: StateFlow<AppUpdateState> = _state.asStateFlow()

    private var activeDownloadId: Long? = null
    private var progressJob: Job? = null

    private val downloadReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val completedId = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L) ?: return
            if (completedId == activeDownloadId) scope.launch { finishDownload(completedId) }
        }
    }

    init {
        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        ContextCompat.registerReceiver(
            appContext,
            downloadReceiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED
        )
        recoverActiveDownload()
    }

    fun setAutomaticallyDownloads(enabled: Boolean) {
        preferences.edit().putBoolean(AUTO_DOWNLOAD_KEY, enabled).apply()
        _state.value = _state.value.copy(
            automaticallyDownloads = enabled,
            message = if (enabled) {
                "새 버전은 Wi-Fi 같은 무과금 네트워크에서 자동 다운로드합니다."
            } else {
                "새 버전을 확인한 뒤 직접 다운로드합니다."
            }
        )
    }

    fun checkForUpdates(manual: Boolean) {
        if (_state.value.phase == UpdatePhase.CHECKING || activeDownloadId != null) return
        scope.launch {
            _state.value = _state.value.copy(
                phase = UpdatePhase.CHECKING,
                message = "업데이트 확인 중…",
                progressPercent = null
            )
            runCatching { fetchLatestRelease() }
                .onSuccess { release ->
                    if (release == null) {
                        _state.value = _state.value.copy(
                            phase = UpdatePhase.CURRENT,
                            message = "최신 버전을 사용 중입니다.",
                            release = null
                        )
                    } else {
                        _state.value = _state.value.copy(
                            phase = UpdatePhase.AVAILABLE,
                            message = "새 버전 ${release.versionName} · ${formatBytes(release.size)}",
                            release = release
                        )
                        if (!manual && _state.value.automaticallyDownloads) {
                            download(release, allowMetered = false)
                        }
                    }
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(
                        phase = UpdatePhase.ERROR,
                        message = error.message ?: "업데이트 확인에 실패했습니다. 다시 시도해 주세요."
                    )
                }
        }
    }

    fun downloadManually() {
        val release = _state.value.release ?: return
        download(release, allowMetered = true)
    }

    fun cancelDownload() {
        activeDownloadId?.let { downloadManager.remove(it) }
        activeDownloadId = null
        progressJob?.cancel()
        clearPersistedDownload()
        _state.value.release?.let { File(downloadDirectory(), it.assetName).delete() }
        _state.value = _state.value.copy(
            phase = UpdatePhase.AVAILABLE,
            message = "다운로드를 취소했습니다. 다시 다운로드할 수 있습니다.",
            progressPercent = null
        )
    }

    fun retry() {
        val release = _state.value.release
        if (release == null) checkForUpdates(manual = true) else download(release, allowMetered = true)
    }

    fun install() {
        val id = activeDownloadId ?: preferences.getLong(DOWNLOAD_ID_KEY, -1L).takeIf { it >= 0 } ?: return
        if (Build.VERSION.SDK_INT >= 26 && !appContext.packageManager.canRequestPackageInstalls()) {
            _state.value = _state.value.copy(
                phase = UpdatePhase.PERMISSION_REQUIRED,
                message = "이 앱의 업데이트 설치를 허용한 뒤 설치를 다시 눌러 주세요."
            )
            val permissionIntent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${appContext.packageName}")
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { appContext.startActivity(permissionIntent) }.onFailure {
                fail("설치 허용 화면을 열지 못했습니다. 기기 설정에서 이 앱의 설치 권한을 확인해 주세요.")
            }
            return
        }
        val uri = downloadManager.getUriForDownloadedFile(id) ?: run {
            fail("다운로드한 APK를 찾을 수 없습니다. 다시 다운로드해 주세요.")
            return
        }
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        runCatching { appContext.startActivity(intent) }.onFailure {
            fail("Android 설치 화면을 열지 못했습니다. 다시 시도해 주세요.")
        }
    }

    private fun download(release: UpdateRelease, allowMetered: Boolean) {
        if (activeDownloadId != null) return
        val destination = File(downloadDirectory(), release.assetName)
        if (destination.exists() && !destination.delete()) {
            fail("이전 임시 업데이트 파일을 정리하지 못했습니다.")
            return
        }
        val id = runCatching {
            val request = DownloadManager.Request(Uri.parse(release.downloadUrl))
                .setTitle("HtOMS Brief ${release.versionName}")
                .setDescription(if (allowMetered) "업데이트 다운로드 중" else "무과금 네트워크 대기 또는 다운로드 중")
                .setMimeType("application/vnd.android.package-archive")
                .setAllowedOverMetered(allowMetered)
                .setAllowedOverRoaming(false)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                .setDestinationInExternalFilesDir(appContext, Environment.DIRECTORY_DOWNLOADS, release.assetName)
            downloadManager.enqueue(request)
        }.getOrElse {
            fail("다운로드를 시작하지 못했습니다. 다시 시도해 주세요.")
            return
        }
        activeDownloadId = id
        persistDownload(id, release)
        _state.value = _state.value.copy(
            phase = UpdatePhase.DOWNLOADING,
            message = if (allowMetered) "업데이트 다운로드 중…" else "무과금 네트워크에서 자동 다운로드합니다.",
            release = release,
            progressPercent = 0
        )
        monitorProgress(id)
    }

    private fun monitorProgress(id: Long) {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (activeDownloadId == id) {
                val cursor = downloadManager.query(DownloadManager.Query().setFilterById(id))
                cursor.use {
                    if (!it.moveToFirst()) return@launch
                    val downloaded = it.getLong(it.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                    val total = it.getLong(it.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                    val status = it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                    if (status == DownloadManager.STATUS_FAILED) {
                        fail("다운로드에 실패했습니다. 네트워크를 확인하고 다시 시도해 주세요.")
                        return@launch
                    }
                    val percent = if (total > 0) ((downloaded * 100) / total).toInt().coerceIn(0, 100) else null
                    _state.value = _state.value.copy(progressPercent = percent)
                }
                delay(1_000)
            }
        }
    }

    private suspend fun finishDownload(id: Long) {
        progressJob?.cancel()
        val release = _state.value.release ?: loadPersistedRelease() ?: run {
            fail("다운로드한 버전 정보를 찾을 수 없습니다.")
            return
        }
        val cursor = downloadManager.query(DownloadManager.Query().setFilterById(id))
        val successful = cursor.use {
            it.moveToFirst() && it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)) == DownloadManager.STATUS_SUCCESSFUL
        }
        if (!successful) {
            fail("다운로드에 실패했습니다. 다시 시도해 주세요.")
            return
        }
        _state.value = _state.value.copy(phase = UpdatePhase.VERIFYING, message = "다운로드 확인 중…", progressPercent = 100)
        val file = File(downloadDirectory(), release.assetName)
        val verificationError = withContext(Dispatchers.IO) { verifyApk(file, release) }
        if (verificationError != null) {
            file.delete()
            downloadManager.remove(id)
            activeDownloadId = null
            clearPersistedDownload()
            fail(verificationError)
            return
        }
        _state.value = _state.value.copy(
            phase = UpdatePhase.READY,
            message = "${release.versionName} 설치 준비됨 · 설치할 때 Android 확인 화면이 열립니다.",
            progressPercent = 100
        )
    }

    private suspend fun fetchLatestRelease(): UpdateRelease? = withContext(Dispatchers.IO) {
        val connection = URL(GitHubReleaseParser.API_URL).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 15_000
            connection.readTimeout = 15_000
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
            connection.setRequestProperty("User-Agent", "HtOMS-Brief-Android/${BuildConfig.VERSION_NAME}")
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw IllegalStateException("업데이트 서버 응답 오류 (${connection.responseCode})")
            }
            val payload = connection.inputStream.bufferedReader().use { it.readText() }
            GitHubReleaseParser.parse(payload, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE.toLong())
        } finally {
            connection.disconnect()
        }
    }

    private fun verifyApk(file: File, release: UpdateRelease): String? {
        if (!file.isFile || file.length() != release.size) return "APK 크기가 릴리스 정보와 다릅니다."
        val actualDigest = file.inputStream().use { input ->
            val digest = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        }
        if (actualDigest != release.sha256) return "APK SHA-256 확인에 실패했습니다. 파일을 삭제했습니다."

        @Suppress("DEPRECATION")
        val signatureFlags = if (Build.VERSION.SDK_INT >= 28) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            PackageManager.GET_SIGNATURES
        }
        @Suppress("DEPRECATION")
        val archive = appContext.packageManager.getPackageArchiveInfo(
            file.absolutePath,
            signatureFlags
        ) ?: return "APK 패키지 정보를 읽을 수 없습니다."
        if (archive.packageName != appContext.packageName) return "다른 앱의 APK라서 설치하지 않았습니다."
        val archiveVersionCode = packageVersionCode(archive)
        if (archiveVersionCode != release.versionCode || archiveVersionCode <= BuildConfig.VERSION_CODE.toLong()) {
            return "현재 버전보다 새로운 APK가 아닙니다."
        }
        if (archive.versionName != release.versionName) return "APK 버전 이름이 릴리스 정보와 다릅니다."

        @Suppress("DEPRECATION")
        val installed = appContext.packageManager.getPackageInfo(
            appContext.packageName,
            signatureFlags
        )
        val archiveCertificates = certificateDigests(archive)
        val installedCertificates = certificateDigests(installed)
        if (archiveCertificates.isEmpty() || archiveCertificates != installedCertificates) {
            return "APK 서명 인증서가 현재 앱과 다릅니다. 파일을 삭제했습니다."
        }
        return null
    }

    private fun certificateDigests(info: PackageInfo): Set<String> {
        @Suppress("DEPRECATION")
        val signatures = if (Build.VERSION.SDK_INT >= 28) {
            info.signingInfo?.apkContentsSigners.orEmpty()
        } else {
            info.signatures.orEmpty()
        }
        return signatures.map { signature ->
            MessageDigest.getInstance("SHA-256").digest(signature.toByteArray())
                .joinToString("") { "%02x".format(it) }
        }.toSet()
    }

    @Suppress("DEPRECATION")
    private fun packageVersionCode(info: PackageInfo): Long =
        if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong()

    private fun recoverActiveDownload() {
        val id = preferences.getLong(DOWNLOAD_ID_KEY, -1L).takeIf { it >= 0 } ?: return
        val release = loadPersistedRelease() ?: run { clearPersistedDownload(); return }
        activeDownloadId = id
        _state.value = _state.value.copy(
            phase = UpdatePhase.DOWNLOADING,
            message = "업데이트 다운로드 상태 확인 중…",
            release = release,
            progressPercent = null
        )
        val cursor = downloadManager.query(DownloadManager.Query().setFilterById(id))
        cursor.use {
            if (!it.moveToFirst()) {
                activeDownloadId = null
                clearPersistedDownload()
                return
            }
            when (it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))) {
                DownloadManager.STATUS_SUCCESSFUL -> scope.launch { finishDownload(id) }
                DownloadManager.STATUS_FAILED -> fail("이전 업데이트 다운로드가 실패했습니다. 다시 시도해 주세요.")
                else -> monitorProgress(id)
            }
        }
    }

    private fun persistDownload(id: Long, release: UpdateRelease) {
        preferences.edit()
            .putLong(DOWNLOAD_ID_KEY, id)
            .putString(RELEASE_KEY, json.encodeToString(release))
            .apply()
    }

    private fun loadPersistedRelease(): UpdateRelease? = preferences.getString(RELEASE_KEY, null)
        ?.let { runCatching { json.decodeFromString<UpdateRelease>(it) }.getOrNull() }

    private fun clearPersistedDownload() {
        preferences.edit().remove(DOWNLOAD_ID_KEY).remove(RELEASE_KEY).apply()
    }

    private fun downloadDirectory(): File =
        requireNotNull(appContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS))

    private fun fail(message: String) {
        progressJob?.cancel()
        activeDownloadId = null
        _state.value = _state.value.copy(phase = UpdatePhase.ERROR, message = message, progressPercent = null)
    }

    private fun formatBytes(bytes: Long): String = when {
        bytes >= 1_048_576 -> "%.1f MB".format(bytes / 1_048_576.0)
        bytes >= 1_024 -> "%.1f KB".format(bytes / 1_024.0)
        else -> "$bytes B"
    }

    private companion object {
        const val AUTO_DOWNLOAD_KEY = "automatically_download_updates"
        const val DOWNLOAD_ID_KEY = "download_id"
        const val RELEASE_KEY = "download_release"
    }
}
