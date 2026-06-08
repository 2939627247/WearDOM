package com.example.weardomgr

import android.app.Application
import android.app.admin.DevicePolicyManager
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.ProxyInfo
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ─────────────────────────────── Data layer ──────────────────────────────────

data class AppItem(
    val packageName: String,
    val label: String,
    val isHidden: Boolean,
    val isSystemApp: Boolean,
)

data class ProxyInput(
    val host: String = "",
    val port: String = "",
    val exclusions: String = "",
)

data class ProxyStatus(
    val host: String,
    val port: Int,
    val exclusions: List<String> = emptyList(),
) {
    override fun toString() = "$host:$port"
}

data class UiState(
    val isDeviceOwner: Boolean    = false,
    val proxyInput: ProxyInput    = ProxyInput(),
    val activeProxy: ProxyStatus? = null,
    val apps: List<AppItem>       = emptyList(),
    val isLoadingApps: Boolean    = false,
    val appsFilter: String        = "",
)

// ─────────────────────────────── ViewModel ───────────────────────────────────

class DeviceOwnerViewModel(app: Application) : AndroidViewModel(app) {

    private val dpm: DevicePolicyManager =
        app.getSystemService(DevicePolicyManager::class.java)

    private val admin = WearDeviceAdminReceiver.componentName(app)

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init { refreshOwnerStatus() }

    // ──────────────────────── DO status ──────────────────────────────────────

    fun refreshOwnerStatus() {
        _state.update {
            it.copy(isDeviceOwner = dpm.isDeviceOwnerApp(getApplication<Application>().packageName))
        }
    }

    // ──────────────────────── HTTP Proxy ─────────────────────────────────────

    fun updateProxyInput(input: ProxyInput) =
        _state.update { it.copy(proxyInput = input) }

    fun applyProxy() = viewModelScope.launch {
        val input = _state.value.proxyInput
        if (input.host.isBlank()) return@launch
        val port = input.port.toIntOrNull()
        if (port == null || port !in 1..65535) return@launch

        safe {
            val exclusions = input.exclusions
                .split(",").map { it.trim() }.filter { it.isNotEmpty() }
            dpm.setRecommendedGlobalProxy(
                admin, ProxyInfo.buildDirectProxy(input.host, port, exclusions)
            )
            _state.update {
                it.copy(activeProxy = ProxyStatus(input.host, port, exclusions))
            }
        }
    }

    fun clearProxy() = viewModelScope.launch {
        safe {
            dpm.setRecommendedGlobalProxy(admin, null)
            _state.update { it.copy(activeProxy = null) }
        }
    }

    // ──────────────────────── App Hiding ─────────────────────────────────────

    fun loadApps() = viewModelScope.launch {
        _state.update { it.copy(isLoadingApps = true) }
        val pm      = getApplication<Application>().packageManager
        val selfPkg = getApplication<Application>().packageName

        val apps = withContext(Dispatchers.IO) {
            val rawList = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0L))
            } else {
                @Suppress("DEPRECATION") pm.getInstalledApplications(0)
            }
            rawList
                .filter { it.packageName != selfPkg }
                .mapNotNull { info ->
                    runCatching {
                        AppItem(
                            packageName = info.packageName,
                            label       = pm.getApplicationLabel(info).toString(),
                            isHidden    = runCatching {
                                dpm.isApplicationHidden(admin, info.packageName)
                            }.getOrDefault(false),
                            isSystemApp = info.flags and ApplicationInfo.FLAG_SYSTEM != 0,
                        )
                    }.getOrNull()
                }
                .sortedWith(compareBy({ it.isSystemApp }, { it.label.lowercase() }))
        }
        _state.update { it.copy(apps = apps, isLoadingApps = false) }
    }

    fun updateFilter(query: String) =
        _state.update { it.copy(appsFilter = query) }

    fun toggleHidden(packageName: String) = viewModelScope.launch {
        val target = _state.value.apps
            .find { it.packageName == packageName } ?: return@launch
        safe {
            val newHidden = !target.isHidden
            if (dpm.setApplicationHidden(admin, packageName, newHidden)) {
                _state.update { s ->
                    s.copy(apps = s.apps.map {
                        if (it.packageName == packageName) it.copy(isHidden = newHidden) else it
                    })
                }
            }
        }
    }

    // ──────────────────────── Main-screen quick-actions ──────────────────────

    fun toggleProxy() {
        if (_state.value.activeProxy != null) clearProxy() else applyProxy()
    }

    fun unhideAll() = viewModelScope.launch {
        safe {
            val hidden = _state.value.apps.filter { it.isHidden }
            hidden.forEach { app ->
                dpm.setApplicationHidden(admin, app.packageName, false)
            }
            _state.update { s ->
                s.copy(apps = s.apps.map { it.copy(isHidden = false) })
            }
        }
    }

    // ──────────────────────── Helper ─────────────────────────────────────────

    private suspend fun safe(block: suspend () -> Unit) = runCatching { block() }
}
