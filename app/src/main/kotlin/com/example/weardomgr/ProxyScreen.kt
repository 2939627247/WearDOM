package com.example.weardomgr

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.rememberTransformationSpec
import androidx.wear.compose.material3.transformedHeight

@Composable
fun ProxyScreen(vm: DeviceOwnerViewModel) {
    val state     by vm.state.collectAsState()
    val listState  = rememberTransformingLazyColumnState()
    val input      = state.proxyInput
    val spec       = rememberTransformationSpec()

    ScreenScaffold(scrollState = listState) {
        TransformingLazyColumn(
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
                    hint          = "1–65535，例 8080",
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
                    hint          = "逗号分隔，例 localhost,*.local",
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
                    onClick        = { vm.applyProxy() },
                    modifier       = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, spec),
                    transformation = SurfaceTransformation(spec),
                ) { Text("应用代理") }
            }

            item {
                OutlinedButton(
                    onClick        = { vm.clearProxy() },
                    modifier       = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, spec),
                    transformation = SurfaceTransformation(spec),
                ) { Text("清除代理") }
            }

            item {
                Text(
                    text      = "DO API: setRecommendedGlobalProxy()",
                    style     = MaterialTheme.typography.labelSmall,
                    color     = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier  = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, spec)
                        .padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun ProxyInputField(
    label: String,
    value: String,
    hint: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction        = ImeAction.Next,
    modifier: Modifier          = Modifier,
) {
    val shape       = RoundedCornerShape(10.dp)
    val borderColor = MaterialTheme.colorScheme.outline
    val bgColor     = MaterialTheme.colorScheme.surfaceContainerHigh

    Column(
        modifier            = modifier,
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
