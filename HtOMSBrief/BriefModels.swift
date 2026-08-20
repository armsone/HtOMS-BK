import Foundation

struct ChannelShare: Identifiable, Sendable, Equatable {
    let name: String
    let count: Int
    let percentage: Int

    var id: String { name }
}

struct SlicePortion: Identifiable, Sendable, Equatable {
    let name: String
    let value: Double

    var id: String { name }
}

struct DailyPoint: Identifiable, Sendable, Equatable {
    let label: String
    let reference: Int?
    let result: Int?

    var id: String { label }
}

struct MonthlyPoint: Identifiable, Sendable, Equatable {
    let date: Date
    let count: Int

    var id: Date { date }
}

struct TodaySales: Sendable, Equatable {
    let day: Int
    let level: String
    let amount: Int
}

struct ServerStatus: Identifiable, Sendable, Equatable {
    let name: String
    let isOperational: Bool

    var id: String { name }
}

enum DeliveryStatus: String, CaseIterable, Sendable {
    case preparing = "준비"
    case accepted = "인수"
    case moving = "이동"
    case departing = "출발"
    case completed = "완료"
    case invoiceError = "송장오류"
    case undelivered = "미배달"
    case unavailable = "배송불가"
}

struct DeliveryStatusCount: Identifiable, Sendable, Equatable {
    let status: DeliveryStatus
    let count: Int

    var id: DeliveryStatus { status }
}

struct BriefOverview: Sendable, Equatable {
    let todaySales: TodaySales
    let channels: [ChannelShare]
    let monthLabel: String
    let monthProgress: Int
    let monthTotal: Int
    let monthAverage: Int
    let levelMix: [SlicePortion]
    let categoryMix: [SlicePortion]
    let refreshedAt: String
    let serverStatuses: [ServerStatus]
}

struct DeliverySummary: Sendable, Equatable {
    let dateRange: String
    let statuses: [DeliveryStatusCount]

    var total: Int { statuses.reduce(0) { $0 + $1.count } }
}

struct BriefSnapshot: Sendable, Equatable {
    let overview: BriefOverview
    let dayTrend: [DailyPoint]
    let monthTrend: [MonthlyPoint]
    let monthAverage: Int
    let deliverySummary: DeliverySummary
    var widgetSnapshot: DashboardWidgetSnapshot? = nil
}
