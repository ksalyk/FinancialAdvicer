/**
 * File location: src/androidMain/kotlin/com/example/network/ui/InspektorUI.kt
 */

package com.example.network.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.shreyashkore.inspektor.ui.InspektorUI as InspektorUILib

/**
 * Composable button to open Inspektor debugging UI (Android only).
 * Call this from your app's debug menu, settings screen, or development panel.
 *
 * Example usage:
 * ```
 * InspektorDebugButton(context = this)
 * ```
 *
 * This button opens the Inspektor in-app UI which shows:
 * - All captured HTTP requests/responses
 * - Request headers, body, and response details
 * - Request/response timeline
 * - HAR format export
 * - Mock response injection for testing
 */
@Composable
fun InspektorDebugButton(context: Context) {
    Button(
        onClick = {
            // Launch Inspektor UI - shows all captured network requests
            InspektorUILib.show(context)
        },
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
            .height(48.dp)
    ) {
        Icon(
            imageVector = Icons.Default.BugReport,
            contentDescription = "Network Inspector",
            tint = Color.White,
            modifier = Modifier
                .size(20.dp)
                .padding(end = 8.dp)
        )
        Text(
            text = "Open Network Inspector",
            fontSize = 14.sp
        )
    }
}

/**
 * Alternative: Floating Action Button style for quick access to Inspektor.
 * Can be placed in a corner of your screen for always-accessible debugging.
 *
 * Example usage:
 * ```
 * Box(
 *     modifier = Modifier.fillMaxSize()
 * ) {
 *     // Your main content here
 *
 *     InspektorFloatingButton(
 *         context = this@MainActivity,
 *         modifier = Modifier
 *             .align(Alignment.BottomEnd)
 *             .padding(16.dp)
 *     )
 * }
 * ```
 */
@Composable
fun InspektorFloatingButton(
    context: Context,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(60.dp)
            .background(
                color = Color(0xFF2196F3),
                shape = androidx.compose.foundation.shape.CircleShape
            )
            .clickable {
                InspektorUILib.show(context)
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.BugReport,
            contentDescription = "Network Inspector",
            tint = Color.White,
            modifier = Modifier.size(28.dp)
        )
    }
}

/**
 * Alternative: Simple Text Button style
 */
@Composable
fun InspektorSimpleButton(context: Context) {
    Text(
        text = "🔍 Inspect Network",
        modifier = Modifier
            .padding(16.dp)
            .clickable {
                InspektorUILib.show(context)
            }
            .background(Color.LightGray, shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
            .padding(12.dp),
        fontSize = 12.sp
    )
}

/**
 * Debug Panel Component - Show multiple debugging options including Inspektor
 * Useful for grouping all debugging tools in one place.
 */
@Composable
fun DebugPanel(
    context: Context,
    onClearLogs: () -> Unit = {},
    onExportLogs: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF5F5F5))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Debug Tools",
            fontSize = 16.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Button(
            onClick = { InspektorUILib.show(context) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("View Network Logs")
        }

        Button(
            onClick = onClearLogs,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Clear Logs")
        }

        Button(
            onClick = onExportLogs,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Export Logs (HAR)")
        }
    }
}
