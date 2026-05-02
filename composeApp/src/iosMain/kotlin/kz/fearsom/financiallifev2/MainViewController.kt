package kz.fearsom.financiallifev2

import androidx.compose.ui.window.ComposeUIViewController
import kz.fearsom.financiallifev2.di.commonModule
import kz.fearsom.financiallifev2.di.iosModule
import kz.fearsom.financiallifev2.network.NetworkConfig
import org.koin.core.context.startKoin

/**
 * iOS entry point. Called from Swift like:
 *
 *   ContentView.swift:
 *   ```swift
 *   import shared
 *
 *   struct ContentView: View {
 *       var body: some View {
 *           ComposeView()
 *               .ignoresSafeArea(.keyboard) // optional
 *       }
 *   }
 *
 *   struct ComposeView: UIViewControllerRepresentable {
 *       func makeUIViewController(context: Context) -> UIViewController {
 *           MainViewControllerKt.MainViewController()
 *       }
 *       func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
 *   }
 *   ```
 */
fun MainViewController() = ComposeUIViewController(
    configure = {
        // iOS simulator reaches host machine directly via localhost,
        // unlike Android emulator which requires the 10.0.2.2 alias.
        NetworkConfig.baseUrl = "http://localhost:8082/api/v1"
        // Init Koin once per app launch
        startKoin { modules(commonModule, iosModule) }
    }
) {
    App()
}
