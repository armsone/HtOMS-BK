package com.htoms.brief.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.htoms.brief.auth.AuthServicing
import com.htoms.brief.auth.SessionController
import com.htoms.brief.theme.BriefTheme
import kotlinx.coroutines.launch

/** 공항 안내판 스타일의 로그인 화면. */
@Composable
fun LoginScreen(
    controller: SessionController,
    authService: AuthServicing
) {
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var isPasswordRevealed by rememberSaveable { mutableStateOf(false) }
    var isSubmitting by rememberSaveable { mutableStateOf(false) }
    var statusMessage by rememberSaveable { mutableStateOf<String?>(null) }
    val passwordFocus = remember { FocusRequester() }
    val scope = rememberCoroutineScope()

    fun submit() {
        if (isSubmitting) return
        isSubmitting = true
        statusMessage = null
        val name = username.trim()
        val secret = password
        scope.launch {
            try {
                val session = authService.authenticate(username = name, password = secret)
                controller.establish(session)
            } catch (error: Exception) {
                password = ""
                statusMessage = error.message ?: "로그인에 실패했습니다. 잠시 후 다시 시도해 주세요."
            } finally {
                isSubmitting = false
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BriefTheme.background)
            .imePadding()
            .verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 460.dp)
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 48.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            Header()
            StatusBoard(statusMessage = statusMessage)
            CredentialCard(
                username = username,
                onUsernameChange = { username = it },
                password = password,
                onPasswordChange = { password = it },
                isPasswordRevealed = isPasswordRevealed,
                onToggleReveal = { isPasswordRevealed = !isPasswordRevealed },
                isSubmitting = isSubmitting,
                passwordFocus = passwordFocus,
                onSubmit = ::submit
            )
        }
    }
}

@Composable
private fun Header() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                heading()
                contentDescription = "HTOMS BRIEF, 운영 브리핑 정보 보드"
            },
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        FlapText("HTOMS BRIEF")
        Text(
            text = "운영 브리핑 정보 보드",
            fontSize = 15.sp,
            fontFamily = FontFamily.Monospace,
            color = BriefTheme.mutedText
        )
    }
}

/** 스플릿 플랩 안내판처럼 글자를 한 칸씩 나눠 그린다. */
@Composable
private fun FlapText(text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        text.forEach { character ->
            Box(
                modifier = Modifier
                    .size(width = 21.dp, height = 34.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (character == ' ') Color.Transparent else BriefTheme.boardCell),
                contentAlignment = Alignment.Center
            ) {
                if (character != ' ') {
                    Text(
                        text = character.toString(),
                        fontSize = 21.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = BriefTheme.boardAmber
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusBoard(statusMessage: String?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(BriefTheme.cardStroke)
            .border(1.dp, BriefTheme.cardStroke, RoundedCornerShape(10.dp))
            .testTag("login-status-board"),
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        BoardRow(code = "AUTH", label = "사내 계정 인증", state = "LOGIN", color = BriefTheme.boardAmber)
        BoardRow(code = "KEY", label = "이 기기 Keystore 보관", state = "LOCAL", color = BriefTheme.mutedText)
        if (statusMessage != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BriefTheme.boardCell)
                    .padding(12.dp)
                    .semantics(mergeDescendants = true) {}
                    .testTag("login-error"),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = null,
                    tint = BriefTheme.negative,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = statusMessage,
                    fontSize = 13.sp,
                    color = BriefTheme.negative,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun BoardRow(code: String, label: String, state: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BriefTheme.boardCell)
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "$label 상태 $state"
            },
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = code,
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = BriefTheme.boardAmber,
            modifier = Modifier.width(56.dp)
        )
        Text(text = label, fontSize = 13.sp, color = Color.White)
        Spacer(modifier = Modifier.weight(1f))
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Text(
                text = state,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                color = color
            )
        }
    }
}

@Composable
private fun CredentialCard(
    username: String,
    onUsernameChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    isPasswordRevealed: Boolean,
    onToggleReveal: () -> Unit,
    isSubmitting: Boolean,
    passwordFocus: FocusRequester,
    onSubmit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(BriefTheme.card)
            .border(1.dp, BriefTheme.cardStroke, RoundedCornerShape(14.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "로그인",
            fontSize = 17.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )

        FieldContainer {
            LoginTextField(
                value = username,
                onValueChange = onUsernameChange,
                placeholder = "이메일",
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                    autoCorrectEnabled = false
                ),
                keyboardActions = KeyboardActions(onNext = { passwordFocus.requestFocus() }),
                visualTransformation = VisualTransformation.None,
                modifier = Modifier
                    .weight(1f)
                    .semantics { contentDescription = "이메일" }
                    .testTag("login-email")
            )
        }

        FieldContainer {
            LoginTextField(
                value = password,
                onValueChange = onPasswordChange,
                placeholder = "비밀번호",
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Go,
                    autoCorrectEnabled = false
                ),
                keyboardActions = KeyboardActions(onGo = { onSubmit() }),
                visualTransformation = if (isPasswordRevealed) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(passwordFocus)
                    .semantics { contentDescription = "비밀번호" }
                    .testTag("login-password")
            )
            IconButton(
                onClick = onToggleReveal,
                modifier = Modifier
                    .size(28.dp)
                    .testTag("login-reveal-toggle")
            ) {
                Icon(
                    imageVector = if (isPasswordRevealed) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    contentDescription = if (isPasswordRevealed) "비밀번호 숨기기" else "비밀번호 표시",
                    tint = BriefTheme.mutedText,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .focusBorder(cornerRadius = 10.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(BriefTheme.boardAmber)
                .clickable(enabled = !isSubmitting, onClick = onSubmit)
                .padding(vertical = 14.dp)
                .semantics(mergeDescendants = true) {
                    contentDescription = "로그인. 사내 OMS 계정으로 로그인합니다"
                }
                .testTag("login-submit"),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(
                    color = Color.Black.copy(alpha = 0.7f),
                    strokeWidth = 2.dp,
                    modifier = Modifier
                        .size(18.dp)
                        .testTag("login-progress")
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = "로그인",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1A1708),
                textAlign = TextAlign.Center
            )
        }

        Text(
            text = "사내 OMS 계정으로 로그인합니다. 인증 토큰은 이 기기의 Keystore에만 저장됩니다.",
            fontSize = 12.sp,
            color = BriefTheme.mutedText
        )
    }
}

@Composable
private fun FieldContainer(content: @Composable RowScopeAlias.() -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(BriefTheme.boardCell)
            .border(1.dp, BriefTheme.cardStroke, RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        content()
    }
}

private typealias RowScopeAlias = androidx.compose.foundation.layout.RowScope

@Composable
private fun LoginTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardOptions: KeyboardOptions,
    keyboardActions: KeyboardActions,
    visualTransformation: VisualTransformation,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        if (value.isEmpty()) {
            Text(
                text = placeholder,
                fontSize = 16.sp,
                fontFamily = FontFamily.Monospace,
                color = BriefTheme.mutedText
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(
                color = Color.White,
                fontSize = 16.sp,
                fontFamily = FontFamily.Monospace
            ),
            cursorBrush = SolidColor(BriefTheme.boardAmber),
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            visualTransformation = visualTransformation,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
