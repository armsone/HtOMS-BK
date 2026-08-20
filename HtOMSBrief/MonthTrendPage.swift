import SwiftUI
import Charts

struct MonthTrendPage: View {
    let points: [MonthlyPoint]
    let average: Int

    var body: some View {
        BriefSection(title: "월간 매출", subtitle: "MONTH · \(dateRange)") {
            BriefCard("일별 매출") {
                HStack {
                    Text("단위: 만원")
                        .font(.caption)
                        .foregroundStyle(BriefTheme.mutedText)
                    Spacer()
                    Text("평균 \(BriefFormat.number(average))")
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(BriefTheme.warning)
                }
                Chart {
                    ForEach(points) { point in
                        LineMark(x: .value("날짜", point.date), y: .value("판매", point.count))
                            .foregroundStyle(Color.white.opacity(0.54))
                            .lineStyle(StrokeStyle(lineWidth: 1.6))
                        PointMark(x: .value("날짜", point.date), y: .value("판매", point.count))
                            .foregroundStyle(point.count > 0 ? Color.white.opacity(0.62) : BriefTheme.negative)
                            .symbolSize(16)
                    }
                    RuleMark(y: .value("평균", average))
                        .foregroundStyle(BriefTheme.warning.opacity(0.72))
                        .lineStyle(StrokeStyle(lineWidth: 1.15, dash: [5, 4]))
                }
                .chartYAxis {
                    AxisMarks(position: .leading) { value in
                        AxisGridLine().foregroundStyle(Color.white.opacity(0.06))
                        AxisValueLabel {
                            if let count = value.as(Int.self) { Text(BriefFormat.number(count)) }
                        }
                    }
                }
                .chartXAxis {
                    AxisMarks(values: .stride(by: .day, count: 6)) { value in
                        AxisGridLine().foregroundStyle(Color.white.opacity(0.05))
                        AxisValueLabel {
                            if let date = value.as(Date.self) { Text(date, format: .dateTime.month(.abbreviated).day()) }
                        }
                    }
                }
                .frame(height: 280)
                .accessibilityElement(children: .ignore)
                .accessibilityLabel("월간 일별 판매 추이, 평균 \(average)")
            }
        }
    }

    private var dateRange: String {
        guard let first = points.first?.date, let last = points.last?.date else { return "최근 30일" }
        return "\(first.formatted(.dateTime.year().month().day()))~\(last.formatted(.dateTime.year().month().day()))"
    }
}
