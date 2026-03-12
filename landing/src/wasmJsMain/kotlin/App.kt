import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import sections.*
import theme.AppColors

@Composable
fun App() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            HeroSection()
            StatsSection()
            ChartSection()
            HowItHelpsSection()
            DownloadSection()
            SocialSection()
            FooterSection()
        }
    }
}
