import Foundation

protocol BriefProviding: Sendable {
    func loadSnapshot() async throws -> BriefSnapshot
}

/// 네트워크를 전혀 사용하지 않는 결정적 화면 샘플입니다.
struct SampleBriefProvider: BriefProviding {
    func loadSnapshot() async throws -> BriefSnapshot { Self.snapshot }

    static let snapshot = makeSnapshot()

    private static func makeSnapshot() -> BriefSnapshot {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = TimeZone(identifier: "Asia/Seoul") ?? .current
        let start = calendar.date(from: DateComponents(year: 2026, month: 7, day: 21))!
        let monthValues = [
            1710, 1450, 1080, 1660, 0, 0, 1960, 1310, 1430, 820,
            1360, 0, 0, 1450, 1560, 1730, 1490, 1670, 0, 0,
            2200, 2220, 2100, 920, 650, 0, 0, 0, 1220, 1820, 1067,
        ]

        let overview = BriefOverview(
            todaySales: TodaySales(day: 20, level: "보통", amount: 1_067),
            channels: [
                ChannelShare(name: "스토어", count: 4_065, percentage: 20),
                ChannelShare(name: "방판", count: 216, percentage: 1),
                ChannelShare(name: "전화", count: 16_072, percentage: 79),
            ],
            monthLabel: "8월",
            monthProgress: 66,
            monthTotal: 20_353,
            monthAverage: 1_018,
            levelMix: [
                SlicePortion(name: "최고", value: 23),
                SlicePortion(name: "높음", value: 38),
                SlicePortion(name: "보통", value: 23),
                SlicePortion(name: "낮음", value: 8),
                SlicePortion(name: "위험", value: 8),
            ],
            categoryMix: [
                SlicePortion(name: "일회용식판", value: 30.7),
                SlicePortion(name: "한통식판본체", value: 14.5),
                SlicePortion(name: "한통식판뚜껑", value: 12.6),
                SlicePortion(name: "실링비닐D", value: 7.9),
                SlicePortion(name: "6찬식판본체", value: 7.8),
                SlicePortion(name: "기타", value: 26.5),
            ],
            refreshedAt: "09:48",
            serverStatuses: ["장항", "인천", "삼송", "초월"].map {
                ServerStatus(name: $0, isOperational: true)
            }
        )

        let dayTrend = [
            DailyPoint(label: "08h", reference: 5_400, result: 800),
            DailyPoint(label: "09h", reference: 5_000, result: 2_300),
            DailyPoint(label: "10h", reference: 3_200, result: 200),
            DailyPoint(label: "11h", reference: 4_000, result: 2_600),
            DailyPoint(label: "12h", reference: 200, result: 100),
            DailyPoint(label: "13h", reference: 5_800, result: 2_700),
            DailyPoint(label: "14h", reference: 5_000, result: 800),
            DailyPoint(label: "15h", reference: 4_400, result: 600),
            DailyPoint(label: "16h", reference: 400, result: 150),
            DailyPoint(label: "17h", reference: 0, result: nil),
        ]

        let monthTrend = monthValues.enumerated().map { index, value in
            MonthlyPoint(date: calendar.date(byAdding: .day, value: index, to: start)!, count: value)
        }

        return BriefSnapshot(
            overview: overview,
            dayTrend: dayTrend,
            monthTrend: monthTrend,
            monthAverage: 1_079,
            deliverySummary: DeliverySummary(
                dateRange: "2026-08-06~2026-08-19(14일)",
                statuses: [
                    DeliveryStatusCount(status: .preparing, count: 0),
                    DeliveryStatusCount(status: .accepted, count: 0),
                    DeliveryStatusCount(status: .moving, count: 17),
                    DeliveryStatusCount(status: .departing, count: 41),
                    DeliveryStatusCount(status: .completed, count: 1_046),
                    DeliveryStatusCount(status: .invoiceError, count: 0),
                    DeliveryStatusCount(status: .undelivered, count: 0),
                    DeliveryStatusCount(status: .unavailable, count: 0),
                ]
            )
        )
    }
}
