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
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.*

/**
 * HTTP Proxy configuration screen.
 *
 * DO API: [DevicePolicyManager.setRecommendedGlobalProxy] —
 * installs a system-wide HTTP proxy recommendation enforced across all networks.
 * Pass `null` to clear.
 */
@Composable
fun ProxyScreen(vm: DeviceOwnerViewModel) {
    val state     by vm.state.collectAsState()
    val listState  = rememberScalingLazyListState()
    val input      = state.proxyInput

    Scaffold(
        timeText          = { TimeText() },
        vignette          = { Vignette(vignettePosition = VignettePosition.TopAndBottom) },
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) },
    ) {
        ScalingLazyColumn(
            state               = listState,
            modifier            = Modifier.fillMaxSize(),
            contentPadding      = PaddingValues(
                top = 40.dp, bottom = 32.dp, start = 14.dp, end = 14.dp,
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {

            item {
                Text(
                    text      = "HTTP 代理设置",
                    style     = MaterialTheme.typography.titleSmall,
                    textAlign = TextAlign.Center,
                    modifier  = Modifier.fillMaxWidth(),
                )
            }

            // ── Active proxy status ──
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
                    modifier  = Modifier.fillMaxWidth(),
                )
            }

            item {
                ProxyInputField(
                    label         = "代理主机",
                    value         = input.host,
                    hint          = "192.168.1.1 或 proxy.corp.com",
                    keyboardType  = KeyboardType.Uri,
                    imeAction     = ImeAction.Next,
                    onValueChange = { vm.updateProxyInput(input.copy(host = it)) },
                )
            }

            item {
                ProxyInputField(
                    label         = "端口",
                    value         = input.port,
                    hint          = "1–65535，例 8080",
                    keyboardType  = KeyboardType.Number,
                    imeAction     = ImeAction.Next,
                    onValueChange = { vm.updateProxyInput(input.copy(port = it)) },
                )
            }

            item {
                ProxyInputField(
                    label         = "排除列表（可选）",
                    value         = input.exclusions,
                    hint          = "逗号分隔，例 localhost,*.local",
                    keyboardType  = KeyboardType.Text,
                    imeAction     = ImeAction.Done,
                    onValueChange = { vm.updateProxyInput(input.copy(exclusions = it)) },
                )
            }

            item {
                Button(
                    onClick  = { vm.applyProxy() },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("应用代理") }
            }

            item {
                OutlinedButton(
                    onClick  = { vm.clearProxy() },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("清除代理") }
            }

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

            item {
                Text(
                    text      = "DO API: setRecommendedGlobalProxy()",
                    style     = MaterialTheme.typography.labelSmall,
                    color     = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier  = Modifier.fillMaxWidth().padding(top = 4.dp),
                )
            }
        }
    }
}

// ─────────────────────── Input field helper ──────────────────────────────────

@Composable
private fun ProxyInputField(
    label: String,
    value: String,
    hint: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction       = ImeAction.Next,
    modifier: Modifier         = Modifier,
) {
    val shape       = RoundedCornerShape(10.dp)
    val borderColor = MaterialTheme.colorScheme.outline
    val bgColor     = MaterialTheme.colorScheme.surface

    Column(
        modifier            = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text  = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(color = bgColor, shape = shape)
                .border(width = 1.dp, color = borderColor, shape = shape)
                .padding(horizontal = 10.dp, vertical = 7.dp),
        ) {
            if (value.isEmpty()) {
                Text(
                    text  = hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                )
            }
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
            )
        }
    }
}
