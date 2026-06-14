package kz.fearsom.financiallifev2.ui.components.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.runtime.Composable

/**
 * Entry animation for each chat message row.
 * [animate] is only true for messages appearing for the first time. Messages
 * re-entering the viewport after being scrolled off skip the animation so they
 * don't visually "replay" on scroll-back.
 */
@Composable
fun AnimatedMessageEntry(animate: Boolean = true, content: @Composable () -> Unit) {
    if (animate) {
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(tween(400)) + slideInVertically(tween(350)) { it / 3 }
        ) {
            content()
        }
    } else {
        content()
    }
}