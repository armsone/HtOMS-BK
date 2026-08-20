import SwiftUI
import Charts

struct DayTrendPage: View {
    let points: [DailyPoint]

    private var plottedPoints: [(point: DailyPoint, hour: Int)] {
        points.compactMap { point in hour(from: point.label).map { (point, $0) } }
    }

    private var axisHours: [Int] {
        guard let first = plottedPoints.first?.hour, let last = plottedPoints.last?.hour else { return [] }
        return Array(stride(from: first, through: last, by: 2))
    }

    private var xDomain: ClosedRange<Int> {
        guard let first = plottedPoints.first?.hour, let last = plottedPoints.last?.hour else { return 0...1 }
        return (first - 1)...(last + 1)
    }

    private var accessibilitySummary: String {
        guard let latest = points.last(where: { $0.result != nil }), let result = latest.result else {
            return "시간대별 매출. 오늘 집계 데이터가 없습니다."
        }
        return "시간대별 매출 비교. 오렌지는 오늘 매출 10배, 회색은 최근 30일 합계. 최신 \(latest.label) 오늘 비교값 \(BriefFormat.number(result))"
    }

    var body: some View {
        BriefSection(title: "시간대별 매출", subtitle: "DAY · 기존 OMS 비교 기준") {
            BriefCard("시간대별 매출") {
                HStack(spacing: 14) {
                    legend(color: BriefTheme.warning, label: "오늘 ×10", dashed: false)
                    legend(color: Color.white.opacity(0.66), label: "30일 합계", dashed: true)
                    Spacer()
                    Text("만원 비교값")
                        .font(.caption)
                        .foregroundStyle(BriefTheme.mutedText)
                }

                Chart(plottedPoints, id: \.point.id) { item in
                    let point = item.point
                    let hour = item.hour
                    if let reference = point.reference {
                        LineMark(
                            x: .value("시간", hour),
                            y: .value("매출", reference),
                            series: .value("계열", "30일 합계")
                        )
                            .foregroundStyle(Color.white.opacity(0.66))
                            .lineStyle(StrokeStyle(lineWidth: 1.15, dash: [4, 4]))
                        PointMark(x: .value("시간", hour), y: .value("30일 합계", reference))
                            .foregroundStyle(Color.white.opacity(0.72))
                            .symbolSize(12)
                    }

                    if let result = point.result {
                        LineMark(
                            x: .value("시간", hour),
                            y: .value("매출", result),
                            series: .value("계열", "오늘")
                        )
                            .foregroundStyle(BriefTheme.warning)
                            .lineStyle(StrokeStyle(lineWidth: 1.6))
                        PointMark(x: .value("시간", hour), y: .value("오늘", result))
                            .foregroundStyle(BriefTheme.warning)
                            .symbolSize(16)
                    }
                }
                .chartXScale(domain: xDomain)
                .chartYAxis {
                    AxisMarks(position: .leading) { value in
                        AxisGridLine().foregroundStyle(Color.white.opacity(0.06))
                        AxisValueLabel {
                            if let count = value.as(Int.self) { Text(BriefFormat.number(count)) }
                        }
                    }
                }
                .chartXAxis {
                    AxisMarks(values: axisHours) { value in
                        AxisGridLine().foregroundStyle(Color.white.opacity(0.05))
                        AxisValueLabel {
                            if let hour = value.as(Int.self) {
                                Text("\(hour)시")
                                    .monospacedDigit()
                            }
                        }
                    }
                }
                .frame(height: 240)
                .accessibilityElement(children: .ignore)
                .accessibilityLabel(accessibilitySummary)
            }
        }
    }

    private func legend(color: Color, label: String, dashed: Bool) -> some View {
        HStack(spacing: 6) {
            Capsule()
                .stroke(style: StrokeStyle(lineWidth: 1.5, dash: dashed ? [4, 3] : []))
                .foregroundStyle(color)
                .frame(width: 22, height: 2)
            Text(label)
                .font(.caption.weight(.semibold))
                .foregroundStyle(BriefTheme.mutedText)
        }
        .accessibilityElement(children: .combine)
    }

    private func hour(from label: String) -> Int? {
        Int(label.filter(\.isNumber))
    }
}
