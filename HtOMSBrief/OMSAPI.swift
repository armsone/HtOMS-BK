import Foundation

enum OMSAPIError: LocalizedError, Equatable {
    case invalidURL
    case invalidResponse
    case unauthorized
    case server(statusCode: Int)
    case malformedData

    var errorDescription: String? {
        switch self {
        case .invalidURL: return "OMS 서버 주소를 만들 수 없습니다."
        case .invalidResponse: return "OMS 서버의 응답을 확인할 수 없습니다."
        case .unauthorized: return "로그인이 만료되었거나 계정 정보가 올바르지 않습니다."
        case .server(let statusCode): return "OMS 서버 요청에 실패했습니다. (\(statusCode))"
        case .malformedData: return "OMS 서버 데이터 형식이 예상과 다릅니다."
        }
    }
}

enum JSONValue: Decodable, Sendable, Equatable {
    case object([String: JSONValue])
    case array([JSONValue])
    case string(String)
    case number(Double)
    case bool(Bool)
    case null

    init(from decoder: Decoder) throws {
        let container = try decoder.singleValueContainer()
        if container.decodeNil() { self = .null }
        else if let value = try? container.decode(Bool.self) { self = .bool(value) }
        else if let value = try? container.decode(Double.self) { self = .number(value) }
        else if let value = try? container.decode(String.self) { self = .string(value) }
        else if let value = try? container.decode([String: JSONValue].self) { self = .object(value) }
        else if let value = try? container.decode([JSONValue].self) { self = .array(value) }
        else { throw OMSAPIError.malformedData }
    }

    var objectValue: [String: JSONValue]? {
        guard case .object(let value) = self else { return nil }
        return value
    }

    var arrayValue: [JSONValue]? {
        guard case .array(let value) = self else { return nil }
        return value
    }

    var stringValue: String? {
        switch self {
        case .string(let value): return value
        case .number(let value): return String(value)
        default: return nil
        }
    }

    var doubleValue: Double? {
        switch self {
        case .number(let value): return value
        case .string(let value): return Double(value.replacingOccurrences(of: ",", with: ""))
        default: return nil
        }
    }
}

struct OMSAPIClient: Sendable {
    static let productionBaseURL = URL(string: "https://htoms.cafe24.com")!

    let baseURL: URL
    let session: URLSession

    init(baseURL: URL = productionBaseURL, session: URLSession = .shared) {
        self.baseURL = baseURL
        self.session = session
    }

    /// 인증 수립에 필요한 유일한 POST 요청입니다. 업무 데이터 요청은 `get`만 허용합니다.
    func signIn(email: String, password: String) async throws -> AuthSession {
        guard !email.isEmpty, !password.isEmpty else { throw AuthError.emptyCredentials }
        let url = baseURL.appending(path: "/api/auth/signin")
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = try JSONEncoder().encode(SignInRequest(email: email, password: password))
        let json = try await execute(request)
        guard let body = json.objectValue,
              let token = body["access_token"]?.stringValue,
              !token.isEmpty else { throw OMSAPIError.malformedData }
        let user = body["user"]?.objectValue
        let displayName = user?["username"]?.stringValue ?? user?["email"]?.stringValue ?? email
        return AuthSession(token: token, username: displayName, issuedAt: .now)
    }

    /// 읽기 전용 엔드포인트만 표현할 수 있어 PUT/PATCH/DELETE 및 업무 POST가 생성되지 않습니다.
    func get(_ endpoint: ReadEndpoint, token: String) async throws -> JSONValue {
        guard var components = URLComponents(url: baseURL, resolvingAgainstBaseURL: false) else {
            throw OMSAPIError.invalidURL
        }
        components.path = endpoint.path
        components.queryItems = endpoint.queryItems
        guard let url = components.url else { throw OMSAPIError.invalidURL }
        var request = URLRequest(url: url)
        request.httpMethod = "GET"
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        return try await execute(request)
    }

    private func execute(_ request: URLRequest) async throws -> JSONValue {
        let (data, response) = try await session.data(for: request)
        guard let http = response as? HTTPURLResponse else { throw OMSAPIError.invalidResponse }
        switch http.statusCode {
        case 200..<300: break
        case 401, 403: throw OMSAPIError.unauthorized
        default: throw OMSAPIError.server(statusCode: http.statusCode)
        }
        return try JSONDecoder().decode(JSONValue.self, from: data)
    }

    private struct SignInRequest: Encodable {
        let email: String
        let password: String
    }

    enum ReadEndpoint: Sendable, Equatable {
        case dailySales(month: String)
        case monthChart
        case referenceHourly
        case todayHourly
        case salesTargets(year: String)
        case monthlySales
        case salesLevels
        case productSales(start: String, end: String)
        case botPings

        var path: String {
            switch self {
            case .dailySales(let month): return "/api/data-view/daily-sales-amount/\(month)"
            case .monthChart: return "/api/data-view/sales-chart-data/30"
            case .referenceHourly: return "/api/data-view/sales-chart-data/30-per-hour"
            case .todayHourly: return "/api/data-view/sales-chart-data/day-per-hour"
            case .salesTargets(let year): return "/api/data-view/sales-target/\(year)"
            case .monthlySales: return "/api/dynamic-crud/v_monthly_sales_amount/all"
            case .salesLevels: return "/api/dynamic-crud/calendar_sales_level/all"
            case .productSales: return "/api/dynamic-crud/v_sales_status/range"
            case .botPings: return "/api/dynamic-crud/hantong_bot_ping/all"
            }
        }

        var queryItems: [URLQueryItem]? {
            guard case .productSales(let start, let end) = self else { return nil }
            return [
                URLQueryItem(name: "lteKey", value: "delivery_date"),
                URLQueryItem(name: "lteValue", value: end),
                URLQueryItem(name: "gteKey", value: "delivery_date"),
                URLQueryItem(name: "gteValue", value: start),
            ]
        }
    }
}

struct OMSAuthService: AuthServicing {
    let client: OMSAPIClient

    init(client: OMSAPIClient = OMSAPIClient()) { self.client = client }

    func authenticate(username: String, password: String) async throws -> AuthSession {
        try await client.signIn(email: username, password: password)
    }
}

/// 기존 현황판의 공개 `ViewData` 컬렉션에서 배송 상태만 읽어 합계로 변환합니다.
/// 개별 주문번호·송장번호는 모델에 담거나 저장하지 않습니다.
struct DeliveryAggregateClient: Sendable {
    static let productionURL = URL(string:
        "https://firestore.googleapis.com/v1/projects/hantondeliverytrack/databases/(default)/documents/ViewData?pageSize=100"
    )!

    let url: URL
    let session: URLSession

    init(url: URL = productionURL, session: URLSession = .shared) {
        self.url = url
        self.session = session
    }

    func loadSummary() async throws -> DeliverySummary {
        var request = URLRequest(url: url)
        request.httpMethod = "GET"
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        let (data, response) = try await session.data(for: request)
        guard let http = response as? HTTPURLResponse else { throw OMSAPIError.invalidResponse }
        guard (200..<300).contains(http.statusCode) else {
            throw OMSAPIError.server(statusCode: http.statusCode)
        }
        return Self.makeSummary(try JSONDecoder().decode(JSONValue.self, from: data))
    }

    static func makeSummary(_ root: JSONValue) -> DeliverySummary {
        var counts = Dictionary(uniqueKeysWithValues: DeliveryStatus.allCases.map { ($0, 0) })
        var documentDates: [Date] = []

        for document in root.objectValue?["documents"]?.arrayValue ?? [] {
            guard let body = document.objectValue else { continue }
            if let name = body["name"]?.stringValue,
               let date = documentDateFormatter.date(from: String(name.split(separator: "/").last ?? "")) {
                documentDates.append(date)
            }

            guard let fields = body["fields"]?.objectValue else { continue }
            let keys = fields["keys"]?.objectValue?["arrayValue"]?.objectValue?["values"]?.arrayValue ?? []
            for keyValue in keys {
                guard let key = keyValue.objectValue?["stringValue"]?.stringValue,
                      let statusFields = fields[key]?.objectValue?["mapValue"]?.objectValue?["fields"]?.objectValue else {
                    continue
                }
                let statusId = statusFields["statusId"]?.objectValue?["stringValue"]?.stringValue ?? ""
                let statusText = statusFields["statusText"]?.objectValue?["stringValue"]?.stringValue ?? ""
                if let status = deliveryStatus(id: statusId, text: statusText) {
                    counts[status, default: 0] += 1
                }
            }
        }

        let dateRange: String
        if let start = documentDates.min(), let end = documentDates.max() {
            let days = (deliveryCalendar.dateComponents([.day], from: start, to: end).day ?? 0) + 1
            dateRange = "\(documentDateFormatter.string(from: start))~\(documentDateFormatter.string(from: end))(\(days)일)"
        } else {
            dateRange = "택배 집계 조회"
        }
        return DeliverySummary(
            dateRange: dateRange,
            statuses: DeliveryStatus.allCases.map { DeliveryStatusCount(status: $0, count: counts[$0, default: 0]) }
        )
    }

    private static func deliveryStatus(id: String, text: String) -> DeliveryStatus? {
        switch id {
        case "information_received": return .preparing
        case "at_pickup": return .accepted
        case "in_transit": return .moving
        case "out_for_delivery": return .departing
        case "delivered": return text.contains("배송불가") ? .unavailable : .completed
        case "track_error":
            if text.contains("미배달") { return .undelivered }
            if text.contains("배송불가") { return .unavailable }
            return .invoiceError
        default:
            return ["용달신용", "용달착불", "직접수령", "쿠팡"].contains { text.contains($0) } ? .completed : nil
        }
    }

    private static var deliveryCalendar: Calendar = {
        var value = Calendar(identifier: .gregorian)
        value.timeZone = TimeZone(identifier: "Asia/Seoul") ?? .current
        return value
    }()

    private static let documentDateFormatter: DateFormatter = {
        let value = DateFormatter()
        value.calendar = deliveryCalendar
        value.timeZone = deliveryCalendar.timeZone
        value.locale = Locale(identifier: "en_US_POSIX")
        value.dateFormat = "yyyy-MM-dd"
        return value
    }()
}

struct RemoteBriefProvider: BriefProviding {
    let token: String
    let client: OMSAPIClient
    let deliveryClient: DeliveryAggregateClient
    let now: @Sendable () -> Date

    init(
        token: String,
        client: OMSAPIClient = OMSAPIClient(),
        deliveryClient: DeliveryAggregateClient = DeliveryAggregateClient(),
        now: @escaping @Sendable () -> Date = { .now }
    ) {
        self.token = token
        self.client = client
        self.deliveryClient = deliveryClient
        self.now = now
    }

    func loadSnapshot() async throws -> BriefSnapshot {
        let current = now()
        let calendar = Self.seoulCalendar
        let month = Self.monthFormatter.string(from: current)
        let year = Self.yearFormatter.string(from: current)
        let monthStart = calendar.date(from: calendar.dateComponents([.year, .month], from: current)) ?? current
        let monthEnd = calendar.date(byAdding: DateComponents(month: 1, day: -1), to: monthStart) ?? current

        async let daily = client.get(.dailySales(month: month), token: token)
        async let chart = client.get(.monthChart, token: token)
        async let reference = client.get(.referenceHourly, token: token)
        async let today = client.get(.todayHourly, token: token)
        async let targets = optional(.salesTargets(year: year))
        async let monthly = optional(.monthlySales)
        async let levels = optional(.salesLevels)
        async let products = optional(.productSales(
            start: Self.dayFormatter.string(from: monthStart),
            end: Self.dayFormatter.string(from: monthEnd)
        ))
        async let botPings = optional(.botPings)
        async let delivery = optionalDelivery()

        let dailyValue = try await daily
        let chartValue = try await chart
        let referenceValue = try await reference
        let todayValue = try await today
        let targetsValue = try await targets
        let monthlyValue = try await monthly
        let levelsValue = try await levels
        let productsValue = try await products
        let botPingsValue = try await botPings
        let deliveryValue = await delivery

        return try Self.makeSnapshot(
            current: current, month: month, daily: dailyValue, chart: chartValue,
            referenceHourly: referenceValue, todayHourly: todayValue, targets: targetsValue,
            monthlySales: monthlyValue, levels: levelsValue, products: productsValue,
            botPings: botPingsValue, refreshedAt: now(),
            deliverySummary: deliveryValue
        )
    }

    /// 보조 패널 하나가 비어도 오늘 매출과 핵심 차트는 표시하되, 인증 만료는 숨기지 않습니다.
    private func optional(_ endpoint: OMSAPIClient.ReadEndpoint) async throws -> JSONValue {
        do {
            return try await client.get(endpoint, token: token)
        } catch OMSAPIError.unauthorized {
            throw OMSAPIError.unauthorized
        } catch {
            return .array([])
        }
    }

    private func optionalDelivery() async -> DeliverySummary {
        (try? await deliveryClient.loadSummary()) ?? DeliverySummary(
            dateRange: "택배 집계 조회 대기",
            statuses: DeliveryStatus.allCases.map { DeliveryStatusCount(status: $0, count: 0) }
        )
    }

    static func makeSnapshot(
        current: Date,
        month: String,
        daily: JSONValue,
        chart: JSONValue,
        referenceHourly: JSONValue,
        todayHourly: JSONValue,
        targets: JSONValue,
        monthlySales: JSONValue,
        levels: JSONValue,
        products: JSONValue,
        botPings: JSONValue = .array([]),
        refreshedAt: Date? = nil,
        deliverySummary: DeliverySummary = DeliverySummary(
            dateRange: "택배 집계 조회 대기",
            statuses: DeliveryStatus.allCases.map { DeliveryStatusCount(status: $0, count: 0) }
        )
    ) throws -> BriefSnapshot {
        let calendar = seoulCalendar
        let day = calendar.component(.day, from: current)
        let todayKey = dayFormatter.string(from: current)
        let dailyRows = rows(daily)
        let todayRow = dailyRows.first { string($0, "date") == todayKey }
        let todayWon = channelWon(todayRow)
        let monthWon = dailyRows.reduce(0) { $0 + channelWon($1) }
        let previousDay = calendar.date(byAdding: .day, value: -1, to: current) ?? current
        let yesterdayKey = dayFormatter.string(from: previousDay)
        let yesterdayWon = channelWon(dailyRows.first { string($0, "date") == yesterdayKey })
        let thresholds = rows(levels).first
        let todayLevel = level(for: Double(todayWon), thresholds: thresholds)

        let chartBody = chart.objectValue ?? [:]
        let chartRows = rows(chartBody["chartData"] ?? chart)
        let averageWon = Int(number(chartBody, "average"))
        let monthTrend = chartRows.compactMap { row -> MonthlyPoint? in
            guard let text = string(row, "date"), let date = parseChartDate(text, relativeTo: current) else { return nil }
            return MonthlyPoint(date: date, count: Int((number(row, "sales") / 10_000).rounded()))
        }
        let monthPoints = monthTrend.filter { calendar.isDate($0.date, equalTo: current, toGranularity: .month) }
        let levelMix = ["위험", "낮음", "보통", "높음", "최고"].map { name in
            SlicePortion(name: name, value: Double(monthPoints.filter {
                level(for: Double($0.count * 10_000), thresholds: thresholds) == name
            }.count))
        }

        let referenceByHour = hourlyValues(rows(referenceHourly), multiplier: 1)
        let todayByHour = hourlyValues(rows(todayHourly), multiplier: 10)
        let hours = Set(referenceByHour.keys).union(todayByHour.keys).sorted()
        let dayTrend = hours.map { hour in
            DailyPoint(
                label: String(format: "%02dh", hour),
                reference: referenceByHour[hour],
                result: todayByHour[hour]
            )
        }

        let targetWon = rows(targets).first { string($0, "date") == month }.map { Int(number($0, "sales_target")) } ?? 0
        let monthlyRows = rows(monthlySales)
        let previousDate = calendar.date(byAdding: .month, value: -1, to: current) ?? current
        let previousMonth = monthFormatter.string(from: previousDate)
        let previousMonthWon = monthlyRows.first { string($0, "year_month") == previousMonth }.map {
            Int(number($0, "monthly_sales_amount"))
        } ?? 0
        let authoritativeMonthWon = monthlyRows.first { string($0, "year_month") == month }.map {
            Int(number($0, "monthly_sales_amount"))
        } ?? monthWon
        let monthAverageWon = day > 0 ? authoritativeMonthWon / day : averageWon
        let refreshDate = refreshedAt ?? current
        let refreshedAtText = timeFormatter.string(from: refreshDate)

        let widget = DashboardWidgetSnapshot(
            schemaVersion: DashboardWidgetSnapshot.currentSchemaVersion,
            todayAmount: todayWon, yesterdayAmount: yesterdayWon,
            monthTotal: authoritativeMonthWon, previousMonthTotal: previousMonthWon,
            dailyAverage: monthAverageWon, targetAmount: targetWon,
            refreshedAt: refreshDate, serverTime: refreshDate, isSample: false
        )
        return BriefSnapshot(
            overview: BriefOverview(
                todaySales: TodaySales(day: day, level: todayLevel, amount: tenThousandUnits(todayWon)),
                channels: channelShares(todayRow),
                monthLabel: "\(calendar.component(.month, from: current))월",
                monthProgress: targetWon > 0 ? Int((Double(authoritativeMonthWon) / Double(targetWon) * 100).rounded()) : 0,
                monthTotal: tenThousandUnits(authoritativeMonthWon),
                monthAverage: tenThousandUnits(monthAverageWon),
                levelMix: levelMix,
                categoryMix: productMix(rows(products)),
                refreshedAt: refreshedAtText,
                serverStatuses: serverStatuses(rows(botPings), current: current)
            ),
            dayTrend: dayTrend,
            monthTrend: monthTrend,
            monthAverage: tenThousandUnits(averageWon),
            deliverySummary: deliverySummary,
            widgetSnapshot: widget
        )
    }

    private static func rows(_ value: JSONValue) -> [[String: JSONValue]] {
        let source = value.arrayValue ?? value.objectValue?["data"]?.arrayValue ?? []
        return source.compactMap(\.objectValue)
    }

    private static func string(_ row: [String: JSONValue], _ key: String) -> String? { row[key]?.stringValue }
    private static func number(_ row: [String: JSONValue], _ key: String) -> Double { row[key]?.doubleValue ?? 0 }

    /// 기존 OMS 화면의 `Math.round(amount / 10000)`와 같은 만원 단위 표기 규칙입니다.
    private static func tenThousandUnits(_ amount: Int) -> Int {
        Int((Double(amount) / 10_000).rounded())
    }

    private static func channelWon(_ row: [String: JSONValue]?) -> Int {
        guard let row else { return 0 }
        return Int(number(row, "is_onsite") + number(row, "is_store") + number(row, "is_normal"))
    }

    private static func channelShares(_ row: [String: JSONValue]?) -> [ChannelShare] {
        guard let row else { return [] }
        let entries = [("스토어", number(row, "is_store")), ("방판", number(row, "is_onsite")), ("전화", number(row, "is_normal"))]
        let total = entries.reduce(0) { $0 + $1.1 }
        return entries.map { name, won in
            ChannelShare(name: name, count: Int((won / 10_000).rounded()),
                         percentage: total > 0 ? Int((won / total * 100).rounded()) : 0)
        }
    }

    /// 기존 OMS 현황판의 비교 규칙: 30일 합계는 만원, 오늘 값은 비교를 위해 10배 확대합니다.
    private static func hourlyValues(_ rows: [[String: JSONValue]], multiplier: Double) -> [Int: Int] {
        var rawByHour: [Int: Double] = [:]
        for row in rows {
            guard let text = string(row, "hour"), let hour = normalizedHour(text) else { continue }
            rawByHour[hour, default: 0] += number(row, "sales")
        }
        return rawByHour.mapValues { Int(($0 * multiplier / 10_000).rounded()) }
    }

    private static func normalizedHour(_ text: String) -> Int? {
        let digits = text.filter(\.isNumber)
        guard let hour = Int(digits), (0...23).contains(hour) else { return nil }
        return hour
    }

    private static func level(for sales: Double, thresholds: [String: JSONValue]?) -> String {
        guard let thresholds else { return "확인중" }
        if sales >= number(thresholds, "level5_min") { return "최고" }
        if sales >= number(thresholds, "level4_min") { return "높음" }
        if sales >= number(thresholds, "level3_min") { return "보통" }
        if sales >= number(thresholds, "level2_min") { return "낮음" }
        return "위험"
    }

    private static func productMix(_ rows: [[String: JSONValue]]) -> [SlicePortion] {
        var sums: [String: Double] = [:]
        rows.forEach { sums[string($0, "product_name") ?? "기타", default: 0] += number($0, "price_sum") }
        let sorted = sums.sorted { $0.value > $1.value }
        var result = sorted.prefix(5).map { SlicePortion(name: $0.key, value: $0.value) }
        let remainder = sorted.dropFirst(5).reduce(0) { $0 + $1.value }
        if remainder > 0 { result.append(SlicePortion(name: "기타", value: remainder)) }
        return result
    }

    /// 기존 OMS와 동일하게 마지막 핑이 현재 시각 기준 10분 이내면 정상으로 봅니다.
    private static func serverStatuses(_ rows: [[String: JSONValue]], current: Date) -> [ServerStatus] {
        let servers = [
            (botName: "HBot-0 : 장항", displayName: "장항"),
            (botName: "HBot-2 : 인천", displayName: "인천"),
            (botName: "HBot-1 : 삼송", displayName: "삼송"),
            (botName: "HBot-3 : 초월", displayName: "초월"),
        ]

        return servers.map { server in
            let row = rows.first { string($0, "name") == server.botName }
            let ping = row.flatMap { string($0, "ping_date") }.flatMap(parsePingDate)
            let isOperational = ping.map { current.timeIntervalSince($0) <= 10 * 60 } ?? false
            return ServerStatus(name: server.displayName, isOperational: isOperational)
        }
    }

    private static func parsePingDate(_ text: String) -> Date? {
        let normalized = text
            .replacingOccurrences(of: "T", with: " ")
            .replacingOccurrences(of: "Z", with: "")
        return pingDateFormatters.lazy.compactMap { $0.date(from: normalized) }.first
    }

    private static func parseChartDate(_ text: String, relativeTo current: Date) -> Date? {
        if let date = dayFormatter.date(from: text) { return date }
        let partial = chartDateFormatters.lazy.compactMap { $0.date(from: text) }.first
        guard let partial else { return nil }
        let currentYear = seoulCalendar.component(.year, from: current)
        let partialComponents = seoulCalendar.dateComponents([.month, .day], from: partial)
        guard var candidate = seoulCalendar.date(from: DateComponents(
            year: currentYear,
            month: partialComponents.month,
            day: partialComponents.day
        )) else { return nil }
        if candidate > seoulCalendar.date(byAdding: .day, value: 1, to: current) ?? current,
           let previousYear = seoulCalendar.date(byAdding: .year, value: -1, to: candidate) {
            candidate = previousYear
        }
        return candidate
    }

    private static var seoulCalendar: Calendar = {
        var value = Calendar(identifier: .gregorian)
        value.timeZone = TimeZone(identifier: "Asia/Seoul") ?? .current
        return value
    }()

    private static let dayFormatter = formatter("yyyy-MM-dd")
    private static let monthFormatter = formatter("yyyy-MM")
    private static let yearFormatter = formatter("yyyy")
    private static let timeFormatter = formatter("HH:mm")
    private static let chartDateFormatters = [formatter("MMM d"), formatter("MMMd")]
    private static let pingDateFormatters = [
        formatter("yyyy-MM-dd HH:mm:ss.SSSSSS"),
        formatter("yyyy-MM-dd HH:mm:ss.SSS"),
        formatter("yyyy-MM-dd HH:mm:ss"),
    ]

    private static func formatter(_ format: String) -> DateFormatter {
        let value = DateFormatter()
        value.calendar = seoulCalendar
        value.timeZone = seoulCalendar.timeZone
        value.locale = Locale(identifier: "en_US_POSIX")
        value.dateFormat = format
        return value
    }
}
