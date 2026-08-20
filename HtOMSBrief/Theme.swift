import SwiftUI

/// 공항 출발/도착 안내판에 한통도시락의 그레이·오렌지 제품 색상을 결합한 색 체계.
enum BriefTheme {
    static let background = BrandPalette.background
    static let card = BrandPalette.card
    static let cardStroke = Color.white.opacity(0.12)

    /// 제품 사진의 코럴 오렌지를 앱 전체 강조색으로 사용한다.
    static let brandOrange = BrandPalette.orange
    static let boardCell = BrandPalette.boardCell
    static let boardAmber = brandOrange
    static let mutedText = BrandPalette.mutedText
    static let accent = brandOrange
    static let positive = Color(red: 0.30, green: 0.78, blue: 0.55)
    static let warning = brandOrange
    static let negative = Color(red: 0.92, green: 0.38, blue: 0.40)

    static let seriesPalette: [Color] = [
        brandOrange,
        Color(red: 0.78, green: 0.78, blue: 0.80),
        Color(red: 0.96, green: 0.57, blue: 0.39),
        Color(red: 0.48, green: 0.48, blue: 0.50),
        Color(red: 0.99, green: 0.72, blue: 0.58),
        Color(red: 0.32, green: 0.32, blue: 0.34),
    ]

    static func color(for status: DeliveryStatus) -> Color {
        switch status {
        case .preparing: return accent
        case .accepted: return warning
        case .moving: return positive
        case .departing: return mutedText
        case .completed: return Color.white.opacity(0.72)
        case .invoiceError, .undelivered: return Color(red: 0.78, green: 0.22, blue: 0.46)
        case .unavailable: return negative
        }
    }
}

/// 숫자 표기 도우미.
enum BriefFormat {
    private static let grouped: NumberFormatter = {
        let formatter = NumberFormatter()
        formatter.numberStyle = .decimal
        formatter.groupingSeparator = ","
        return formatter
    }()

    static func number(_ value: Int) -> String {
        grouped.string(from: NSNumber(value: value)) ?? "\(value)"
    }

    static func won(_ value: Int) -> String {
        number(value) + "원"
    }

    static func count(_ value: Int) -> String {
        number(value) + "건"
    }

    /// 차트 축 등 좁은 자리용 축약 표기.
    static func compactWon(_ value: Int) -> String {
        if value >= 100_000_000 {
            let eok = Double(value) / 100_000_000
            return String(format: eok.truncatingRemainder(dividingBy: 1) == 0 ? "%.0f억" : "%.1f억", eok)
        }
        if value >= 10_000 {
            return number(value / 10_000) + "만"
        }
        return number(value)
    }
}
