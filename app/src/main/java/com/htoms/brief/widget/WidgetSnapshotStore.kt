package com.htoms.brief.widget

import android.content.Context
import com.htoms.brief.model.DashboardWidgetSnapshot
import kotlinx.serialization.json.Json
import java.io.File

/**
 * 앱이 쓰고 위젯이 읽는 단방향 JSON 스냅샷 저장소.
 * 앱 전용(filesDir) 저장소만 사용하며 토큰·자격 증명·PII는 포함하지 않는다.
 */
class WidgetSnapshotStore(private val baseDir: File) {

    constructor(context: Context) : this(context.applicationContext.filesDir)

    fun load(): DashboardWidgetSnapshot? {
        val file = snapshotFile()
        if (!file.exists()) return null
        return runCatching {
            json.decodeFromString<DashboardWidgetSnapshot>(file.readText())
        }.getOrNull()?.takeIf { it.schemaVersion == DashboardWidgetSnapshot.CURRENT_SCHEMA_VERSION }
    }

    fun save(snapshot: DashboardWidgetSnapshot) {
        val file = snapshotFile()
        val temp = File(baseDir, "$FILE_NAME.tmp")
        temp.writeText(json.encodeToString(DashboardWidgetSnapshot.serializer(), snapshot))
        if (!temp.renameTo(file)) {
            file.writeText(json.encodeToString(DashboardWidgetSnapshot.serializer(), snapshot))
            temp.delete()
        }
    }

    fun remove() {
        val file = snapshotFile()
        if (file.exists()) file.delete()
    }

    private fun snapshotFile(): File = File(baseDir, FILE_NAME)

    companion object {
        const val FILE_NAME = "dashboard-widget-snapshot.json"
        private val json = Json { ignoreUnknownKeys = true }
    }
}
