package app.planlift.fit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import app.planlift.fit.data.sync.SyncConfig
import app.planlift.fit.data.sync.SyncConfigStore
import app.planlift.fit.ui.PlanLiftApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 仅 debug：允许用 adb 预填 WebDAV 配置，方便自动化测试（release 构建不存在这段）
        if (BuildConfig.DEBUG) {
            intent?.getStringExtra("sync_url")?.let { url ->
                SyncConfigStore.save(
                    this,
                    SyncConfig(
                        url = url,
                        user = intent.getStringExtra("sync_user").orEmpty(),
                        password = intent.getStringExtra("sync_pass").orEmpty(),
                        dir = intent.getStringExtra("sync_dir") ?: "PlanLift"
                    )
                )
            }
        }

        enableEdgeToEdge(
            statusBarStyle = androidx.activity.SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = androidx.activity.SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )
        // 放在 enableEdgeToEdge 之后，避免被它覆盖
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        @Suppress("DEPRECATION")
        run {
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
        }
        window.setBackgroundDrawable(
            android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT)
        )
        if (android.os.Build.VERSION.SDK_INT >= 29) {
            window.isNavigationBarContrastEnforced = false
            window.isStatusBarContrastEnforced = false
        }
        setContent { PlanLiftApp() }
    }
}
