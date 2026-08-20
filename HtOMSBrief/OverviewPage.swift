import SwiftUI
import Charts

struct OverviewPage: View {
    let overview: BriefOverview
    var refreshCountdown: Int? = nil
    var onRefresh: (() -> Void)? = nil

    private let columns = [GridItem(.adaptive(minimum: 300), spacing: 16)]

    var body: some View {
        BriefSection(title: "매출 요약", subtitle: "BRIEF · 오늘과 월간 판매") {
            todaySalesCard
            refreshCard
            serverStatusCard

            BriefCard("판매 채널") {
                ChannelMixView(channels: overview.channels)
            }

            LazyVGrid(columns: columns, spacing: 16) {
                metricCard("월 누계 · \(overview.monthLabel) (\(overview.monthProgress)%)", value: overview.monthTotal)
                metricCard("일 평균 · MONTH AVG", value: overview.monthAverage)
                BriefCard("매출 등급 · LEVEL") {
                    DoughnutChartView(unitName: "Level", portions: overview.levelMix)
                }
                BriefCard("상품 분류 · CATEGORY") {
                    DoughnutChartView(unitName: "Category", portions: overview.categoryMix)
                }
            }
        }
    }

    private var serverStatusCard: some View {
        BriefCard("서버 상태 · SERVER") {
            ViewThatFits(in: .horizontal) {
                HStack(spacing: 18) { serverLabels }
                VStack(alignment: .leading, spacing: 8) { serverLabels }
            }
            .frame(maxWidth: .infinity)
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(overview.serverStatuses.map {
                "\($0.name) \($0.isOperational ? "정상" : "문제")"
            }.joined(separator: ", "))
        }
    }

    @ViewBuilder
    private var refreshCard: some View {
        if let onRefresh {
            Button(action: onRefresh) {
                BriefCard("다음 갱신 · REFRESH") {
                    centeredText(refreshText)
                }
            }
            .buttonStyle(.plain)
            .accessibilityHint("OMS 데이터를 다시 불러옵니다")
        } else {
            BriefCard("다음 갱신 · REFRESH") {
                centeredText(refreshText)
            }
        }
    }

    private var refreshText: String {
        guard let refreshCountdown else { return overview.refreshedAt }
        return String(format: "%02d:%02d", refreshCountdown / 60, refreshCountdown % 60)
    }

    @ViewBuilder
    private var serverLabels: some View {
        ForEach(overview.serverStatuses) { server in
            Text(server.name)
                .font(.headline)
                .foregroundStyle(server.isOperational ? BriefTheme.mutedText : BriefTheme.negative)
        }
    }

    private var todaySalesCard: some View {
        BriefCard("오늘 매출 · TODAY") {
            VStack(alignment: .leading, spacing: 12) {
                HStack(alignment: .center, spacing: 12) {
                Text(overview.todaySales.level)
                    .font(.headline)
                    .foregroundStyle(.white)
                    .padding(.horizontal, 18)
                    .padding(.vertical, 10)
                    .background(BriefTheme.warning.opacity(0.72), in: RoundedRectangle(cornerRadius: 10))

                Spacer(minLength: 8)

                Text("\(overview.todaySales.day)일")
                    .font(.title3.weight(.semibold))
                    .foregroundStyle(BriefTheme.accent)
                }

                HStack(alignment: .lastTextBaseline, spacing: 8) {
                    Text(BriefFormat.number(overview.todaySales.amount))
                        .font(.system(size: 48, weight: .bold, design: .monospaced))
                        .monospacedDigit()
                        .minimumScaleFactor(0.62)
                        .lineLimit(1)
                        .foregroundStyle(.white)
                        .contentTransition(.numericText())
                    Text("만원")
                        .font(.system(.headline, design: .monospaced).weight(.semibold))
                        .foregroundStyle(BriefTheme.mutedText)
                }
            }
            .accessibilityElement(children: .ignore)
            .accessibilityLabel("오늘 \(overview.todaySales.day)일 매출 \(BriefFormat.number(overview.todaySales.amount)), 등급 \(overview.todaySales.level)")
        }
    }

    private func metricCard(_ title: String, value: Int) -> some View {
        BriefCard(title) {
            centeredText(BriefFormat.number(value))
        }
    }

    private func centeredText(_ value: String) -> some View {
        Text(value)
            .font(.system(.title2, design: .monospaced).weight(.semibold))
            .monospacedDigit()
            .foregroundStyle(.white)
            .frame(maxWidth: .infinity)
    }
}

struct ChannelMixView: View {
    let channels: [ChannelShare]

    var body: some View {
        VStack(spacing: 14) {
            GeometryReader { proxy in
                HStack(spacing: 0) {
                    ForEach(Array(channels.enumerated()), id: \.element.id) { index, channel in
                        BriefTheme.seriesPalette[index % BriefTheme.seriesPalette.count]
                            .frame(width: proxy.size.width * CGFloat(channel.percentage) / 100)
                    }
                }
            }
            .frame(height: 20)
            .clipShape(RoundedRectangle(cornerRadius: 2))

            ViewThatFits(in: .horizontal) {
                HStack(spacing: 18) { channelLabels }
                VStack(alignment: .leading, spacing: 8) { channelLabels }
            }
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(channels.map { "\($0.name) \(BriefFormat.number($0.count)), \($0.percentage)퍼센트" }.joined(separator: ", "))
    }

    @ViewBuilder
    private var channelLabels: some View {
        ForEach(Array(channels.enumerated()), id: \.element.id) { index, channel in
            HStack(spacing: 6) {
                Rectangle()
                    .fill(BriefTheme.seriesPalette[index % BriefTheme.seriesPalette.count])
                    .frame(width: 12, height: 12)
                Text("\(channel.name): \(BriefFormat.number(channel.count)) [\(channel.percentage)%]")
                    .font(.footnote)
                    .foregroundStyle(BriefTheme.mutedText)
            }
        }
    }
}
