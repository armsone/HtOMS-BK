package com.htoms.brief

import android.app.Application
import com.htoms.brief.auth.SessionController
import com.htoms.brief.security.KeystoreSecureStore
import com.htoms.brief.update.AppUpdateManager
import com.htoms.brief.widget.WidgetSnapshotStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class HtOMSBriefApplication : Application() {

    /** 인증 게이트 상태. 토큰은 Keystore 기반 저장소에만 기록된다. */
    val sessionController: SessionController by lazy {
        SessionController(KeystoreSecureStore(this))
    }

    val widgetSnapshotStore: WidgetSnapshotStore by lazy {
        WidgetSnapshotStore(this)
    }

    /** 로그아웃 시 위젯 캐시 정리처럼 화면 수명과 무관하게 끝나야 하는 작업 전용 범위. */
    val applicationScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val updateManager: AppUpdateManager by lazy {
        AppUpdateManager(this, applicationScope)
    }

    override fun onCreate() {
        super.onCreate()
        updateManager.checkForUpdates(manual = false)
    }
}
