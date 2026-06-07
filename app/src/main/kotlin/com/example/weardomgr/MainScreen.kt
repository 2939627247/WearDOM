package com.example.weardomgr

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text

// ─────────────────────────── Dimensions ──────────────────────────────────────
private val CARD_RADIUS    = RoundedCornerShape(26.dp)
private val TRACK_W        = 52.dp
private val TRACK_H        = 28.dp
private val THUMB_D        = 24.dp   // thumb diameter
private val THUMB_PAD      = 2.dp    // gap between thumb and track edge
private val ANIM_MS        = 180     // toggle animation duration

// ─────────────────────────── MainScreen ──────────────────────────────────────

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

            // ── In-list toast ──────────────────────────────────────────────
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
                        modifier  = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                    )
                }
            }
        }
    }
}

// ─────────────────────────── FeatureCard ─────────────────────────────────────
//
// Layout (matches the reference screenshot):
//
//  ╔══════════════════════════════════════╗
//  ║  Title               │  ┌──────────┐ ║
//  ║  Subtitle            │  │   ○  ●   │ ║
//  ╚══════════════════════════════════════╝
//        ↑ tap → sub-menu     ↑ tap → toggle only
//
// The divider is purely visual — it carries no functional separation.
// Clicking the SWITCH track is the only way to toggle; any other tap
// on the card navigates to the settings sub-menu.
// The switch click is consumed by the inner clickable, so the outer
// card clickable does NOT fire simultaneously.
//
// When isAdmin = false the whole card dims to 45 % alpha; tapping the
// switch shows "This app is not an admin." instead of acting.

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
    val cardBg    = MaterialTheme.colorScheme.surfaceContainer
    val dividerCl = MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(CARD_RADIUS)
            .background(cardBg)
            // Whole card navigates — inner switch clickable will consume
            // its own tap, so the card handler fires only outside the switch.
            .clickable { onCardClick() }
            .padding(start = 16.dp, end = 10.dp, top = 12.dp, bottom = 12.dp),
    ) {
        Row(
            modifier          = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {

            // ── Text area ─────────────────────────────────────────────────
            Column(
                modifier            = Modifier
                    .weight(1f)
                    .alpha(if (isAdmin) 1f else 0.45f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
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

            // ── Divider — visual only, no functional role ─────────────────
            Box(
                modifier = Modifier
                    .padding(horizontal = 10.dp)
                    .width(1.dp)
                    .height(30.dp)
                    .background(dividerCl),
            )

            // ── Toggle switch ─────────────────────────────────────────────
            // Touch target = switch track only (TRACK_W × TRACK_H).
            // Its clickable consumes the pointer → card handler skipped.
            TrackSwitch(
                checked   = checked && isAdmin,
                onToggle  = onToggle,
                // dim switch visually when not admin (card text already dimmed above)
                modifier  = Modifier.alpha(if (isAdmin) 1f else 0.45f),
            )
        }
    }
}

// ─────────────────────────── TrackSwitch ─────────────────────────────────────
//
// A self-contained animated toggle that looks like a standard Android switch:
//  • pill-shaped track  (blue when on, surface-variant when off)
//  • round white thumb  (slides left ↔ right with a short tween)
//  • touch target       = track bounds only (TRACK_W × TRACK_H)
//  • Modifier.clickable consumes the pointer event, preventing the parent
//    FeatureCard.clickable from also firing.

@Composable
private fun TrackSwitch(
    checked: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val trackColor by animateColorAsState(
        targetValue = if (checked) MaterialTheme.colorScheme.primary
                      else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.30f),
        animationSpec = tween(ANIM_MS),
        label = "trackColor",
    )
    val thumbOffset by animateDpAsState(
        targetValue   = if (checked) TRACK_W - THUMB_D - THUMB_PAD else THUMB_PAD,
        animationSpec = tween(ANIM_MS),
        label = "thumbOffset",
    )

    Box(
        modifier = modifier
            .width(TRACK_W)
            .height(TRACK_H)
            .clip(CircleShape)
            .background(trackColor)
            // THIS clickable consumes the touch → outer card clickable does not fire
            .clickable { onToggle() },
    ) {
        Box(
            modifier = Modifier
                .padding(start = thumbOffset, top = THUMB_PAD)
                .size(THUMB_D)
                .clip(CircleShape)
                .background(Color.White),
        )
    }
}
