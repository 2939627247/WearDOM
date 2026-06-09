package com.example.weardomgr

import android.app.Application
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.ProxyInfo
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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

    // SharedPreferences — persists the last applied proxy across process restarts.
    // (DPM has no getter for setRecommendedGlobalProxy, so we store it ourselves.)
    private val prefs = app.getSharedPreferences("smartthings", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        refreshOwnerStatus()
        // Restore proxy that was applied in a previous session
        val saved = restoreProxy()
        if (saved != null) {
            _state.update {
                it.copy(
                    activeProxy = saved,
                    // Pre-fill input fields so user can see / edit last config
                    proxyInput  = ProxyInput(
                        host       = saved.host,
                        port       = saved.port.toString(),
                        exclusions = saved.exclusions.joinToString(","),
                    ),
                )
            }
        }
    }

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
            val status = ProxyStatus(input.host, port, exclusions)
            saveProxy(status)                             // persist
            _state.update { it.copy(activeProxy = status) }
        }
    }

    fun clearProxy() = viewModelScope.launch {
        safe {
            dpm.setRecommendedGlobalProxy(admin, null)
            saveProxy(null)                               // clear persisted
            _state.update { it.copy(activeProxy = null) }
        }
    }

    // ──────────────────────── App Hiding ─────────────────────────────────────

    // Cancels any in-flight load before starting a new one, preventing a slower
    // earlier load from overwriting the results of a faster later one.
    private var loadAppsJob: Job? = null

    fun loadApps() {
        loadAppsJob?.cancel()
        loadAppsJob = viewModelScope.launch {
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
        val hidden = _state.value.apps.filter { it.isHidden }
        // Process each app independently: a failure on one must not prevent
        // the others from being unhidden, and only successfully changed apps
        // should be reflected in the UI state.
        val unhiddenPkgs = withContext(Dispatchers.IO) {
            hidden
                .filter { app ->
                    runCatching {
                        dpm.setApplicationHidden(admin, app.packageName, false)
                    }.getOrDefault(false)
                }
                .map { it.packageName }
                .toHashSet()
        }
        if (unhiddenPkgs.isNotEmpty()) {
            _state.update { s ->
                s.copy(apps = s.apps.map {
                    if (it.packageName in unhiddenPkgs) it.copy(isHidden = false) else it
                })
            }
        }
    }

    // ──────────────────────── Persistence helpers ─────────────────────────────

    private fun saveProxy(proxy: ProxyStatus?) {
        prefs.edit().apply {
            if (proxy != null) {
                putString("proxy_host",       proxy.host)
                putInt(   "proxy_port",       proxy.port)
                putString("proxy_exclusions", proxy.exclusions.joinToString(","))
            } else {
                remove("proxy_host")
                remove("proxy_port")
                remove("proxy_exclusions")
            }
        }.apply()
    }

    private fun restoreProxy(): ProxyStatus? {
        val host = prefs.getString("proxy_host", null) ?: return null
        val port = prefs.getInt("proxy_port", -1).takeIf { it in 1..65535 } ?: return null
        val exclusions = prefs.getString("proxy_exclusions", "")
            ?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
        return ProxyStatus(host, port, exclusions)
    }

    // ──────────────────────── Internal helper ────────────────────────────────

    private suspend fun safe(block: suspend () -> Unit) = runCatching { block() }
}
