package com.example.weardomgr

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.*

/**
 * App Hide/Show management screen.
 *
 * Uses two Device Owner-only APIs:
 *   • [DevicePolicyManager.setApplicationHidden]  — hides or shows an app
 *   • [DevicePolicyManager.isApplicationHidden]   — queries current visibility
 *
 * A hidden app disappears from the launcher and cannot be launched by the
 * user. Its data is untouched. Calling setApplicationHidden(…, false) makes
 * it visible again instantly.
 */
@Composable
fun AppHideScreen(vm: DeviceOwnerViewModel) {
    val state     by vm.state.collectAsState()
    val listState  = rememberScalingLazyListState()

    // Load the app list once when the screen opens
    LaunchedEffect(Unit) { vm.loadApps() }

    // Apply the search filter in the UI layer (no extra suspend needed)
    val displayedApps = remember(state.apps, state.appsFilter) {
        if (state.appsFilter.isBlank()) state.apps
        else state.apps.filter {
            it.label.contains(state.appsFilter, ignoreCase = true) ||
            it.packageName.contains(state.appsFilter, ignoreCase = true)
        }
    }

    Scaffold(
        timeText          = { TimeText() },
        vignette          = { Vignette(vignettePosition = VignettePosition.TopAndBottom) },
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) },
    ) {
        if (state.isLoadingApps) {
            // ── Loading spinner ──
            Box(
                modifier        = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else {
            ScalingLazyColumn(
                state               = listState,
                modifier            = Modifier.fillMaxSize(),
                contentPadding      = PaddingValues(
                    top    = 40.dp,
                    bottom = 32.dp,
                    start  = 10.dp,
                    end    = 10.dp,
                ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {

                // ── Title + stats ──
                item {
                    Text(
                        text      = "应用隐藏管理",
                        style     = MaterialTheme.typography.titleSmall,
                        textAlign = TextAlign.Center,
                        modifier  = Modifier.fillMaxWidth(),
                    )
                }

                item {
                    val total  = state.apps.size
                    val hidden = state.apps.count { it.isHidden }
                    Text(
                        text  = "共 $total 个 · 已隐藏 $hidden 个",
                        style = MaterialTheme.typography.captionSmall,
                        color = if (hidden > 0)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }

                // ── Search filter ──
                item {
                    AppSearchField(
                        value         = state.appsFilter,
                        onValueChange = { vm.updateFilter(it) },
                    )
                }

                // ── Inline message / toast ──
                if (state.message != null) {
                    item {
                        Text(
                            text      = state.message!!,
                            style     = MaterialTheme.typography.captionSmall,
                            color     = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center,
                            modifier  = Modifier.fillMaxWidth(),
                        )
                    }
                }

                // ── Section header: User apps ──
                val userApps   = displayedApps.filter { !it.isSystemApp }
                val systemApps = displayedApps.filter {  it.isSystemApp }

                if (userApps.isNotEmpty()) {
                    item {
                        ListHeader {
                            Text(
                                text  = "用户应用 (${userApps.size})",
                                style = MaterialTheme.typography.captionSmall,
                            )
                        }
                    }
                    items(userApps, key = { it.packageName }) { app ->
                        AppHideRow(
                            app      = app,
                            onToggle = { vm.toggleHidden(app.packageName) },
                        )
                    }
                }

                // ── Section header: System apps ──
                if (systemApps.isNotEmpty()) {
                    item {
                        ListHeader {
                            Text(
                                text  = "系统应用 (${systemApps.size})",
                                style = MaterialTheme.typography.captionSmall,
                            )
                        }
                    }
                    items(systemApps, key = { it.packageName }) { app ->
                        AppHideRow(
                            app      = app,
                            onToggle = { vm.toggleHidden(app.packageName) },
                        )
                    }
                }

                if (displayedApps.isEmpty()) {
                    item {
                        Text(
                            text      = "无匹配应用",
                            style     = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            modifier  = Modifier.fillMaxWidth(),
                        )
                    }
                }

                // ── DO API note ──
                item {
                    Text(
                        text = "使用 DO API:\nsetApplicationHidden()\nisApplicationHidden()",
                        style     = MaterialTheme.typography.captionMicro,
                        color     = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier  = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                    )
                }
            }
        }
    }
}

// ─────────────────────────── Sub-composables ─────────────────────────────────

/**
 * One row in the app list.
 *
 * Uses [SplitToggleButton] — the canonical Wear OS Material3 component for
 * a list item that has both a tappable label area and a dedicated toggle control.
 *
 *   Left side  (onClick)         → reserved for future "app info" action
 *   Right side (onCheckedChange) → calls [onToggle] to hide/show the app
 */
@Composable
private fun AppHideRow(
    app: AppItem,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SplitToggleButton(
        checked           = app.isHidden,
        onCheckedChange   = { onToggle() },
        onClick           = { onToggle() },    // tapping the label also toggles
        toggleControl     = { Switch() },       // Switch inherits checked/enabled from scope
        modifier          = modifier.fillMaxWidth(),
        colors            = SplitToggleButtonDefaults.splitToggleButtonColors(
            checkedContainerColor        = MaterialTheme.colorScheme.errorContainer,
            checkedContentColor          = MaterialTheme.colorScheme.onErrorContainer,
            checkedSecondaryContentColor = MaterialTheme.colorScheme.onErrorContainer,
            checkedSplitContainerColor   = MaterialTheme.colorScheme.error.copy(alpha = 0.25f),
        ),
    ) {
        Column(
            modifier            = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text     = app.label,
                style    = MaterialTheme.typography.captionDefault,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text  = when {
                    app.isHidden   -> "已隐藏"
                    app.isSystemApp -> "系统 · 可见"
                    else           -> "可见"
                },
                style = MaterialTheme.typography.captionMicro,
                color = when {
                    app.isHidden    -> MaterialTheme.colorScheme.error
                    app.isSystemApp -> MaterialTheme.colorScheme.onSurfaceVariant
                    else            -> MaterialTheme.colorScheme.primary
                },
            )
        }
    }
}

/** Compact search / filter field for the app list. */
@Composable
private fun AppSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape       = RoundedCornerShape(20.dp)      // pill shape
    val borderColor = MaterialTheme.colorScheme.outline
    val surfaceColor= MaterialTheme.colorScheme.surface

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(color = surfaceColor, shape = shape)
            .border(width = 1.dp, color = borderColor, shape = shape)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        if (value.isEmpty()) {
            Text(
                text  = "🔍  搜索应用…",
                style = MaterialTheme.typography.captionSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            )
        }
        BasicTextField(
            value           = value,
            onValueChange   = onValueChange,
            singleLine      = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction    = ImeAction.Search,
            ),
            textStyle = MaterialTheme.typography.captionDefault.copy(
                color = MaterialTheme.colorScheme.onSurface,
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            modifier    = Modifier.fillMaxWidth(),
        )
    }
}
