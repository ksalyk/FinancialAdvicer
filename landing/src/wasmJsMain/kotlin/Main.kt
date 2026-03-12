import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import config.WebConfig
import kotlinx.browser.document

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    try {
        val canvasElement = document.getElementById(WebConfig.CANVAS_ELEMENT_ID) ?: run {
            // Element not found - app won't render
            return
        }

        ComposeViewport(canvasElement) {
            App()
        }
    } catch (e: Exception) {
        // Error during initialization - log would go to browser console via JS
        // In production, you can add JS interop for console.error(e.message)
    }
}
