package kz.fearsom.financiallifev2.ui.theme

import androidx.compose.runtime.Composable
import platform.UIKit.UITraitCollection
import platform.UIKit.UIUserInterfaceStyle
import platform.UIKit.currentTraitCollection

@Composable
actual fun isSystemInDarkTheme(): Boolean =
    UITraitCollection.currentTraitCollection.userInterfaceStyle ==
        UIUserInterfaceStyle.UIUserInterfaceStyleDark
