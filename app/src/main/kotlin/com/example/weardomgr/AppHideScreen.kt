@file:OptIn(ExperimentalWearMaterial3Api::class)

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
 * DO APIs used:
 *   • [DevicePolicyManager.setApplicationHidden]  — hide or show an app
 *   • [DevicePolicyManager.isApplicationHidden]   — query current visibility
 *
 * A hidden app disappears from the launcher and cannot be launched by the user.
 * Its data is untouched. setApplicationHidden(…, false) restores it instantly.
 */
@Composable
fun AppHideScreen(vm: DeviceOwnerViewModel) {
    val state     by vm.state.collectAsState()
    val listState  = rememberScalingLazyListState()

    LaunchedEffect(Unit) { vm.loadApps() }

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
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            ScalingLazyColumn(
                state               = listState,
                modifier            = Modifier.fillMaxSize(),
                contentPadding      = PaddingValues(
                    top = 40.dp, bottom = 32.dp, start = 10.dp, end = 10.dp,
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
                        style = MaterialTheme.typography.labelSmall,
                        color = if (hidden > 0) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurfaceVariant,
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

                // ── Inline message ──
                if (state.message != null) {
                    item {
                        Text(
                            text      = state.message!!,
                            style     = MaterialTheme.typography.labelSmall,
                            color     = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center,
                            modifier  = Modifier.fillMaxWidth(),
                        )
                    }
                }

                // ── User apps section ──
                val userApps   = displayedApps.filter { !it.isSystemApp }
                val systemApps = displayedApps.filter {  it.isSystemApp }

                if (userApps.isNotEmpty()) {
                    item {
                        ListHeader { Text("用户应用 (${userApps.size})") }
                    }
                    items(userApps, key = { it.packageName }) { app ->
                        AppHideRow(app = app, onToggle = { vm.toggleHidden(app.packageName) })
                    }
                }

                // ── System apps section ──
                if (systemApps.isNotEmpty()) {
                    item {
                        ListHeader { Text("系统应用 (${systemApps.size})") }
                    }
                    items(systemApps, key = { it.packageName }) { app ->
                        AppHideRow(app = app, onToggle = { vm.toggleHidden(app.packageName) })
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

                item {
                    Text(
                        text      = "DO API: setApplicationHidden()\nisApplicationHidden()",
                        style     = MaterialTheme.typography.labelSmall,
                        color     = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier  = Modifier.fillMaxWidth().padding(top = 6.dp),
                    )
                }
            }
        }
    }
}

// ─────────────────────── AppHideRow ─────────────────────────────────────────

/**
 * One row per app — uses [SplitToggleButton] (the canonical Wear OS M3 component
 * for a list item with a separate toggle area).
 *
 * Left  (onClick)           → also toggles (same action for ease of use)
 * Right (onCheckedChange)   → [Switch] toggle
 */
@Composable
private fun AppHideRow(
    app: AppItem,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SplitToggleButton(
        checked         = app.isHidden,
        onCheckedChange = { onToggle() },
        onClick         = { onToggle() },
        toggleControl   = { Switch() },
        modifier        = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier            = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text     = app.label,
                style    = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text  = when {
                    app.isHidden    -> "已隐藏"
                    app.isSystemApp -> "系统 · 可见"
                    else            -> "可见"
                },
                style = MaterialTheme.typography.labelSmall,
                color = when {
                    app.isHidden    -> MaterialTheme.colorScheme.error
                    app.isSystemApp -> MaterialTheme.colorScheme.onSurfaceVariant
                    else            -> MaterialTheme.colorScheme.primary
                },
            )
        }
    }
}

// ─────────────────────── Search field ───────────────────────────────────────

@Composable
private fun AppSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape       = RoundedCornerShape(20.dp)
    val borderColor = MaterialTheme.colorScheme.outline
    val bgColor     = MaterialTheme.colorScheme.surface

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(color = bgColor, shape = shape)
            .border(width = 1.dp, color = borderColor, shape = shape)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        if (value.isEmpty()) {
            Text(
                text  = "搜索应用…",
                style = MaterialTheme.typography.bodySmall,
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
            textStyle   = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onSurface,
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            modifier    = Modifier.fillMaxWidth(),
        )
    }
}
