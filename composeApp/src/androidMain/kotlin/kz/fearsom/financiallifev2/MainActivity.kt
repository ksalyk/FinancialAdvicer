package kz.fearsom.financiallifev2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import kz.fearsom.financiallifev2.di.androidModule
import kz.fearsom.financiallifev2.di.commonModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Napier once; DebugAntilog routes to Logcat with tags.
        if (BuildConfig.DEBUG) {
            Napier.base(DebugAntilog())
        }

        if (GlobalContext.getOrNull() == null) {
            startKoin {
                androidContext(application)
                modules(commonModule, androidModule)
            }
        }

        setContent {
            App()
        }
    }
}
