package kz.fearsom.financiallifev2.adminui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kz.fearsom.financiallifev2.model.EndingType
import kz.fearsom.financiallifev2.scenarios.analysis.GraphNode
import kz.fearsom.financiallifev2.scenarios.analysis.ScenarioAnalysis

// ── Canvas layout constants (world units) ───────────────────────────────────────
private const val NODE_W = 168f
private const val NODE_H = 56f
private const val H_GAP  = 28f
private const val V_GAP  = 44f
private const val PAD    = 28f

/** Color coding for ending nodes, shared by the scenario viewer and story editor. */
fun endingColor(type: EndingType?): Color = when (type) {
    EndingType.WEALTH               -> Color(0xFF2E7D32)
    EndingType.FINANCIAL_FREEDOM    -> Color(0xFF388E3C)
    EndingType.FINANCIAL_STABILITY  -> Color(0xFF1565C0)
    EndingType.PAYCHECK_TO_PAYCHECK -> Color(0xFFF9A825)
    EndingType.BANKRUPTCY           -> Color(0xFFC62828)
    null                            -> Color(0xFF6A6A6A)
}

/**
 * Pannable/zoomable canvas rendering a [ScenarioAnalysis]. Extracted from the
 * read-only scenario viewer so the story editor can reuse it as a live preview.
 */
@Composable
fun ScenarioCanvas(
    analysis: ScenarioAnalysis,
    selectedEventId: String?,
    onSelect: (String?) -> Unit
) {
    var viewScale by remember(analysis) { mutableStateOf(1f) }
    var offset    by remember(analysis) { mutableStateOf(Offset(PAD, PAD)) }

    val measurer = rememberTextMeasurer()

    // Theme colors captured for use inside the (non-composable) DrawScope.
    val cs = MaterialTheme.colorScheme
    val edgeColor       = cs.outline.copy(alpha = 0.7f)
    val normalFill      = cs.surfaceVariant
    val normalText      = cs.onSurfaceVariant
    val rootFill        = cs.primaryContainer
    val rootText        = cs.onPrimaryContainer
    val selectedBorder  = cs.primary
    val unreachBorder   = cs.error
    val canvasBg        = cs.surface

    fun nodeTopLeft(n: GraphNode) = Offset(
        x = PAD + n.column * (NODE_W + H_GAP),
        y = PAD + n.rank * (NODE_H + V_GAP)
    )

    Box(Modifier.fillMaxSize().clipToBounds().background(canvasBg)) {
        if (analysis.nodes.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Empty graph.", color = normalText)
            }
        } else {
            Canvas(
                modifier = Modifier.fillMaxSize()
                    .pointerInput(analysis) {
                        detectTransformGestures { centroid, pan, zoom, _ ->
                            val newScale = (viewScale * zoom).coerceIn(0.3f, 3f)
                            offset = centroid - (centroid - offset) * (newScale / viewScale) + pan
                            viewScale = newScale
                        }
                    }
                    .pointerInput(analysis) {
                        detectTapGestures { tap ->
                            val world = (tap - offset) / viewScale
                            val hit = analysis.nodes.firstOrNull { n ->
                                val tl = nodeTopLeft(n)
                                world.x in tl.x..(tl.x + NODE_W) && world.y in tl.y..(tl.y + NODE_H)
                            }
                            onSelect(hit?.event?.id)
                        }
                    }
            ) {
                translate(left = offset.x, top = offset.y) {
                    scale(scaleX = viewScale, scaleY = viewScale, pivot = Offset.Zero) {
                        // Edges first (under nodes).
                        val posById = analysis.nodes.associate { it.event.id to nodeTopLeft(it) }
                        for (edge in analysis.edges) {
                            val from = posById[edge.fromId] ?: continue
                            val to   = posById[edge.toId] ?: continue
                            val start = Offset(from.x + NODE_W / 2f, from.y + NODE_H)
                            val end   = Offset(to.x + NODE_W / 2f, to.y)
                            drawLine(edgeColor, start, end, strokeWidth = 1.6f)
                            // Arrival chevron pointing into the target's top edge.
                            drawLine(edgeColor, end, Offset(end.x - 5f, end.y - 8f), strokeWidth = 1.6f)
                            drawLine(edgeColor, end, Offset(end.x + 5f, end.y - 8f), strokeWidth = 1.6f)
                        }

                        // Nodes.
                        for (n in analysis.nodes) {
                            val tl = nodeTopLeft(n)
                            val ending = n.event.isEnding
                            val fill = when {
                                ending -> endingColor(n.event.endingType).copy(alpha = 0.9f)
                                n.isRoot -> rootFill
                                else -> normalFill
                            }
                            val textColor = when {
                                ending -> Color.White
                                n.isRoot -> rootText
                                else -> normalText
                            }
                            drawRoundRect(
                                color = fill,
                                topLeft = tl,
                                size = Size(NODE_W, NODE_H),
                                cornerRadius = CornerRadius(10f, 10f)
                            )
                            // Border: selected > unreachable > none.
                            when {
                                n.event.id == selectedEventId -> drawRoundRect(
                                    color = selectedBorder, topLeft = tl, size = Size(NODE_W, NODE_H),
                                    cornerRadius = CornerRadius(10f, 10f),
                                    style = Stroke(width = 3f)
                                )
                                !n.reachable -> drawRoundRect(
                                    color = unreachBorder, topLeft = tl, size = Size(NODE_W, NODE_H),
                                    cornerRadius = CornerRadius(10f, 10f),
                                    style = Stroke(
                                        width = 1.5f,
                                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))
                                    )
                                )
                            }
                            val label = "${n.event.flavor} ${n.event.id}" +
                                if (ending) "\n🏁 ${n.event.endingType ?: "ending"}" else ""
                            val layout = measurer.measure(
                                text = label,
                                style = TextStyle(color = textColor, fontSize = 11.sp),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                constraints = Constraints(maxWidth = (NODE_W - 16).toInt())
                            )
                            drawText(layout, topLeft = Offset(tl.x + 8f, tl.y + 8f))
                        }
                    }
                }
            }

            // Zoom / reset overlay.
            Row(
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                SmallControl("−") { viewScale = (viewScale * 0.8f).coerceIn(0.3f, 3f) }
                SmallControl("+") { viewScale = (viewScale * 1.25f).coerceIn(0.3f, 3f) }
                SmallControl("Reset") { viewScale = 1f; offset = Offset(PAD, PAD) }
            }
        }
    }
}

@Composable
private fun SmallControl(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}
