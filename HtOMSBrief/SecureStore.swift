import Foundation
import Security

/// 세션 토큰 등 비밀값 저장을 추상화한 인터페이스.
/// 실제 앱은 Keychain 구현을 쓰고, 테스트는 메모리 대역을 주입한다.
protocol SecureStoring: Sendable {
    func save(_ data: Data, forKey key: String) throws
    func load(forKey key: String) throws -> Data?
    func delete(forKey key: String) throws
}

/// iOS Keychain(kSecClassGenericPassword) 기반 구현.
/// UserDefaults 등 비보호 저장소는 사용하지 않는다.
struct KeychainSecureStore: SecureStoring {
    enum KeychainError: Error, Equatable {
        case unexpectedStatus(OSStatus)
    }

    let service: String

    init(service: String = "com.htoms.brief") {
        self.service = service
    }

    private func baseQuery(for key: String) -> [String: Any] {
        [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: key,
        ]
    }

    func save(_ data: Data, forKey key: String) throws {
        let update: [String: Any] = [kSecValueData as String: data]
        let updateStatus = SecItemUpdate(baseQuery(for: key) as CFDictionary, update as CFDictionary)
        if updateStatus == errSecSuccess { return }
        guard updateStatus == errSecItemNotFound else {
            throw KeychainError.unexpectedStatus(updateStatus)
        }

        var addQuery = baseQuery(for: key)
        addQuery[kSecValueData as String] = data
        addQuery[kSecAttrAccessible as String] = kSecAttrAccessibleWhenUnlockedThisDeviceOnly
        let addStatus = SecItemAdd(addQuery as CFDictionary, nil)
        guard addStatus == errSecSuccess else {
            throw KeychainError.unexpectedStatus(addStatus)
        }
    }

    func load(forKey key: String) throws -> Data? {
        var query = baseQuery(for: key)
        query[kSecReturnData as String] = true
        query[kSecMatchLimit as String] = kSecMatchLimitOne

        var result: CFTypeRef?
        let status = SecItemCopyMatching(query as CFDictionary, &result)
        if status == errSecItemNotFound { return nil }
        guard status == errSecSuccess else {
            throw KeychainError.unexpectedStatus(status)
        }
        return result as? Data
    }

    func delete(forKey key: String) throws {
        let status = SecItemDelete(baseQuery(for: key) as CFDictionary)
        guard status == errSecSuccess || status == errSecItemNotFound else {
            throw KeychainError.unexpectedStatus(status)
        }
    }
}
