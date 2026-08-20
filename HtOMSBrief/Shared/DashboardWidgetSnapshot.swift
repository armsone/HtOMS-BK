import Foundation

/// 위젯에 전달하는 표시 전용 데이터. 인증정보나 사용자 식별자는 포함하지 않는다.
struct DashboardWidgetSnapshot: Codable, Equatable, Sendable {
    static let currentSchemaVersion = 1

    let schemaVersion: Int
    let todayAmount: Int
    let yesterdayAmount: Int
    let monthTotal: Int
    let previousMonthTotal: Int
    let dailyAverage: Int
    let targetAmount: Int
    let refreshedAt: Date
    let serverTime: Date
    let isSample: Bool

    static let sample: DashboardWidgetSnapshot = {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = TimeZone(identifier: "Asia/Seoul") ?? .current
        let refreshedAt = calendar.date(from: DateComponents(
            year: 2026, month: 8, day: 20, hour: 16, minute: 48
        )) ?? Date(timeIntervalSince1970: 0)
        let serverTime = calendar.date(from: DateComponents(
            year: 2026, month: 8, day: 20, hour: 16, minute: 40
        )) ?? Date(timeIntervalSince1970: 0)

        return DashboardWidgetSnapshot(
            schemaVersion: currentSchemaVersion,
            todayAmount: 10_667_000,
            yesterdayAmount: 18_160_000,
            monthTotal: 203_527_398,
            previousMonthTotal: 320_595_397,
            dailyAverage: 10_791_012,
            targetAmount: 307_482_479,
            refreshedAt: refreshedAt,
            serverTime: serverTime,
            isSample: true
        )
    }()
}
