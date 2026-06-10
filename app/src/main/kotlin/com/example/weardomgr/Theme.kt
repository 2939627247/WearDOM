package com.example.weardomgr

import androidx.compose.runtime.Composable
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.dynamicColorScheme

/**
 * App theme for Wear OS.
 *
 * [dynamicColorScheme] automatically generates a [ColorScheme] that matches
 * the color the user has selected for their watch face, making the app feel
 * native and visually consistent with the rest of the watch UI.
 */
@Composable
fun WearAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = dynamicColorScheme(),
        content     = content,
    )
}
