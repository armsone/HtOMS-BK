import SwiftUI

struct DeliveryPage: View {
    let summary: DeliverySummary

    private let columns = [GridItem(.adaptive(minimum: 145), spacing: 12)]

    var body: some View {
        BriefSection(title: "택배 현황", subtitle: summary.dateRange) {
            BriefCard("전체 배송 · DELIVERY") {
                HStack(alignment: .lastTextBaseline, spacing: 8) {
                    Text(BriefFormat.number(summary.total))
                        .font(.system(size: 44, weight: .bold, design: .monospaced))
                        .monospacedDigit()
                        .foregroundStyle(.white)
                    Text("건")
                        .font(.headline)
                        .foregroundStyle(BriefTheme.mutedText)
                }
                .accessibilityElement(children: .combine)

                Divider().overlay(BriefTheme.mutedText.opacity(0.35))

                LazyVGrid(columns: columns, spacing: 14) {
                    ForEach(summary.statuses) { item in
                        HStack {
                            Text(item.status.rawValue)
                                .foregroundStyle(BriefTheme.color(for: item.status))
                            Spacer()
                            Text("\(BriefFormat.number(item.count)) 건")
                                .foregroundStyle(BriefTheme.color(for: item.status))
                                .monospacedDigit()
                        }
                        .font(.title3)
                        .accessibilityElement(children: .combine)
                    }
                }
            }
        }
    }
}
