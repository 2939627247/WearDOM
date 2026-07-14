package com.example.weardomgr

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.OutlinedButton
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import kotlinx.coroutines.delay

@Composable
fun ProxyScreen(vm: DeviceOwnerViewModel) {
    val state     by vm.state.collectAsState()
    val listState  = rememberTransformingLazyColumnState()
    val input      = state.proxyInput
    val spec       = rememberTransformationSpec()

    // Only start showing validation errors after the user has tried to apply
    // once — avoids nagging red text while they're still mid-typing.
    var attemptedApply by remember { mutableStateOf(false) }

    // "清除代理" is the app's one instant destructive action — require a
    // second tap within ~2.5s to confirm, rather than firing immediately
    // on a single accidental tap.
    var confirmingClear by remember { mutableStateOf(false) }
    LaunchedEffect(confirmingClear) {
        if (confirmingClear) {
            delay(2500)
            confirmingClear = false
        }
    }

    val hostError = if (attemptedApply && input.host.isBlank())
        "请输入代理主机" else null
    val portError = if (attemptedApply &&
        input.port.toIntOrNull()?.let { it in 1..65535 } != true)
        "端口需为 1–65535 之间的数字" else null

    ScreenScaffold(scrollState = listState) { contentPadding ->
        TransformingLazyColumn(
            state               = listState,
            contentPadding      = contentPadding,
            modifier            = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {

            item {
                Text(
                    text      = "HTTP 代理设置",
                    style     = MaterialTheme.typography.titleSmall,
                    textAlign = TextAlign.Center,
                    modifier  = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, spec),
                )
            }

            item {
                val (text, color) = if (state.activeProxy != null)
                    "✓ 当前: ${state.activeProxy}" to MaterialTheme.colorScheme.primary
                else
                    "代理未设置" to MaterialTheme.colorScheme.onSurfaceVariant
                Text(
                    text      = text,
                    style     = MaterialTheme.typography.bodySmall,
                    color     = color,
                    textAlign = TextAlign.Center,
                    modifier  = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, spec),
                )
            }

            item {
                ProxyInputField(
                    label         = "代理主机",
                    value         = input.host,
                    hint          = "192.168.1.1 或 proxy.corp.com",
                    error         = hostError,
                    keyboardType  = KeyboardType.Uri,
                    imeAction     = ImeAction.Next,
                    onValueChange = { vm.updateProxyInput(input.copy(host = it)) },
                    modifier      = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, spec),
                )
            }

            item {
                ProxyInputField(
                    label         = "端口",
                    value         = input.port,
                    hint          = "1–65535，例如 8080",
                    error         = portError,
                    keyboardType  = KeyboardType.Number,
                    imeAction     = ImeAction.Next,
                    onValueChange = { vm.updateProxyInput(input.copy(port = it)) },
                    modifier      = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, spec),
                )
            }

            item {
                ProxyInputField(
                    label         = "排除列表（可选）",
                    value         = input.exclusions,
                    hint          = "localhost,*.local",
                    error         = null,
                    keyboardType  = KeyboardType.Text,
                    imeAction     = ImeAction.Done,
                    onValueChange = { vm.updateProxyInput(input.copy(exclusions = it)) },
                    modifier      = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, spec),
                )
            }

            item {
                Button(
                    onClick  = {
                        attemptedApply = true
                        vm.applyProxy()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, spec),
                ) { Text("应用代理") }
            }

            item {
                OutlinedButton(
                    onClick  = {
                        if (confirmingClear) {
                            attemptedApply   = false
                            confirmingClear  = false
                            vm.clearProxy()
                        } else {
                            confirmingClear = true
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, spec),
                ) { Text(if (confirmingClear) "再次点击确认清除" else "清除代理") }
            }
        }
    }
}

// ── ProxyInputField ───────────────────────────────────────────────────────────
//
// Wear-native input design:
//   • No border — a subtle filled background reads better on a small round
//     display than a phone-style outlined box
//   • MaterialTheme.shapes.small keeps the corner radius consistent with
//     the rest of Wear M3
//   • decorationBox replaces the old Box+Text placeholder overlay hack
//   • Label uses primary color so each field is clearly anchored while
//     scanning down the list
//   • error: when non-null, background/label tint to the error color and
//     a short message appears below — previously an invalid port would
//     silently no-op with zero feedback, which read as "broken"

@Composable
private fun ProxyInputField(
    label: String,
    value: String,
    hint: String,
    error: String?,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction       = ImeAction.Next,
    modifier: Modifier         = Modifier,
) {
    val hasError    = error != null
    val labelColor  = if (hasError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    val fieldBg     = if (hasError) MaterialTheme.colorScheme.errorContainer
                       else MaterialTheme.colorScheme.surfaceContainer

    Column(
        modifier            = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text  = label,
            style = MaterialTheme.typography.labelSmall,
            color = labelColor,
        )

        BasicTextField(
            value           = value,
            onValueChange   = onValueChange,
            singleLine      = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                imeAction    = imeAction,
            ),
            textStyle   = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onSurface,
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            modifier    = Modifier.fillMaxWidth(),
            decorationBox = { innerTextField ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.small)
                        .background(fieldBg)
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                ) {
                    if (value.isEmpty()) {
                        Text(
                            text  = hint,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        )
                    }
                    innerTextField()
                }
            },
        )

        if (error != null) {
            Text(
                text  = error,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}
