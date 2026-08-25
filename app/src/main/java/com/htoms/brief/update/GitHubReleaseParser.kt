package com.htoms.brief.update

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.net.URI

@Serializable
data class UpdateRelease(
    val tag: String,
    val versionName: String,
    val versionCode: Long,
    val notes: String,
    val assetName: String,
    val downloadUrl: String,
    val size: Long,
    val sha256: String
)

class UpdateMetadataException(message: String) : IllegalArgumentException(message)

object GitHubReleaseParser {
    const val OWNER = "armsone"
    const val REPOSITORY = "HtOMS-BK"
    const val API_URL = "https://api.github.com/repos/$OWNER/$REPOSITORY/releases/latest"

    private val tagPattern = Regex("^android-v([0-9]+\\.[0-9]+\\.[0-9]+)$")
    private val assetPattern = Regex("^HtOMS-Brief-Android-([0-9]+\\.[0-9]+\\.[0-9]+)\\.apk$")
    private val versionCodePattern = Regex("(?m)^Android-Version-Code:\\s*([0-9]+)\\s*$")
    private val buildNumberPattern = Regex("(?m)^Build-Number:\\s*([0-9]{12})\\s*$")
    private val digestPattern = Regex("^sha256:([0-9a-fA-F]{64})$")
    private val json = Json { ignoreUnknownKeys = true }

    fun parse(
        payload: String,
        currentVersionName: String,
        currentVersionCode: Long
    ): UpdateRelease? {
        val release = runCatching { json.decodeFromString<GitHubReleaseResponse>(payload) }
            .getOrElse { throw UpdateMetadataException("업데이트 정보를 읽을 수 없습니다.") }
        if (release.draft || release.prerelease) {
            throw UpdateMetadataException("정식 릴리스가 아닙니다.")
        }

        val tagVersion = tagPattern.matchEntire(release.tagName)?.groupValues?.get(1)
            ?: throw UpdateMetadataException("허용되지 않은 릴리스 태그입니다.")
        val candidates = release.assets.mapNotNull { asset ->
            val match = assetPattern.matchEntire(asset.name) ?: return@mapNotNull null
            asset to match.groupValues[1]
        }
        if (candidates.size != 1) {
            throw UpdateMetadataException("허용된 APK 파일이 하나가 아닙니다.")
        }

        val (asset, versionName) = candidates.single()
        if (versionName != tagVersion) {
            throw UpdateMetadataException("태그와 APK 제품 버전이 다릅니다.")
        }
        val releaseNotes = release.body.orEmpty()
        val versionCode = versionCodePattern.find(releaseNotes)?.groupValues?.get(1)?.toLongOrNull()
            ?: throw UpdateMetadataException("Android 내부 버전 정보가 없거나 올바르지 않습니다.")
        if (!buildNumberPattern.containsMatchIn(releaseNotes)) {
            throw UpdateMetadataException("표시 빌드 정보가 없거나 올바르지 않습니다.")
        }
        if (asset.size <= 0) throw UpdateMetadataException("APK 크기가 올바르지 않습니다.")
        val sha256 = digestPattern.matchEntire(asset.digest ?: "")?.groupValues?.get(1)?.lowercase()
            ?: throw UpdateMetadataException("SHA-256 정보가 없거나 올바르지 않습니다.")

        validateDownloadUrl(asset.browserDownloadUrl, release.tagName, asset.name)
        if (versionCode <= currentVersionCode || compareVersions(versionName, currentVersionName) <= 0) {
            return null
        }

        return UpdateRelease(
            tag = release.tagName,
            versionName = versionName,
            versionCode = versionCode,
            notes = releaseNotes,
            assetName = asset.name,
            downloadUrl = asset.browserDownloadUrl,
            size = asset.size,
            sha256 = sha256
        )
    }

    internal fun compareVersions(left: String, right: String): Int {
        val leftParts = left.split('.').map { it.toIntOrNull() ?: -1 }
        val rightParts = right.split('.').map { it.toIntOrNull() ?: -1 }
        if (leftParts.any { it < 0 } || rightParts.any { it < 0 }) {
            throw UpdateMetadataException("버전 형식이 올바르지 않습니다.")
        }
        val length = maxOf(leftParts.size, rightParts.size)
        for (index in 0 until length) {
            val comparison = (leftParts.getOrElse(index) { 0 }).compareTo(rightParts.getOrElse(index) { 0 })
            if (comparison != 0) return comparison
        }
        return 0
    }

    private fun validateDownloadUrl(url: String, tag: String, assetName: String) {
        val uri = runCatching { URI(url) }
            .getOrElse { throw UpdateMetadataException("APK 주소가 올바르지 않습니다.") }
        val expectedPath = "/$OWNER/$REPOSITORY/releases/download/$tag/$assetName"
        if (uri.scheme != "https" || uri.host != "github.com" || uri.rawQuery != null ||
            uri.fragment != null || uri.path != expectedPath
        ) {
            throw UpdateMetadataException("허용되지 않은 APK 주소입니다.")
        }
    }
}

@Serializable
private data class GitHubReleaseResponse(
    @SerialName("tag_name") val tagName: String,
    val draft: Boolean,
    val prerelease: Boolean,
    val body: String? = null,
    val assets: List<GitHubReleaseAsset>
)

@Serializable
private data class GitHubReleaseAsset(
    val name: String,
    @SerialName("browser_download_url") val browserDownloadUrl: String,
    val size: Long,
    val digest: String? = null
)
