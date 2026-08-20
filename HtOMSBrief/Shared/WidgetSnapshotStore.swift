import Foundation

/// 앱이 쓰고 위젯이 읽는 단방향 JSON 스냅샷 저장소.
struct WidgetSnapshotStore: Sendable {
    static let appGroupIdentifier = "group.com.htoms.brief"
    static let fileName = "dashboard-widget-snapshot.json"

    private let baseURL: URL?

    init(baseURL: URL? = FileManager.default.containerURL(
        forSecurityApplicationGroupIdentifier: Self.appGroupIdentifier
    )) {
        self.baseURL = baseURL
    }

    func load() -> DashboardWidgetSnapshot? {
        guard let fileURL else { return nil }
        guard let data = try? Data(contentsOf: fileURL),
              let snapshot = try? JSONDecoder().decode(DashboardWidgetSnapshot.self, from: data),
              snapshot.schemaVersion == DashboardWidgetSnapshot.currentSchemaVersion else {
            return nil
        }
        return snapshot
    }

    func save(_ snapshot: DashboardWidgetSnapshot) throws {
        guard let fileURL else { throw WidgetSnapshotStoreError.appGroupUnavailable }
        let data = try JSONEncoder().encode(snapshot)
        try data.write(to: fileURL, options: .atomic)
    }

    func remove() throws {
        guard let fileURL else { throw WidgetSnapshotStoreError.appGroupUnavailable }
        guard FileManager.default.fileExists(atPath: fileURL.path) else { return }
        try FileManager.default.removeItem(at: fileURL)
    }

    private var fileURL: URL? {
        baseURL?.appendingPathComponent(Self.fileName, isDirectory: false)
    }
}

enum WidgetSnapshotStoreError: Error, Equatable {
    case appGroupUnavailable
}
