package com.example.weardomgr

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight

@Composable
fun ProxyScreen(vm: DeviceOwnerViewModel) {
    val state     by vm.state.collectAsState()
    val listState  = rememberTransformingLazyColumnState()
    val input      = state.proxyInput
    val spec       = rememberTransformationSpec()

    ScreenScaffold(scrollState = listState) { contentPadding ->
        TransformingLazyColumn(
            state = listState, contentPadding = contentPadding,
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            item {
                Text("HTTP 代理设置", style = MaterialTheme.typography.titleSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, spec))
            }
            item {
                val (text, color) = if (state.activeProxy != null)
                    "✓ 当前: ${state.activeProxy}" to MaterialTheme.colorScheme.primary
                else "代理未设置" to MaterialTheme.colorScheme.onSurfaceVariant
                Text(text, style = MaterialTheme.typography.bodySmall, color = color,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, spec))
            }
            item {
                ProxyInputField("代理主机", input.host, "192.168.1.1 或 proxy.corp.com",
                    KeyboardType.Uri, ImeAction.Next, { vm.updateProxyInput(input.copy(host = it)) },
                    Modifier.fillMaxWidth().transformedHeight(this, spec))
            }
            item {
                ProxyInputField("端口", input.port, "1–65535，例 8080",
                    KeyboardType.Number, ImeAction.Next, { vm.updateProxyInput(input.copy(port = it)) },
                    Modifier.fillMaxWidth().transformedHeight(this, spec))
            }
            item {
                ProxyInputField("排除列表（可选）", input.exclusions, "逗号分隔，例 localhost,*.local",
                    KeyboardType.Text, ImeAction.Done, { vm.updateProxyInput(input.copy(exclusions = it)) },
                    Modifier.fillMaxWidth().transformedHeight(this, spec))
            }
            item {
                Button(
                    onClick = { vm.applyProxy() }, transformation = SurfaceTransformation(spec),
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, spec),
                ) { Text("应用代理") }
            }
            item {
                OutlinedButton(
                    onClick = { vm.clearProxy() }, transformation = SurfaceTransformation(spec),
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, spec),
                ) { Text("清除代理") }
            }
        }
    }
}

@Composable
private fun ProxyInputField(
    label: String, value: String, hint: String,
    keyboardType: KeyboardType, imeAction: ImeAction,
    onValueChange: (String) -> Unit, modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Box(
            modifier = Modifier.fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(10.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                .padding(horizontal = 10.dp, vertical = 7.dp),
        ) {
            if (value.isEmpty()) Text(hint, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f))
            BasicTextField(value = value, onValueChange = onValueChange, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
                textStyle = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth())
        }
    }
}
