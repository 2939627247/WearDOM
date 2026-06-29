package com.example.weardomgr

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SplitSwitchButton
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight

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
    val spec         = rememberTransformationSpec()

    LaunchedEffect(Unit) { vm.refreshOwnerStatus() }

    ScreenScaffold(scrollState = listState) { contentPadding ->
        TransformingLazyColumn(
            state               = listState,
            contentPadding      = contentPadding,
            modifier            = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {

            item {
                Text(
                    text      = "SmartThings",
                    style     = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier  = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, spec),
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
                        .fillMaxWidth()
                        .transformedHeight(this, spec),
                    spec     = spec,
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
                        .fillMaxWidth()
                        .transformedHeight(this, spec),
                    spec     = spec,
                )
            }
        }
    }
}

// ── FeatureCard ───────────────────────────────────────────────────────────────
// SplitSwitchButton is the correct Wear M3 component for a card with two
// independent tap areas: label area → sub-menu navigation, switch area → toggle.
// No custom Switch, no ripple workarounds, no manual clickable handling.

@Composable
private fun FeatureCard(
    title: String,
    subtitle: String,
    checked: Boolean,
    isAdmin: Boolean,
    onCardClick: () -> Unit,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    spec: androidx.wear.compose.material3.lazy.TransformationSpec? = null,
) {
    SplitSwitchButton(
        checked                = checked,
        // ignore new-value Boolean — our onToggle owns the state logic
        onCheckedChange        = { onToggle() },
        toggleContentDescription = title,
        onContainerClick       = onCardClick,
        modifier               = modifier,
        // Container always enabled so navigation works even without DO permission;
        // permission check is handled inside onToggle via Toast
        enabled                = true,
        transformation         = spec?.let { SurfaceTransformation(it) },
        label = {
            Column(
                // Dim label when DO permission is absent as visual hint
                modifier            = Modifier.alpha(if (isAdmin) 1f else 0.45f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
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
        },
    )
}
