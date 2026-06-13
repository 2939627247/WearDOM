package com.example.weardomgr

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun AppHideScreen(vm: DeviceOwnerViewModel) {
    val state     by vm.state.collectAsState()
    val listState  = rememberTransformingLazyColumnState()

    LaunchedEffect(Unit) { vm.loadApps() }

    val displayedApps = remember(state.apps, state.appsFilter) {
        if (state.appsFilter.isBlank()) state.apps
        else state.apps.filter {
            it.label.contains(state.appsFilter, ignoreCase = true) ||
            it.packageName.contains(state.appsFilter, ignoreCase = true)
        }
    }

    // FIX: remember called unconditionally, outside if/else — Compose
    // requires composable functions to be called on every recomposition
    // regardless of conditions to keep the slot table consistent.
    val userApps   = remember(displayedApps) { displayedApps.filter { !it.isSystemApp } }
    val systemApps = remember(displayedApps) { displayedApps.filter {  it.isSystemApp } }

    // FIX: memoize counts that are referenced in item bodies
    val totalCount  = remember(state.apps) { state.apps.size }
    val hiddenCount = remember(state.apps) { state.apps.count { it.isHidden } }

    ScreenScaffold(scrollState = listState) { contentPadding ->
        if (state.isLoadingApps) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            TransformingLazyColumn(
                state               = listState,
                contentPadding      = contentPadding,
                modifier            = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {

                item {
                    Text(
                        text      = "应用隐藏管理",
                        style     = MaterialTheme.typography.titleSmall,
                        textAlign = TextAlign.Center,
                        modifier  = Modifier.fillMaxWidth(),
                    )
                }

                item {
                    Text(
                        text  = "共 $totalCount 个 · 已隐藏 $hiddenCount 个",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (hiddenCount > 0) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier  = Modifier.fillMaxWidth(),
                    )
                }

                item {
                    AppSearchField(
                        value         = state.appsFilter,
                        onValueChange = { vm.updateFilter(it) },
                        modifier      = Modifier.fillMaxWidth(),
                    )
                }

                if (userApps.isNotEmpty()) {
                    item {
                        ListHeader(modifier = Modifier.fillMaxWidth()) {
                            Text("用户应用 (${userApps.size})")
                        }
                    }
                    items(userApps, key = { it.packageName }) { app ->
                        AppHideRow(
                            app      = app,
                            onToggle = { vm.toggleHidden(app.packageName) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                if (systemApps.isNotEmpty()) {
                    item {
                        ListHeader(modifier = Modifier.fillMaxWidth()) {
                            Text("系统应用 (${systemApps.size})")
                        }
                    }
                    items(systemApps, key = { it.packageName }) { app ->
                        AppHideRow(
                            app      = app,
                            onToggle = { vm.toggleHidden(app.packageName) },
                            modifier = Modifier.fillMaxWidth(),
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

                item {
                    Text(
                        text      = "DO API: setApplicationHidden()\nisApplicationHidden()",
                        style     = MaterialTheme.typography.labelSmall,
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

// ─────────────────────────── AppHideRow ──────────────────────────────────────

@Composable
private fun AppHideRow(
    app: AppItem,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick  = onToggle,
        modifier = modifier,
        colors   = if (app.isHidden)
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor   = MaterialTheme.colorScheme.onErrorContainer,
            )
        else
            ButtonDefaults.filledTonalButtonColors(),
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AppIcon(packageName = app.packageName, size = 32.dp)

            Column(modifier = Modifier.weight(1f)) {
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

            Text(
                text  = if (app.isHidden) "●" else "○",
                style = MaterialTheme.typography.bodyMedium,
                color = if (app.isHidden)
                    MaterialTheme.colorScheme.error
                else
                    MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ─────────────────────────── AppIcon ─────────────────────────────────────────

@Composable
private fun AppIcon(packageName: String, size: Dp) {
    val context = LocalContext.current
    // FIX: derive pixel size from actual screen density instead of
    // hardcoding size * 3, which over/under-samples on non-3x screens.
    val sizePx = with(LocalDensity.current) { size.roundToPx() }

    val icon by produceState<Bitmap?>(null, packageName) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                context.packageManager
                    .getApplicationIcon(packageName)
                    .toBitmap(width = sizePx, height = sizePx)
            }.getOrNull()
        }
    }

    Box(
        modifier         = Modifier.size(size).clip(CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (icon != null) {
            Image(
                bitmap             = icon!!.asImageBitmap(),
                contentDescription = null,
                modifier           = Modifier.fillMaxSize(),
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            )
        }
    }
}

// ─────────────────────────── AppSearchField ──────────────────────────────────

@Composable
private fun AppSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape       = RoundedCornerShape(20.dp)
    val borderColor = MaterialTheme.colorScheme.outline
    val bgColor     = MaterialTheme.colorScheme.surfaceContainerHigh

    Box(
        modifier = modifier
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
