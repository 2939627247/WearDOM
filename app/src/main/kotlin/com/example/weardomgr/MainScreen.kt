package com.example.weardomgr

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text

@Composable
fun MainScreen(
    vm: DeviceOwnerViewModel,
    onProxy: () -> Unit,
    onAppHide: () -> Unit,
) {
    val state     by vm.state.collectAsState()
    val listState  = rememberScalingLazyListState()

    LaunchedEffect(Unit) { vm.refreshOwnerStatus() }

    // ScreenScaffold: handles TimeText + ScrollIndicator automatically
    ScreenScaffold(scrollState = listState) {
        ScalingLazyColumn(
            state               = listState,
            modifier            = Modifier.fillMaxWidth(),
            contentPadding      = PaddingValues(
                top = 40.dp, bottom = 32.dp, start = 12.dp, end = 12.dp,
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {

            item {
                Text(
                    text      = "WearDOM",
                    style     = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier  = Modifier.fillMaxWidth(),
                )
            }

            item { DoStatusBadge(isOwner = state.isDeviceOwner) }

            if (state.isDeviceOwner) {
                item {
                    FilledTonalButton(
                        onClick  = onProxy,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("HTTP 代理", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text  = state.activeProxy?.toString() ?: "未设置",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (state.activeProxy != null)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                item {
                    FilledTonalButton(
                        onClick  = onAppHide,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("应用隐藏", style = MaterialTheme.typography.bodyMedium)
                            val hiddenCount = state.apps.count { it.isHidden }
                            Text(
                                text  = if (hiddenCount > 0) "已隐藏 $hiddenCount 个" else "暂无隐藏",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (hiddenCount > 0)
                                    MaterialTheme.colorScheme.error
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            } else {
                item {
                    Text(
                        text      = "需要 Device Owner 权限",
                        style     = MaterialTheme.typography.bodyMedium,
                        color     = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier  = Modifier.fillMaxWidth(),
                    )
                }
                item {
                    Text(
                        text      = "通过 ADB 激活:",
                        style     = MaterialTheme.typography.labelSmall,
                        color     = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
                item {
                    Text(
                        text = "adb shell dpm set-device-owner\ncom.example.weardomgr/\n.WearDeviceAdminReceiver",
                        style     = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                        modifier  = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun DoStatusBadge(isOwner: Boolean) {
    Button(
        onClick  = {},
        enabled  = false,
        modifier = Modifier.fillMaxWidth(),
        colors   = ButtonDefaults.buttonColors(
            disabledContainerColor = if (isOwner)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.errorContainer,
            disabledContentColor = if (isOwner)
                MaterialTheme.colorScheme.onPrimaryContainer
            else
                MaterialTheme.colorScheme.onErrorContainer,
        ),
    ) {
        Text(
            text  = if (isOwner) "✓  Device Owner 已激活" else "✗  未激活",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
