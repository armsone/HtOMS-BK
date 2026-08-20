import SwiftUI
import WidgetKit

@main
struct HtOMSBriefApp: App {
    @StateObject private var session = SessionController(store: KeychainSecureStore())

    var body: some Scene {
        WindowGroup {
            AppRootView(session: session)
        }
    }
}

/// 인증 게이트: 세션이 없으면 업무 데이터를 로드하지 않고 로그인 화면만 보여 준다.
struct AppRootView: View {
    @ObservedObject var session: SessionController

    var body: some View {
        #if DEBUG
        if ProcessInfo.processInfo.arguments.contains("-ui-catalog-login") {
            LoginView(controller: session, authService: UnconnectedAuthService())
        } else if isCatalogBrief {
            BriefRootView(provider: SampleBriefProvider(), onLogout: {})
        } else {
            authenticatedContent
        }
        #else
        authenticatedContent
        #endif
    }

    @ViewBuilder
    private var authenticatedContent: some View {
        if let activeSession = session.session {
            BriefRootView(
                provider: RemoteBriefProvider(token: activeSession.token),
                onLogout: logout
            )
            .id(activeSession.token)
        } else {
            LoginView(controller: session, authService: OMSAuthService())
        }
    }

    #if DEBUG
    private var isCatalogBrief: Bool {
        // 과거 4페이지 시절의 "-ui-catalog-page <n>"도 통합 페이지로 이어지도록 허용한다.
        let arguments = ProcessInfo.processInfo.arguments
        return arguments.contains("-ui-catalog-brief") || arguments.contains("-ui-catalog-page")
    }
    #endif

    private func logout() {
        session.logout()
        try? WidgetSnapshotStore().remove()
        WidgetCenter.shared.reloadAllTimelines()
    }
}
