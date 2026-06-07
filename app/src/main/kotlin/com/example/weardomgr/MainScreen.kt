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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SplitToggleButton
import androidx.wear.compose.material3.Switch
import androidx.wear.compose.material3.Text

@Composable
fun MainScreen(
    vm: DeviceOwnerViewModel,
    onProxy: () -> Unit,
    onAppHide: () -> Unit,
) {
    val state     by vm.state.collectAsState()
    val listState  = rememberScalingLazyListState()
    val isAdmin    = state.isDeviceOwner
    val hiddenCount = state.apps.count { it.isHidden }

    LaunchedEffect(Unit) { vm.refreshOwnerStatus() }

    ScreenScaffold(scrollState = listState) {
        ScalingLazyColumn(
            state               = listState,
            modifier            = Modifier.fillMaxWidth(),
            contentPadding      = PaddingValues(
                top = 40.dp, bottom = 32.dp, start = 8.dp, end = 8.dp,
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {

            // ── Title ──────────────────────────────────────────────────────
            item {
                Text(
                    text      = "SmartThings",
                    style     = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier  = Modifier.fillMaxWidth(),
                )
            }

            // ── HTTP Proxy card ────────────────────────────────────────────
            item {
                FeatureCard(
                    title    = "HTTP Proxy",
                    subtitle = state.activeProxy?.toString() ?: "未配置",
                    checked  = state.activeProxy != null,
                    isAdmin  = isAdmin,
                    // Switch: toggle proxy on/off; shows toast when not admin
                    onToggle = {
                        if (isAdmin) vm.toggleProxy() else vm.notifyNotAdmin()
                    },
                    // Card body: always navigates to proxy settings
                    onCardClick = onProxy,
                )
            }

            // ── App Hide card ──────────────────────────────────────────────
            item {
                FeatureCard(
                    title    = "App Hide",
                    subtitle = when {
                        !isAdmin    -> "需要 Device Owner"
                        hiddenCount > 0 -> "已隐藏 $hiddenCount 个应用"
                        else        -> "无隐藏应用"
                    },
                    checked  = hiddenCount > 0,
                    isAdmin  = isAdmin,
                    // Switch ON→OFF: unhide all;  OFF→ON: open settings to configure
                    onToggle = {
                        when {
                            !isAdmin        -> vm.notifyNotAdmin()
                            hiddenCount > 0 -> vm.unhideAll()   // turn off = show all
                            else            -> onAppHide()       // turn on  = go configure
                        }
                    },
                    onCardClick = onAppHide,
                )
            }

            // ── Toast message ──────────────────────────────────────────────
            if (state.message != null) {
                item {
                    Text(
                        text      = state.message!!,
                        style     = MaterialTheme.typography.labelSmall,
                        color     = if (state.message!!.contains("not an admin"))
                                        MaterialTheme.colorScheme.error
                                    else
                                        MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier  = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    )
                }
            }
        }
    }
}

// ─────────────────────── FeatureCard ─────────────────────────────────────────

/**
 * A Wear OS split-toggle card:
 *  • Left area (card body) → always clickable → navigates to settings
 *  • Right area (switch)   → toggles the feature when admin;
 *                            shows "not an admin" toast otherwise
 *
 * When [isAdmin] is false the card text is dimmed to signal the restriction,
 * but the card itself remains tappable so the user can still explore settings.
 */
@Composable
private fun FeatureCard(
    title: String,
    subtitle: String,
    checked: Boolean,
    isAdmin: Boolean,
    onToggle: () -> Unit,
    onCardClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SplitToggleButton(
        checked         = checked,
        onCheckedChange = { onToggle() },
        onClick         = onCardClick,         // always navigable
        toggleControl   = { Switch() },
        modifier        = modifier.fillMaxWidth(),
        // keep enabled=true so the card-click still fires when not admin
        enabled         = true,
    ) {
        Column(
            modifier            = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                // dim text when no admin rights, but keep card tappable
                .alpha(if (isAdmin) 1f else 0.45f),
            verticalArrangement = Arrangement.Center,
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
    }
}
