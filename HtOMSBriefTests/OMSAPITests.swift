import XCTest
@testable import HtOMSBrief

final class OMSAPITests: XCTestCase {
    override func tearDown() {
        URLProtocolStub.handler = nil
        super.tearDown()
    }

    func testSignInUsesOnlyFixedAuthPostAndDecodesSession() async throws {
        URLProtocolStub.handler = { request in
            XCTAssertEqual(request.httpMethod, "POST")
            XCTAssertEqual(request.url?.path, "/api/auth/signin")
            XCTAssertNil(request.value(forHTTPHeaderField: "Authorization"))
            XCTAssertEqual(request.value(forHTTPHeaderField: "Content-Type"), "application/json")
            XCTAssertTrue(request.httpBody != nil || request.httpBodyStream != nil)
            return Self.response(for: request, json: #"{"access_token":"jwt-token","user":{"username":"대표"}}"#)
        }

        let result = try await makeClient().signIn(email: "ceo@example.com", password: "secret")
        XCTAssertEqual(result.token, "jwt-token")
        XCTAssertEqual(result.username, "대표")
    }

    func testBusinessEndpointAlwaysUsesGetAndBearerToken() async throws {
        URLProtocolStub.handler = { request in
            XCTAssertEqual(request.httpMethod, "GET")
            XCTAssertEqual(request.value(forHTTPHeaderField: "Authorization"), "Bearer read-token")
            XCTAssertEqual(request.url?.path, "/api/data-view/sales-chart-data/30")
            return Self.response(for: request, json: #"{"chartData":[],"average":0}"#)
        }

        _ = try await makeClient().get(.monthChart, token: "read-token")
    }

    func testReadEndpointWhitelistContainsNoMutationRoute() {
        let endpoints: [OMSAPIClient.ReadEndpoint] = [
            .dailySales(month: "2026-08"), .monthChart, .referenceHourly, .todayHourly,
            .salesTargets(year: "2026"), .monthlySales, .salesLevels,
            .productSales(start: "2026-08-01", end: "2026-08-31"),
            .botPings,
        ]
        for endpoint in endpoints {
            XCTAssertFalse(endpoint.path.contains("delete"))
            XCTAssertFalse(endpoint.path.contains("update"))
            XCTAssertFalse(endpoint.path.contains("create"))
        }
    }

    func testDeliveryAggregateUsesOnlyGet() async throws {
        URLProtocolStub.handler = { request in
            XCTAssertEqual(request.httpMethod, "GET")
            XCTAssertEqual(request.url?.host, "firestore.googleapis.com")
            XCTAssertTrue(request.url?.path.hasSuffix("/documents/ViewData") == true)
            return Self.response(for: request, json: #"{"documents":[]}"#)
        }

        let configuration = URLSessionConfiguration.ephemeral
        configuration.protocolClasses = [URLProtocolStub.self]
        _ = try await DeliveryAggregateClient(
            url: DeliveryAggregateClient.productionURL,
            session: URLSession(configuration: configuration)
        ).loadSummary()
    }

    func testDeliveryAggregateCountsOnlyStatuses() {
        let result = DeliveryAggregateClient.makeSummary(decode(#"""
        {
          "documents": [{
            "name": "projects/test/databases/(default)/documents/ViewData/2026-08-20",
            "fields": {
              "keys": {"arrayValue":{"values":[{"stringValue":"a"},{"stringValue":"b"},{"stringValue":"c"},{"stringValue":"d"}]}},
              "a": {"mapValue":{"fields":{"statusId":{"stringValue":"information_received"},"statusText":{"stringValue":"준비"}}}},
              "b": {"mapValue":{"fields":{"statusId":{"stringValue":"delivered"},"statusText":{"stringValue":"완료"}}}},
              "c": {"mapValue":{"fields":{"statusId":{"stringValue":"track_error"},"statusText":{"stringValue":"미배달"}}}},
              "d": {"mapValue":{"fields":{"statusId":{"stringValue":"track_error"},"statusText":{"stringValue":"배송불가"}}}}
            }
          }]
        }
        """#))

        XCTAssertEqual(result.dateRange, "2026-08-20~2026-08-20(1일)")
        XCTAssertEqual(result.statuses.first { $0.status == .preparing }?.count, 1)
        XCTAssertEqual(result.statuses.first { $0.status == .completed }?.count, 1)
        XCTAssertEqual(result.statuses.first { $0.status == .undelivered }?.count, 1)
        XCTAssertEqual(result.statuses.first { $0.status == .unavailable }?.count, 1)
        XCTAssertEqual(result.total, 4)
    }

    func testMapperBuildsTodayMonthChartsAndWidgetFromServerJSON() throws {
        let current = try XCTUnwrap(Self.dateFormatter.date(from: "2026-08-20"))
        let snapshot = try RemoteBriefProvider.makeSnapshot(
            current: current,
            month: "2026-08",
            daily: decode(#"[{"date":"2026-08-19","is_store":100000,"is_onsite":0,"is_normal":200000},{"date":"2026-08-20","is_store":46000,"is_onsite":10000,"is_normal":50000}]"#),
            chart: decode(#"{"chartData":[{"date":"Jul21","sales":300000},{"date":"Aug20","sales":100000}],"average":200000}"#),
            referenceHourly: decode(#"[{"hour":"10h","sales":100000},{"hour":"09h","sales":100000}]"#),
            todayHourly: decode(#"[{"hour":"9h","sales":20000}]"#),
            targets: decode(#"[{"date":"2026-08","sales_target":1000000}]"#),
            monthlySales: decode(#"[{"year_month":"2026-07","monthly_sales_amount":700000},{"year_month":"2026-08","monthly_sales_amount":526000}]"#),
            levels: decode(#"[{"level2_min":100000,"level3_min":200000,"level4_min":300000,"level5_min":400000}]"#),
            products: decode(#"[{"product_name":"한통식판","price_sum":300000},{"product_name":"실링비닐","price_sum":100000}]"#),
            botPings: decode(#"[{"name":"HBot-0 : 장항","ping_date":"2026-08-20T00:05:00Z"},{"name":"HBot-2 : 인천","ping_date":"2026-08-19T23:49:00Z"},{"name":"HBot-1 : 삼송","ping_date":"2026-08-20T00:10:00Z"},{"name":"HBot-3 : 초월","ping_date":"2026-08-20T00:09:59.000Z"}]"#)
        )

        XCTAssertEqual(snapshot.overview.todaySales.amount, 11)
        XCTAssertEqual(snapshot.overview.todaySales.level, "낮음")
        XCTAssertEqual(snapshot.overview.monthTotal, 53)
        XCTAssertEqual(snapshot.overview.monthAverage, 3)
        XCTAssertEqual(snapshot.overview.monthProgress, 53)
        XCTAssertEqual(snapshot.dayTrend.map(\.label), ["09h", "10h"])
        XCTAssertEqual(snapshot.dayTrend.first, DailyPoint(label: "09h", reference: 10, result: 20))
        XCTAssertEqual(snapshot.dayTrend.last, DailyPoint(label: "10h", reference: 10, result: nil))
        XCTAssertEqual(snapshot.monthTrend.count, 2)
        XCTAssertEqual(Calendar.current.component(.month, from: snapshot.monthTrend[0].date), 7)
        XCTAssertEqual(Calendar.current.component(.day, from: snapshot.monthTrend[0].date), 21)
        XCTAssertEqual(Calendar.current.component(.month, from: snapshot.monthTrend[1].date), 8)
        XCTAssertEqual(Calendar.current.component(.day, from: snapshot.monthTrend[1].date), 20)
        XCTAssertEqual(snapshot.widgetSnapshot?.todayAmount, 106000)
        XCTAssertEqual(snapshot.widgetSnapshot?.yesterdayAmount, 300000)
        XCTAssertFalse(try XCTUnwrap(snapshot.widgetSnapshot).isSample)
        XCTAssertEqual(snapshot.overview.serverStatuses, [
            ServerStatus(name: "장항", isOperational: true),
            ServerStatus(name: "인천", isOperational: false),
            ServerStatus(name: "삼송", isOperational: true),
            ServerStatus(name: "초월", isOperational: true),
        ])
    }

    private func makeClient() -> OMSAPIClient {
        let configuration = URLSessionConfiguration.ephemeral
        configuration.protocolClasses = [URLProtocolStub.self]
        return OMSAPIClient(
            baseURL: URL(string: "https://example.invalid")!,
            session: URLSession(configuration: configuration)
        )
    }

    private func decode(_ json: String) -> JSONValue {
        try! JSONDecoder().decode(JSONValue.self, from: Data(json.utf8))
    }

    private static func response(for request: URLRequest, json: String) -> (HTTPURLResponse, Data) {
        let response = HTTPURLResponse(url: request.url!, statusCode: 200, httpVersion: nil, headerFields: nil)!
        return (response, Data(json.utf8))
    }

    private static let dateFormatter: DateFormatter = {
        let value = DateFormatter()
        value.calendar = Calendar(identifier: .gregorian)
        value.timeZone = TimeZone(identifier: "Asia/Seoul")
        value.locale = Locale(identifier: "en_US_POSIX")
        value.dateFormat = "yyyy-MM-dd"
        return value
    }()
}

private final class URLProtocolStub: URLProtocol, @unchecked Sendable {
    static var handler: ((URLRequest) throws -> (HTTPURLResponse, Data))?

    override class func canInit(with request: URLRequest) -> Bool { true }
    override class func canonicalRequest(for request: URLRequest) -> URLRequest { request }

    override func startLoading() {
        do {
            guard let handler = Self.handler else { throw OMSAPIError.invalidResponse }
            let (response, data) = try handler(request)
            client?.urlProtocol(self, didReceive: response, cacheStoragePolicy: .notAllowed)
            client?.urlProtocol(self, didLoad: data)
            client?.urlProtocolDidFinishLoading(self)
        } catch {
            client?.urlProtocol(self, didFailWithError: error)
        }
    }

    override func stopLoading() {}
}
