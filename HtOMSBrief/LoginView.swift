import SwiftUI

/// 공항 안내판 스타일의 로그인 화면.
struct LoginView: View {
    @ObservedObject var controller: SessionController
    let authService: any AuthServicing

    @State private var username = ""
    @State private var password = ""
    @State private var isPasswordRevealed = false
    @State private var isSubmitting = false
    @State private var statusMessage: String?
    @FocusState private var focusedField: Field?

    private enum Field { case username, password }

    var body: some View {
        ZStack {
            BriefTheme.background.ignoresSafeArea()

            ScrollView {
                VStack(spacing: 28) {
                    header
                    statusBoard
                    credentialCard
                }
                .frame(maxWidth: 460)
                .padding(.horizontal, 20)
                .padding(.top, 48)
                .padding(.bottom, 40)
                .frame(maxWidth: .infinity)
            }
            .scrollDismissesKeyboard(.interactively)
        }
        .preferredColorScheme(.dark)
    }

    // MARK: - 안내판 헤더

    private var header: some View {
        VStack(alignment: .leading, spacing: 10) {
            flapText("HTOMS BRIEF")
            Text("운영 브리핑 정보 보드")
                .font(.system(.subheadline, design: .monospaced))
                .foregroundStyle(BriefTheme.mutedText)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .accessibilityElement(children: .combine)
        .accessibilityAddTraits(.isHeader)
    }

    /// 스플릿 플랩 안내판처럼 글자를 한 칸씩 나눠 그린다.
    private func flapText(_ text: String) -> some View {
        HStack(spacing: 3) {
            ForEach(Array(text.enumerated()), id: \.offset) { _, character in
                Text(String(character))
                    .font(.system(size: 21, weight: .bold, design: .monospaced))
                    .foregroundStyle(character == " " ? Color.clear : BriefTheme.boardAmber)
                    .frame(width: 21, height: 34)
                    .background(
                        character == " " ? Color.clear : BriefTheme.boardCell,
                        in: RoundedRectangle(cornerRadius: 4)
                    )
            }
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(text)
    }

    // MARK: - 시스템 상태 보드

    private var statusBoard: some View {
        VStack(spacing: 1) {
            boardRow(code: "AUTH", label: "사내 계정 인증", state: "LOGIN", color: BriefTheme.boardAmber)
            boardRow(code: "KEY", label: "이 기기 Keychain 보관", state: "LOCAL", color: BriefTheme.mutedText)
            if let statusMessage {
                HStack(alignment: .top, spacing: 8) {
                    Image(systemName: "exclamationmark.triangle.fill")
                        .foregroundStyle(BriefTheme.negative)
                    Text(statusMessage)
                        .font(.footnote)
                        .foregroundStyle(BriefTheme.negative)
                        .frame(maxWidth: .infinity, alignment: .leading)
                }
                .padding(12)
                .background(BriefTheme.boardCell)
                .accessibilityElement(children: .combine)
            }
        }
        .background(BriefTheme.cardStroke)
        .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 10, style: .continuous)
                .strokeBorder(BriefTheme.cardStroke)
        )
    }

    private func boardRow(code: String, label: String, state: String, color: Color) -> some View {
        HStack(spacing: 12) {
            Text(code)
                .font(.system(.footnote, design: .monospaced).weight(.bold))
                .foregroundStyle(BriefTheme.boardAmber)
                .frame(width: 56, alignment: .leading)
            Text(label)
                .font(.footnote)
                .foregroundStyle(.white)
            Spacer()
            HStack(spacing: 6) {
                Circle().fill(color).frame(width: 7, height: 7)
                Text(state)
                    .font(.system(.footnote, design: .monospaced).weight(.semibold))
                    .foregroundStyle(color)
            }
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 12)
        .background(BriefTheme.boardCell)
        .accessibilityElement(children: .combine)
        .accessibilityLabel("\(label) 상태 \(state)")
    }

    // MARK: - 로그인 입력

    private var credentialCard: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("로그인")
                .font(.system(.headline, design: .monospaced))
                .foregroundStyle(.white)

            fieldContainer {
                TextField("이메일", text: $username)
                    .textContentType(.emailAddress)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                    .keyboardType(.emailAddress)
                    .focused($focusedField, equals: .username)
                    .submitLabel(.next)
                    .onSubmit { focusedField = .password }
                    .accessibilityLabel("이메일")
            }

            fieldContainer {
                Group {
                    if isPasswordRevealed {
                        TextField("비밀번호", text: $password)
                            .textInputAutocapitalization(.never)
                            .autocorrectionDisabled()
                    } else {
                        SecureField("비밀번호", text: $password)
                    }
                }
                .textContentType(.password)
                .focused($focusedField, equals: .password)
                .submitLabel(.go)
                .onSubmit(submit)
                .accessibilityLabel("비밀번호")

                Button {
                    isPasswordRevealed.toggle()
                } label: {
                    Image(systemName: isPasswordRevealed ? "eye.slash" : "eye")
                        .foregroundStyle(BriefTheme.mutedText)
                }
                .accessibilityLabel(isPasswordRevealed ? "비밀번호 숨기기" : "비밀번호 표시")
            }

            Button(action: submit) {
                HStack {
                    if isSubmitting {
                        ProgressView().tint(Color.black.opacity(0.7))
                    }
                    Text("로그인")
                        .font(.headline)
                }
                .frame(maxWidth: .infinity)
                .padding(.vertical, 14)
                .background(BriefTheme.boardAmber, in: RoundedRectangle(cornerRadius: 10))
                .foregroundStyle(Color(red: 0.10, green: 0.09, blue: 0.03))
            }
            .disabled(isSubmitting)
            .accessibilityLabel("로그인")
            .accessibilityHint("사내 OMS 계정으로 로그인합니다")

            Text("사내 OMS 계정으로 로그인합니다. 인증 토큰은 이 기기의 Keychain에만 저장됩니다.")
                .font(.caption)
                .foregroundStyle(BriefTheme.mutedText)
        }
        .padding(18)
        .background(BriefTheme.card, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 14, style: .continuous)
                .strokeBorder(BriefTheme.cardStroke)
        )
    }

    private func fieldContainer<Content: View>(@ViewBuilder content: () -> Content) -> some View {
        HStack(spacing: 10) {
            content()
        }
        .font(.system(.body, design: .monospaced))
        .foregroundStyle(.white)
        .padding(.horizontal, 14)
        .padding(.vertical, 12)
        .background(BriefTheme.boardCell, in: RoundedRectangle(cornerRadius: 10))
        .overlay(
            RoundedRectangle(cornerRadius: 10)
                .strokeBorder(BriefTheme.cardStroke)
        )
    }

    private func submit() {
        guard !isSubmitting else { return }
        isSubmitting = true
        statusMessage = nil

        let name = username.trimmingCharacters(in: .whitespaces)
        let secret = password

        Task { @MainActor in
            defer { isSubmitting = false }
            do {
                let session = try await authService.authenticate(username: name, password: secret)
                try controller.establish(session)
            } catch {
                password = ""
                statusMessage = (error as? LocalizedError)?.errorDescription
                    ?? "로그인에 실패했습니다. 잠시 후 다시 시도해 주세요."
            }
        }
    }
}
