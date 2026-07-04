package kz.fearsom.financiallifev2.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Line icon set for the 2026-07 redesign (lucide-style: 24×24 viewport,
 * 2px stroke, round caps/joins). Emoji glyphs in chrome are replaced by these.
 *
 * All paths are authored in white; `Icon(tint = …)` recolors them at use sites.
 */
object LineIcons {

    /** Diary / notebook — gameplay top-bar leading icon. */
    val Notebook: ImageVector by lazy {
        lineIcon("LineIcons.Notebook") {
            strokePath {
                moveTo(5f, 4f)
                arcToRelative(1f, 1f, 0f, false, true, 1f, -1f)
                horizontalLineToRelative(13f)
                verticalLineToRelative(18f)
                horizontalLineTo(6f)
                arcToRelative(1f, 1f, 0f, false, true, -1f, -1f)
                close()
            }
            strokePath {
                moveTo(9f, 3f)
                verticalLineToRelative(18f)
            }
        }
    }

    /** Bar chart — opens financial health metrics. */
    val Bars: ImageVector by lazy {
        lineIcon("LineIcons.Bars") {
            strokePath {
                moveTo(5f, 19f)
                verticalLineTo(11f)
            }
            strokePath {
                moveTo(12f, 19f)
                verticalLineTo(5f)
            }
            strokePath {
                moveTo(19f, 19f)
                verticalLineToRelative(-6f)
            }
        }
    }

    /** Pencil — "Твой ход" action panel label. */
    val Pencil: ImageVector by lazy {
        lineIcon("LineIcons.Pencil") {
            strokePath {
                moveTo(12f, 20f)
                horizontalLineToRelative(9f)
            }
            strokePath {
                moveTo(16.5f, 3.5f)
                arcToRelative(2.1f, 2.1f, 0f, false, true, 3f, 3f)
                lineTo(7f, 19f)
                lineToRelative(-4f, 1f)
                lineToRelative(1f, -4f)
                close()
            }
        }
    }

    /** Microphone — Asan voice composer. */
    val Mic: ImageVector by lazy {
        lineIcon("LineIcons.Mic") {
            strokePath {
                moveTo(12f, 15f)
                arcToRelative(3f, 3f, 0f, false, false, 3f, -3f)
                verticalLineTo(6f)
                arcToRelative(3f, 3f, 0f, false, false, -6f, 0f)
                verticalLineToRelative(6f)
                arcToRelative(3f, 3f, 0f, false, false, 3f, 3f)
                close()
            }
            strokePath {
                moveTo(19f, 11f)
                arcToRelative(7f, 7f, 0f, false, true, -14f, 0f)
            }
            strokePath {
                moveTo(12f, 19f)
                verticalLineTo(22f)
            }
        }
    }

    /** Close (X). */
    val Close: ImageVector by lazy {
        lineIcon("LineIcons.Close") {
            strokePath(strokeWidth = 2.1f) {
                moveTo(6f, 6f)
                lineTo(18f, 18f)
            }
            strokePath(strokeWidth = 2.1f) {
                moveTo(18f, 6f)
                lineTo(6f, 18f)
            }
        }
    }

    /** Back chevron. */
    val ChevronLeft: ImageVector by lazy {
        lineIcon("LineIcons.ChevronLeft") {
            strokePath {
                moveTo(15f, 5f)
                lineToRelative(-7f, 7f)
                lineToRelative(7f, 7f)
            }
        }
    }

    /** Red-flag marker for scam breakdown chips. */
    val Flag: ImageVector by lazy {
        lineIcon("LineIcons.Flag") {
            strokePath {
                moveTo(5f, 21f)
                verticalLineTo(4f)
                horizontalLineToRelative(12f)
                lineToRelative(-2f, 4f)
                lineToRelative(2f, 4f)
                horizontalLineTo(5f)
            }
        }
    }

    /** Four-point sparkle (filled) — Asan avatar / entry point. */
    val Sparkle: ImageVector by lazy {
        lineIcon("LineIcons.Sparkle") {
            fillPath {
                moveTo(12f, 2f)
                lineToRelative(1.6f, 6.4f)
                lineTo(20f, 10f)
                lineToRelative(-6.4f, 1.6f)
                lineTo(12f, 18f)
                lineToRelative(-1.6f, -6.4f)
                lineTo(4f, 10f)
                lineToRelative(6.4f, -1.6f)
                close()
            }
        }
    }

    /** Paper-plane send. */
    val Send: ImageVector by lazy {
        lineIcon("LineIcons.Send") {
            strokePath {
                moveTo(22f, 2f)
                lineTo(11f, 13f)
            }
            strokePath {
                moveTo(22f, 2f)
                lineToRelative(-7f, 20f)
                lineToRelative(-4f, -9f)
                lineToRelative(-9f, -4f)
                close()
            }
        }
    }
}

// ─── Builders ─────────────────────────────────────────────────────────────────

private inline fun lineIcon(
    name: String,
    block: ImageVector.Builder.() -> Unit
): ImageVector = ImageVector.Builder(
    name = name,
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply(block).build()

private fun ImageVector.Builder.strokePath(
    strokeWidth: Float = 2f,
    pathBuilder: PathBuilder.() -> Unit
) {
    path(
        fill = null,
        stroke = SolidColor(Color.White),
        strokeLineWidth = strokeWidth,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
        pathBuilder = pathBuilder
    )
}

private fun ImageVector.Builder.fillPath(
    pathBuilder: PathBuilder.() -> Unit
) {
    path(
        fill = SolidColor(Color.White),
        pathBuilder = pathBuilder
    )
}
