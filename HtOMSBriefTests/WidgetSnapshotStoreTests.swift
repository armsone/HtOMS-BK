import Foundation
import Testing
@testable import HtOMSBrief

struct WidgetSnapshotStoreTests {
    @Test func savesAndLoadsSnapshot() throws {
        let directory = FileManager.default.temporaryDirectory
            .appendingPathComponent(UUID().uuidString, isDirectory: true)
        try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        defer { try? FileManager.default.removeItem(at: directory) }

        let store = WidgetSnapshotStore(baseURL: directory)
        try store.save(.sample)

        #expect(store.load() == .sample)
    }

    @Test func damagedFileDoesNotProduceSnapshot() throws {
        let directory = FileManager.default.temporaryDirectory
            .appendingPathComponent(UUID().uuidString, isDirectory: true)
        try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        defer { try? FileManager.default.removeItem(at: directory) }

        let fileURL = directory.appendingPathComponent(WidgetSnapshotStore.fileName)
        try Data("not-json".utf8).write(to: fileURL)

        #expect(WidgetSnapshotStore(baseURL: directory).load() == nil)
    }
}
