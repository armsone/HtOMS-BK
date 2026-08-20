import XCTest
@testable import HtOMSBrief

/// 실제 Keychain을 건드리지 않는 메모리 기반 SecureStoring 대역.
final class InMemorySecureStore: SecureStoring, @unchecked Sendable {
    private(set) var storage: [String: Data] = [:]

    func save(_ data: Data, forKey key: String) throws {
        storage[key] = data
    }

    func load(forKey key: String) throws -> Data? {
        storage[key]
    }

    func delete(forKey key: String) throws {
        storage[key] = nil
    }
}

final class SessionControllerTests: XCTestCase {
    private let sampleSession = AuthSession(
        token: "test-token-123",
        username: "tester",
        issuedAt: Date(timeIntervalSince1970: 1_700_000_000)
    )

    func testColdLaunchWithoutStoredSessionIsLoggedOut() {
        let controller = SessionController(store: InMemorySecureStore())
        XCTAssertFalse(controller.isAuthenticated)
        XCTAssertNil(controller.session)
    }

    func testEstablishPersistsSessionInSecureStore() throws {
        let store = InMemorySecureStore()
        let controller = SessionController(store: store)

        try controller.establish(sampleSession)

        XCTAssertTrue(controller.isAuthenticated)
        XCTAssertEqual(controller.session, sampleSession)
        let stored = try XCTUnwrap(store.storage[SessionController.storageKey])
        let decoded = try JSONDecoder().decode(AuthSession.self, from: stored)
        XCTAssertEqual(decoded, sampleSession)
    }

    func testSessionIsRestoredOnNextLaunchFromSameStore() throws {
        let store = InMemorySecureStore()
        try SessionController(store: store).establish(sampleSession)

        // 같은 저장소로 새 컨트롤러를 만들면(다음 실행) 세션이 복원돼야 한다.
        let relaunched = SessionController(store: store)
        XCTAssertTrue(relaunched.isAuthenticated)
        XCTAssertEqual(relaunched.session, sampleSession)
    }

    func testLogoutClearsSessionAndStore() throws {
        let store = InMemorySecureStore()
        let controller = SessionController(store: store)
        try controller.establish(sampleSession)

        controller.logout()

        XCTAssertFalse(controller.isAuthenticated)
        XCTAssertNil(store.storage[SessionController.storageKey])
        XCTAssertFalse(SessionController(store: store).isAuthenticated)
    }

    func testCorruptedStoredSessionFallsBackToLoggedOut() throws {
        let store = InMemorySecureStore()
        try store.save(Data("not-json".utf8), forKey: SessionController.storageKey)

        let controller = SessionController(store: store)
        XCTAssertFalse(controller.isAuthenticated)
        XCTAssertNil(store.storage[SessionController.storageKey])
    }

    func testEmptyTokenCannotEstablishSession() {
        let controller = SessionController(store: InMemorySecureStore())
        let invalid = AuthSession(token: "", username: "tester", issuedAt: .now)

        XCTAssertThrowsError(try controller.establish(invalid)) { error in
            XCTAssertEqual(error as? AuthError, .invalidSession)
        }
        XCTAssertFalse(controller.isAuthenticated)
    }

    func testUnconnectedAuthServiceNeverAuthenticates() async {
        let service = UnconnectedAuthService()

        await XCTAssertThrowsAuthError(.emptyCredentials) {
            _ = try await service.authenticate(username: "", password: "")
        }
        await XCTAssertThrowsAuthError(.serverNotConnected) {
            _ = try await service.authenticate(username: "anyone", password: "anything")
        }
    }

    private func XCTAssertThrowsAuthError(
        _ expected: AuthError,
        _ body: () async throws -> Void,
        file: StaticString = #filePath,
        line: UInt = #line
    ) async {
        do {
            try await body()
            XCTFail("에러가 발생해야 합니다", file: file, line: line)
        } catch {
            XCTAssertEqual(error as? AuthError, expected, file: file, line: line)
        }
    }
}
