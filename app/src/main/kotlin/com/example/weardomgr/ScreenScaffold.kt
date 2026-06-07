package com.example.weardomgr

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.wear.compose.foundation.lazy.ScalingLazyListState
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.PositionIndicator
import androidx.wear.compose.material3.Scaffold
import androidx.wear.compose.material3.TimeText
import androidx.wear.compose.material3.Vignette
import androidx.wear.compose.material3.VignettePosition

/**
 * Enhanced Scaffold for Wear OS screens with built-in scroll indicators and decorations.
 *
 * Wraps Material3 Scaffold and provides:
 * - TimeText display (top)
 * - Vignette fade effects (top/bottom)
 * - PositionIndicator for scroll state (right edge)
 *
 * @param scrollState The scaling lazy list state to track scroll position
 * @param timeText Optional composable for displaying time (defaults to TimeText())
 * @param modifier Modifier for the Scaffold
 * @param content Main content area receiving PaddingValues
 */
@Composable
fun ScreenScaffold(
    scrollState: ScalingLazyListState,
    timeText: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        timeText = timeText ?: { TimeText() },
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) },
        positionIndicator = {
            PositionIndicator(
                scalingLazyListState = scrollState,
                modifier = Modifier.align(Alignment.CenterEnd),
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            content(paddingValues)
        }
    }
}
