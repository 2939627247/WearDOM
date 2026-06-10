package com.example.weardomgr

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.dynamicColorScheme

/**
 * App theme for Wear OS.
 *
 * Uses [dynamicColorScheme] to match the user's watch face color when the
 * device supports it (Wear OS 4+). Falls back to the Material3 default
 * palette on older devices where dynamic color is unavailable (returns null).
 */
@Composable
fun WearAppTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val dynamic = dynamicColorScheme(context)   // null on devices without dynamic color
    if (dynamic != null) {
        MaterialTheme(colorScheme = dynamic, content = content)
    } else {
        MaterialTheme(content = content)
    }
}
