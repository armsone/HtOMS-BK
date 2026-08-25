package com.htoms.brief.widget

import com.htoms.brief.model.DashboardWidgetSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/** iOS WidgetSnapshotStoreTests 대응: JSON 스냅샷 저장·손상 처리·삭제. */
class WidgetSnapshotStoreTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun savesAndLoadsSnapshot() {
        val store = WidgetSnapshotStore(temporaryFolder.root)
        store.save(DashboardWidgetSnapshot.sample)
        assertEquals(DashboardWidgetSnapshot.sample, store.load())
    }

    @Test
    fun damagedFileDoesNotProduceSnapshot() {
        File(temporaryFolder.root, WidgetSnapshotStore.FILE_NAME).writeText("not-json")
        assertNull(WidgetSnapshotStore(temporaryFolder.root).load())
    }

    @Test
    fun mismatchedSchemaVersionIsRejected() {
        val store = WidgetSnapshotStore(temporaryFolder.root)
        store.save(DashboardWidgetSnapshot.sample.copy(schemaVersion = 999))
        assertNull(store.load())
    }

    @Test
    fun removeDeletesSnapshotAndMissingFileIsNoError() {
        val store = WidgetSnapshotStore(temporaryFolder.root)
        store.save(DashboardWidgetSnapshot.sample)
        store.remove()
        assertNull(store.load())
        store.remove() // 파일이 없어도 예외 없이 통과해야 한다.
    }

    @Test
    fun snapshotJsonNeverContainsCredentialFields() {
        val store = WidgetSnapshotStore(temporaryFolder.root)
        store.save(DashboardWidgetSnapshot.sample)
        val raw = File(temporaryFolder.root, WidgetSnapshotStore.FILE_NAME).readText()
        listOf("token", "password", "username", "email").forEach { field ->
            assertEquals("스냅샷에 $field 가 포함되면 안 됩니다", false, raw.contains(field))
        }
    }
}
