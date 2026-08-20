import XCTest
@testable import HtOMSBrief

final class SampleBriefProviderTests: XCTestCase {
    func testBriefOrderAndCountsAreDeterministic() async throws {
        let snapshot = try await SampleBriefProvider().loadSnapshot()
        XCTAssertEqual(snapshot.dayTrend.count, 10)
        XCTAssertEqual(snapshot.monthTrend.count, 31)
        XCTAssertEqual(snapshot.monthTrend.map(\.date), snapshot.monthTrend.map(\.date).sorted())
        XCTAssertEqual(snapshot.overview.channels.map(\.name), ["스토어", "방판", "전화"])
        XCTAssertEqual(snapshot.overview.channels.reduce(0) { $0 + $1.percentage }, 100)
        XCTAssertEqual(snapshot.overview.todaySales, TodaySales(day: 20, level: "보통", amount: 1_067))
    }

    func testDeliverySummaryMatchesAllReadOnlyStatuses() async throws {
        let snapshot = try await SampleBriefProvider().loadSnapshot()
        XCTAssertEqual(snapshot.deliverySummary.statuses.map(\.status), DeliveryStatus.allCases)
        XCTAssertEqual(snapshot.deliverySummary.total, 1_104)
    }

    func testProviderReturnsSameSnapshot() async throws {
        let provider = SampleBriefProvider()
        let first = try await provider.loadSnapshot()
        let second = try await provider.loadSnapshot()
        XCTAssertEqual(first, second)
    }
}
