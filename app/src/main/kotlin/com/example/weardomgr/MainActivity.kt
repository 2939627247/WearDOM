package com.example.weardomgr

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WearAppTheme {
                WearDOMgrApp()
            }
        }
    }
}

/** Root composable: AppScaffold owns TimeText across navigation transitions. */
@Composable
private fun WearDOMgrApp() {
    val vm  = viewModel<DeviceOwnerViewModel>()
    val nav = rememberSwipeDismissableNavController()

    AppScaffold {
        SwipeDismissableNavHost(
            navController    = nav,
            startDestination = Route.HOME,
        ) {
            composable(Route.HOME) {
                MainScreen(
                    vm        = vm,
                    onProxy   = { nav.navigate(Route.PROXY) },
                    onAppHide = { nav.navigate(Route.APP_HIDE) },
                )
            }
            composable(Route.PROXY)    { ProxyScreen(vm = vm) }
            composable(Route.APP_HIDE) { AppHideScreen(vm = vm) }
        }
    }
}

private object Route {
    const val HOME     = "home"
    const val PROXY    = "proxy"
    const val APP_HIDE = "apphide"
}
