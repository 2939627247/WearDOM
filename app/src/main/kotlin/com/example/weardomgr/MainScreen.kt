package com.example.weardomgr

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Switch
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
    val listState    = rememberScalingLazyListState()
    val isAdmin      = state.isDeviceOwner
    val hiddenCount  = state.apps.count { it.isHidden }

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

            item {
                Text(
                    text      = "SmartThings",
                    style     = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier  = Modifier.fillMaxWidth(),
                )
            }

            // ── HTTP Proxy ─────────────────────────────────────────────────
            item {
                FeatureCard(
                    title       = "HTTP Proxy",
                    subtitle    = state.activeProxy?.toString() ?: "未配置",
                    checked     = state.activeProxy != null,
                    isAdmin     = isAdmin,
                    onCardClick = onProxy,
                    onToggle    = {
                        if (isAdmin) vm.toggleProxy() else vm.notifyNotAdmin()
                    },
                )
            }

            // ── App Hide ───────────────────────────────────────────────────
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
                        when {
                            !isAdmin        -> vm.notifyNotAdmin()
                            hiddenCount > 0 -> vm.unhideAll()
                            else            -> onAppHide()
                        }
                    },
                )
            }

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
                        modifier  = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

// ─────────────────────────── FeatureCard ─────────────────────────────────────
//
// FilledTonalButton as the card surface (standard Wear M3 touch target).
// Inside: Row with text on the left, HorizontalDivider rotated as a separator,
// and the M3 Switch on the right.
//
// Click isolation:
//   • Switch area: Modifier.clickable { onToggle() } consumes the tap →
//     the outer button's onClick does NOT also fire.
//   • Everything else taps through to the button → onCardClick().

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
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier          = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {

            // ── Text ──────────────────────────────────────────────────────
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

            // ── Divider (visual only) ─────────────────────────────────────
            HorizontalDivider(
                modifier  = Modifier
                    .padding(horizontal = 8.dp)
                    .weight(0.01f),   // zero visual width; acts as spacing sentinel
                thickness = 0.dp,
            )

            // ── M3 Switch ─────────────────────────────────────────────────
            // Modifier.clickable wraps only the Switch, consuming the tap
            // so FilledTonalButton.onClick is not triggered simultaneously.
            Switch(
                checked         = checked && isAdmin,
                onCheckedChange = null,   // interaction handled by the clickable below
                enabled         = true,
                modifier        = Modifier
                    .alpha(if (isAdmin) 1f else 0.45f)
                    .clickable { onToggle() },
            )
        }
    }
}
