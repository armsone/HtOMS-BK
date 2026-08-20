import SwiftUI
import WidgetKit

/// 모든 브리프 섹션을 세로 스크롤 한 페이지로 보여 주는 루트 뷰.
struct BriefRootView: View {
    let provider: any BriefProviding
    var onLogout: (() -> Void)? = nil

    @State private var snapshot: BriefSnapshot?
    @State private var loadError: String?
    @State private var isLoading = false
    @State private var refreshCountdown = 10 * 60
    @State private var nextRefreshAt = Date().addingTimeInterval(10 * 60)

    var body: some View {
        ZStack {
            BriefTheme.background.ignoresSafeArea()

            if let snapshot {
                ScrollView {
                    VStack(alignment: .leading, spacing: 36) {
                        OverviewPage(
                            overview: snapshot.overview,
                            refreshCountdown: refreshCountdown,
                            onRefresh: { Task { await load() } }
                        )
                        DayTrendPage(points: snapshot.dayTrend)
                        MonthTrendPage(points: snapshot.monthTrend, average: snapshot.monthAverage)
                        DeliveryPage(summary: snapshot.deliverySummary)
                    }
                    .frame(maxWidth: 1000)
                    .padding(.horizontal, 16)
                    .padding(.top, 20)
                    .padding(.bottom, 48)
                    .frame(maxWidth: .infinity)
                }
            } else if let loadError {
                ContentUnavailableView {
                    Label("브리프를 불러오지 못했습니다", systemImage: "wifi.exclamationmark")
                } description: {
                    Text(loadError)
                } actions: {
                    Button("다시 불러오기") {
                        Task { await load() }
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(BriefTheme.boardAmber)
                }
                .foregroundStyle(BriefTheme.mutedText)
            } else {
                ProgressView("브리프 불러오는 중")
                    .tint(BriefTheme.accent)
                    .foregroundStyle(BriefTheme.mutedText)
            }
        }
        .overlay(alignment: .top) {
            if snapshot != nil, let loadError {
                Label("갱신 실패 · 이전 데이터 표시 중", systemImage: "exclamationmark.triangle.fill")
                    .font(.system(.caption, design: .monospaced).weight(.semibold))
                    .foregroundStyle(BriefTheme.negative)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 8)
                    .background(BriefTheme.boardCell, in: Capsule())
                    .accessibilityLabel("갱신 실패. 이전 데이터를 표시합니다. \(loadError)")
                    .padding(.top, 8)
            }
        }
        .safeAreaInset(edge: .top, spacing: 0) {
            if let onLogout {
                HStack {
                    Text("HTOMS BRIEF")
                        .font(.system(.footnote, design: .monospaced).weight(.bold))
                        .foregroundStyle(BriefTheme.boardAmber)
                    Spacer()
                    Button {
                        Task { await load() }
                    } label: {
                        if isLoading {
                            ProgressView()
                                .controlSize(.small)
                        } else {
                            Image(systemName: "arrow.clockwise")
                        }
                    }
                    .disabled(isLoading)
                    .foregroundStyle(BriefTheme.mutedText)
                    .accessibilityLabel("데이터 새로고침")
                    .accessibilityHint("OMS의 최신 조회 데이터를 다시 불러옵니다")

                    Button("로그아웃", action: onLogout)
                        .font(.footnote.weight(.semibold))
                        .foregroundStyle(BriefTheme.mutedText)
                        .accessibilityHint("세션을 종료하고 로그인 화면으로 돌아갑니다")
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 8)
                .background(BriefTheme.boardCell)
                .overlay(alignment: .bottom) {
                    Rectangle()
                        .fill(BriefTheme.boardAmber.opacity(0.35))
                        .frame(height: 1)
                }
            }
        }
        .preferredColorScheme(.dark)
        .task {
            await load()
            while !Task.isCancelled {
                do {
                    try await Task.sleep(for: .seconds(1))
                } catch {
                    return
                }
                refreshCountdown = max(0, Int(ceil(nextRefreshAt.timeIntervalSinceNow)))
                if refreshCountdown == 0 {
                    await load()
                }
            }
        }
    }

    @MainActor
    private func load() async {
        guard !isLoading else { return }
        isLoading = true
        loadError = nil
        defer {
            isLoading = false
            refreshCountdown = 10 * 60
            nextRefreshAt = Date().addingTimeInterval(10 * 60)
        }
        do {
            let loaded = try await provider.loadSnapshot()
            snapshot = loaded
            if let widget = loaded.widgetSnapshot {
                try? WidgetSnapshotStore().save(widget)
                WidgetCenter.shared.reloadAllTimelines()
            }
        } catch {
            loadError = (error as? LocalizedError)?.errorDescription
                ?? "네트워크 연결을 확인한 뒤 다시 시도해 주세요."
        }
    }
}

#Preview {
    BriefRootView(provider: SampleBriefProvider())
}
