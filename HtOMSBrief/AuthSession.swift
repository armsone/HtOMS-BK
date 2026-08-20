import Foundation
import Combine

/// 인증 성공 후 서버가 발급하는 세션. 토큰은 Keychain에만 보관된다.
struct AuthSession: Codable, Equatable, Sendable {
    let token: String
    let username: String
    let issuedAt: Date
}

enum AuthError: LocalizedError, Equatable {
    case emptyCredentials
    case invalidSession
    case serverNotConnected

    var errorDescription: String? {
        switch self {
        case .emptyCredentials:
            return "아이디와 비밀번호를 모두 입력해 주세요."
        case .invalidSession:
            return "유효한 인증 세션을 만들 수 없습니다."
        case .serverNotConnected:
            return "서버 인증이 아직 연결되지 않았습니다. OMS 인증 API 연동 후 로그인할 수 있습니다."
        }
    }
}

/// 향후 인증 API가 구현할 인터페이스.
protocol AuthServicing: Sendable {
    func authenticate(username: String, password: String) async throws -> AuthSession
}

/// 인증 API 연동 전까지 사용하는 서비스. 어떤 입력으로도 인증되지 않으며,
/// 자격 증명을 저장하거나 로그로 남기지 않는다.
struct UnconnectedAuthService: AuthServicing {
    func authenticate(username: String, password: String) async throws -> AuthSession {
        guard !username.isEmpty, !password.isEmpty else {
            throw AuthError.emptyCredentials
        }
        throw AuthError.serverNotConnected
    }
}

/// 세션 수립·복원·해제를 담당하는 상태 컨트롤러.
/// 토큰은 주입된 SecureStoring(앱에서는 Keychain)에만 기록한다.
final class SessionController: ObservableObject {
    static let storageKey = "auth-session"

    @Published private(set) var session: AuthSession?

    private let store: any SecureStoring

    init(store: any SecureStoring) {
        self.store = store
        restore()
    }

    var isAuthenticated: Bool { session != nil }

    /// 앱 시작 시 저장소에 남아 있는 유효한 세션을 복원한다.
    func restore() {
        guard let data = try? store.load(forKey: Self.storageKey),
              let restored = try? JSONDecoder().decode(AuthSession.self, from: data),
              !restored.token.isEmpty else {
            try? store.delete(forKey: Self.storageKey)
            session = nil
            return
        }
        session = restored
    }

    /// 인증 성공 시(향후 API 또는 테스트 주입) 세션을 저장소에 기록하고 활성화한다.
    func establish(_ newSession: AuthSession) throws {
        guard !newSession.token.isEmpty else { throw AuthError.invalidSession }
        let data = try JSONEncoder().encode(newSession)
        try store.save(data, forKey: Self.storageKey)
        session = newSession
    }

    /// 저장소의 세션을 제거하고 로그인 화면으로 되돌린다.
    func logout() {
        guard (try? store.delete(forKey: Self.storageKey)) != nil else { return }
        session = nil
    }
}
