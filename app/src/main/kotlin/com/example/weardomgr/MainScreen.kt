package com.example.weardomgr

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
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
    val state       by vm.state.collectAsState()
    val listState    = rememberTransformingLazyColumnState()
    val isAdmin      = state.isDeviceOwner
    val hiddenCount  = remember(state.apps) { state.apps.count { it.isHidden } }
    val context      = LocalContext.current

    LaunchedEffect(Unit) { vm.refreshOwnerStatus() }

    ScreenScaffold(scrollState = listState) { contentPadding ->
        TransformingLazyColumn(
            state               = listState,
            modifier            = Modifier.fillMaxWidth(),
            contentPadding      = contentPadding,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {

            item {
                Text(
                    text      = "SmartThings",
                    style     = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier  = Modifier
                        .fillMaxWidth(),
                )
            }

            item {
                FeatureCard(
                    title       = "HTTP Proxy",
                    subtitle    = state.activeProxy?.toString() ?: "未配置",
                    checked     = state.activeProxy != null,
                    isAdmin     = isAdmin,
                    onCardClick = onProxy,
                    onToggle    = {
                        if (isAdmin) vm.toggleProxy()
                        else Toast.makeText(
                            context, "This app is not an admin.", Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier
                        .fillMaxWidth(),
                )
            }

            item {
                FeatureCard(
                    title       = "App Hide",
                    subtitle    = when {
                        !isAdmin        -> "需要 Device Owner"
                        hiddenCount > 0 -> "已隐藏 $hiddenCount 个"
                        else            -> "无隐藏应用"
                    },
                    checked     = hiddenCount > 0,
                    isAdmin     = isAdmin,
                    onCardClick = onAppHide,
                    onToggle    = {
                        if (!isAdmin) Toast.makeText(
                            context, "This app is not an admin.", Toast.LENGTH_SHORT
                        ).show()
                        else when {
                            hiddenCount > 0 -> vm.unhideAll()
                            else            -> onAppHide()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun FeatureCard(
    title: String,
    subtitle: String,
    checked: Boolean,
    isAdmin: Boolean,
    onCardClick: () -> Unit,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilledTonalButton(
        onClick  = onCardClick,
        modifier = modifier,
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                modifier            = Modifier
                    .weight(1f)
                    .alpha(if (isAdmin) 1f else 0.45f),
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                Text(
                    text     = title,
                    style    = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text     = subtitle,
                    style    = MaterialTheme.typography.labelSmall,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // Visible vertical divider — indicates the card has a sub-menu.
            // HorizontalDivider was wrong here (horizontal line, thickness=0dp = invisible).
            Box(
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .size(width = 1.dp, height = 28.dp)
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
            )

            Switch(
                checked         = checked && isAdmin,
                onCheckedChange = null,
                enabled         = true,
                modifier        = Modifier
                    .alpha(if (isAdmin) 1f else 0.45f)
                    .clickable { onToggle() },
            )
        }
    }
}
