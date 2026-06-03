package kz.fearsom.financiallifev2.adminui

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document
import kz.fearsom.financiallifev2.adminui.net.AdminApiClient

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val target = document.getElementById("ComposeTarget") ?: return
    val api    = AdminApiClient()

    ComposeViewport(target) {
        AdminApp(api)
    }
}
