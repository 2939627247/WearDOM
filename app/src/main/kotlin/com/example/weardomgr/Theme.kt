@file:OptIn(ExperimentalWearMaterial3Api::class)

package com.example.weardomgr

import androidx.compose.runtime.Composable
import androidx.wear.compose.material3.MaterialTheme

/**
 * Thin wrapper around [MaterialTheme] for Wear OS.
 * Swap in a custom [colorScheme] / [typography] / [shapes] here when needed.
 */
@Composable
fun WearAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(content = content)
}
