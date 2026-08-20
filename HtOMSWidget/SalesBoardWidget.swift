import SwiftUI
import WidgetKit

struct SalesBoardEntry: TimelineEntry {
    let date: Date
    let snapshot: DashboardWidgetSnapshot
}

struct SalesBoardProvider: TimelineProvider {
    func placeholder(in context: Context) -> SalesBoardEntry {
        SalesBoardEntry(date: .now, snapshot: .sample)
    }

    func getSnapshot(in context: Context, completion: @escaping (SalesBoardEntry) -> Void) {
        completion(SalesBoardEntry(date: .now, snapshot: storedSnapshot))
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<SalesBoardEntry>) -> Void) {
        let entry = SalesBoardEntry(date: .now, snapshot: storedSnapshot)
        let nextRefresh = Calendar.current.date(byAdding: .minute, value: 15, to: .now) ?? .now
        completion(Timeline(entries: [entry], policy: .after(nextRefresh)))
    }

    private var storedSnapshot: DashboardWidgetSnapshot {
        WidgetSnapshotStore().load() ?? .sample
    }
}

struct SalesBoardWidget: Widget {
    private let kind = "SalesBoardWidget"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: SalesBoardProvider()) { entry in
            SalesBoardWidgetView(entry: entry)
        }
        .configurationDisplayName("한통 OMS 현황판")
        .description("오늘과 월간 매출 현황을 공항 안내판처럼 표시합니다.")
        .supportedFamilies([.systemSmall, .systemMedium, .systemLarge])
        .contentMarginsDisabled()
    }
}

private struct SalesBoardWidgetView: View {
    @Environment(\.widgetFamily) private var family
    let entry: SalesBoardEntry

    var body: some View {
        Group {
            switch family {
            case .systemLarge:
                largeBoard
            case .systemSmall:
                smallBoard
            default:
                mediumBoard
            }
        }
        .containerBackground(for: .widget) { BrandPalette.background }
    }

    private var smallBoard: some View {
        VStack(alignment: .leading, spacing: 0) {
            compactValueRow("금일(\(currentDay))", value: entry.snapshot.todayAmount)
            compactValueRow("전일", value: entry.snapshot.yesterdayAmount)
            compactValueRow("당월(\(currentMonth))", value: entry.snapshot.monthTotal)
            compactValueRow("전월", value: entry.snapshot.previousMonthTotal)
            compactValueRow("평균", value: entry.snapshot.dailyAverage)
            compactValueRow("목표(\(achievementRate))", value: entry.snapshot.targetAmount)
            compactTimestampRow("시간", date: entry.snapshot.refreshedAt)
            compactTimestampRow("서버", date: entry.snapshot.serverTime)
        }
        .padding(10)
    }

    private var mediumBoard: some View {
        VStack(alignment: .leading, spacing: 10) {
            boardHeader
            HStack(alignment: .top, spacing: 12) {
                VStack(spacing: 7) {
                    valueRow("금일", value: entry.snapshot.todayAmount)
                    valueRow("전일", value: entry.snapshot.yesterdayAmount)
                }
                VStack(spacing: 7) {
                    valueRow("당월", value: entry.snapshot.monthTotal)
                    valueRow("목표", value: entry.snapshot.targetAmount)
                }
            }
            refreshFooter
        }
        .padding(16)
    }

    private var largeBoard: some View {
        VStack(alignment: .leading, spacing: 9) {
            boardHeader
            valueRow("금일", value: entry.snapshot.todayAmount)
            valueRow("전일", value: entry.snapshot.yesterdayAmount)
            valueRow("당월", value: entry.snapshot.monthTotal)
            valueRow("전월", value: entry.snapshot.previousMonthTotal)
            valueRow("평균", value: entry.snapshot.dailyAverage)
            valueRow("목표", value: entry.snapshot.targetAmount)
            Spacer(minLength: 0)
            timestampRow("갱신", date: entry.snapshot.refreshedAt)
            timestampRow("서버", date: entry.snapshot.serverTime)
        }
        .padding(18)
    }

    private var boardHeader: some View {
        HStack(spacing: 8) {
            Text("현황판")
                .font(.system(.headline, design: .monospaced).weight(.bold))
                .foregroundStyle(.white)
            Text("매출 · 만원")
                .font(.system(.caption, design: .monospaced))
                .foregroundStyle(BrandPalette.mutedText)
            Spacer()
            if entry.snapshot.isSample {
                Text("SAMPLE")
                    .font(.system(.caption2, design: .monospaced).weight(.bold))
                    .foregroundStyle(BrandPalette.orange)
            }
        }
    }

    private func valueRow(_ label: String, value: Int) -> some View {
        HStack(spacing: 8) {
            Text(label)
                .foregroundStyle(BrandPalette.mutedText)
            Spacer(minLength: 6)
            Text(compactWon(value))
                .foregroundStyle(.white)
                .monospacedDigit()
                .privacySensitive()
        }
        .font(.system(.subheadline, design: .monospaced).weight(.semibold))
        .padding(.horizontal, 9)
        .padding(.vertical, 6)
        .background(BrandPalette.boardCell, in: RoundedRectangle(cornerRadius: 6))
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("\(label) 매출 \(value.formatted())원")
    }

    private func compactValueRow(_ label: String, value: Int) -> some View {
        HStack(spacing: 2) {
            Text(label)
                .foregroundStyle(BrandPalette.mutedText)
                .font(.system(size: 15, weight: .bold, design: .monospaced))
                .lineLimit(1)
                .fixedSize(horizontal: true, vertical: false)
                .layoutPriority(1)
            Spacer(minLength: 1)
            Text(compactWon(value))
                .foregroundStyle(.white)
                .font(.system(size: 17, weight: .bold, design: .monospaced))
                .monospacedDigit()
                .privacySensitive()
        }
        .lineLimit(1)
        .minimumScaleFactor(0.9)
    }

    private func compactTimestampRow(_ label: String, date: Date) -> some View {
        HStack(spacing: 4) {
            Text(label)
                .foregroundStyle(BrandPalette.mutedText)
            Spacer(minLength: 2)
            Text(compactTimestamp(date))
                .foregroundStyle(.white)
                .monospacedDigit()
        }
        .font(.system(size: 12, weight: .semibold, design: .monospaced))
        .lineLimit(1)
        .minimumScaleFactor(0.9)
    }

    private var refreshFooter: some View {
        HStack {
            Text("갱신")
            Spacer()
            Text(entry.snapshot.refreshedAt, format: .dateTime.month().day().hour().minute())
        }
        .font(.system(.caption2, design: .monospaced))
        .foregroundStyle(BrandPalette.mutedText)
    }

    private func timestampRow(_ label: String, date: Date) -> some View {
        HStack {
            Text(label)
            Spacer()
            Text(date, format: .dateTime.month().day().hour().minute())
        }
        .font(.system(.caption, design: .monospaced))
        .foregroundStyle(BrandPalette.mutedText)
    }

    private func compactWon(_ value: Int) -> String {
        Int((Double(value) / 10_000).rounded()).formatted(.number.grouping(.automatic))
    }

    private var currentDay: Int {
        Calendar.current.component(.day, from: entry.snapshot.refreshedAt)
    }

    private var currentMonth: String {
        String(format: "%02d", Calendar.current.component(.month, from: entry.snapshot.refreshedAt))
    }

    private var achievementRate: Int {
        guard entry.snapshot.targetAmount > 0 else { return 0 }
        return Int((Double(entry.snapshot.monthTotal) / Double(entry.snapshot.targetAmount) * 100).rounded())
    }

    private func compactTimestamp(_ date: Date) -> String {
        let parts = Calendar.current.dateComponents([.month, .day, .hour, .minute], from: date)
        return String(
            format: "%02d-%02d %02d:%02d",
            parts.month ?? 0,
            parts.day ?? 0,
            parts.hour ?? 0,
            parts.minute ?? 0
        )
    }
}

#Preview(as: .systemMedium) {
    SalesBoardWidget()
} timeline: {
    SalesBoardEntry(date: .now, snapshot: .sample)
}

#Preview(as: .systemSmall) {
    SalesBoardWidget()
} timeline: {
    SalesBoardEntry(date: .now, snapshot: .sample)
}

#Preview(as: .systemLarge) {
    SalesBoardWidget()
} timeline: {
    SalesBoardEntry(date: .now, snapshot: .sample)
}
