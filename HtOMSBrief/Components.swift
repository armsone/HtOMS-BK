import SwiftUI
import Charts

/// 둥근 블루그레이 카드 컨테이너.
struct BriefCard<Content: View>: View {
    let title: String
    @ViewBuilder let content: Content

    init(_ title: String, @ViewBuilder content: () -> Content) {
        self.title = title
        self.content = content()
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(title)
                .font(.system(.subheadline, design: .monospaced).weight(.semibold))
                .foregroundStyle(BriefTheme.mutedText)
            content
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(BriefTheme.card, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .strokeBorder(BriefTheme.cardStroke)
        )
    }
}

/// 통합 브리프 페이지의 섹션 공통 골격: 제목, 부제목, 본문. 스크롤은 루트 뷰가 담당한다.
struct BriefSection<Content: View>: View {
    let title: String
    let subtitle: String
    @ViewBuilder let content: Content

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            VStack(alignment: .leading, spacing: 4) {
                Rectangle()
                    .fill(BriefTheme.boardAmber)
                    .frame(width: 36, height: 3)
                    .accessibilityHidden(true)
                Text(title)
                    .font(.system(.title2, design: .monospaced).weight(.bold))
                    .foregroundStyle(.white)
                    .accessibilityAddTraits(.isHeader)
                Text(subtitle)
                    .font(.system(.footnote, design: .monospaced))
                    .foregroundStyle(BriefTheme.mutedText)
            }
            content
        }
    }
}

/// Level/Category 공용 도넛 차트.
struct DoughnutChartView: View {
    let unitName: String
    let portions: [SlicePortion]

    private var total: Double {
        portions.reduce(0) { $0 + $1.value }
    }

    private var summaryText: String {
        let parts = portions.map { portion -> String in
            let percent = total > 0 ? Int(round(portion.value * 100 / total)) : 0
            return "\(portion.name) \(percent)퍼센트"
        }
        return "\(unitName) 구성: " + parts.joined(separator: ", ")
    }

    var body: some View {
        Chart(portions) { portion in
            SectorMark(
                angle: .value("비중", portion.value),
                innerRadius: .ratio(0.62),
                angularInset: 1.5
            )
            .cornerRadius(3)
            .foregroundStyle(by: .value("항목", portion.name))
        }
        .chartForegroundStyleScale(
            domain: portions.map(\.name),
            range: Array(BriefTheme.seriesPalette.prefix(portions.count))
        )
        .chartLegend(position: .bottom, alignment: .leading, spacing: 8)
        .frame(height: 200)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(summaryText)
    }
}
