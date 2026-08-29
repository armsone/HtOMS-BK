package com.htoms.brief

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.htoms.brief.api.RemoteBriefProvider
import com.htoms.brief.api.OMSAuthService
import com.htoms.brief.auth.SessionController
import com.htoms.brief.auth.UnconnectedAuthService
import com.htoms.brief.provider.SampleBriefProvider
import com.htoms.brief.theme.BriefTheme
import com.htoms.brief.theme.HtOMSBriefTheme
import com.htoms.brief.update.AppUpdateManager
import com.htoms.brief.ui.BriefRootScreen
import com.htoms.brief.ui.AppUpdatePanel
import com.htoms.brief.ui.BriefViewModel
import com.htoms.brief.ui.LoginScreen
import com.htoms.brief.widget.SalesBoardWidget
import kotlinx.coroutines.launch

/** 폰·태블릿·리사이즈 창·Google TV를 모두 담당하는 단일 액티비티. */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as HtOMSBriefApplication
        val catalogMode = if (BuildConfig.DEBUG) resolveCatalogMode() else null
        setContent {
            HtOMSBriefTheme {
                AppRoot(
                    session = app.sessionController,
                    catalogMode = catalogMode,
                    updateManager = app.updateManager
                )
            }
        }
    }

    /**
     * 결정적 UI 카탈로그 모드(디버그 전용).
     * adb shell am start ... --ez ui-catalog-login true / --ez ui-catalog-brief true
     * 과거 4페이지 시절 플래그(ui-catalog-page)도 통합 페이지로 이어지도록 허용한다.
     */
    private fun resolveCatalogMode(): CatalogMode? = when {
        intent?.getBooleanExtra("ui-catalog-login", false) == true -> CatalogMode.LOGIN
        intent?.getBooleanExtra("ui-catalog-brief", false) == true -> CatalogMode.BRIEF
        intent?.getBooleanExtra("ui-catalog-page", false) == true -> CatalogMode.BRIEF
        else -> null
    }
}

enum class CatalogMode { LOGIN, BRIEF }

/** 인증 게이트: 세션이 없으면 업무 데이터를 로드하지 않고 로그인 화면만 보여 준다. */
@Composable
fun AppRoot(
    session: SessionController,
    catalogMode: CatalogMode?,
    updateManager: AppUpdateManager
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BriefTheme.background)
            .safeDrawingPadding()
    ) {
        when (catalogMode) {
            CatalogMode.LOGIN -> LoginScreen(controller = session, authService = UnconnectedAuthService())
            CatalogMode.BRIEF -> {
                val catalogViewModel: BriefViewModel = viewModel(
                    key = "ui-catalog-brief",
                    factory = BriefViewModel.factory(SampleBriefProvider()) {}
                )
                BriefRootScreen(viewModel = catalogViewModel, onLogout = {})
            }
            null -> Column(modifier = Modifier.fillMaxSize()) {
                AppUpdatePanel(updateManager)
                Box(modifier = Modifier.weight(1f)) {
                    AuthenticatedContent(session)
                }
            }
        }
    }
}

@Composable
private fun AuthenticatedContent(session: SessionController) {
    val activeSession by session.session.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val app = context.applicationContext as HtOMSBriefApplication

    val current = activeSession
    if (current != null) {
        val briefViewModel: BriefViewModel = viewModel(
            key = current.token,
            factory = BriefViewModel.factory(RemoteBriefProvider(token = current.token)) { snapshot ->
                snapshot.widgetSnapshot?.let { widget ->
                    runCatching {
                        app.widgetSnapshotStore.save(widget)
                        SalesBoardWidget().updateAll(app)
                    }
                }
            }
        )
        BriefRootScreen(
            viewModel = briefViewModel,
            onLogout = {
                session.logout()
                app.applicationScope.launch {
                    runCatching {
                        app.widgetSnapshotStore.remove()
                        SalesBoardWidget().updateAll(app)
                    }
                }
            }
        )
    } else {
        LoginScreen(controller = session, authService = OMSAuthService())
    }
}
