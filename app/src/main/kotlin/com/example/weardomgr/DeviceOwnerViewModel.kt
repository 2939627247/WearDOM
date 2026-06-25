package com.example.weardomgr

import android.app.Application
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.ProxyInfo
import android.os.Build
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// DataStore delegate — must be at file level (one instance per process).
// Replaces SharedPreferences: async, atomic, never blocks the main thread.
private val Context.dataStore: DataStore<Preferences>
    by preferencesDataStore(name = "smartthings")

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

    private val admin    = WearDeviceAdminReceiver.componentName(app)
    private val dataStore = app.dataStore

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        refreshOwnerStatus()
        // Restore persisted proxy: DataStore exposes data as a Flow, so
        // we collect the first emission in a coroutine (non-blocking).
        viewModelScope.launch {
            restoreProxy()?.let { saved ->
                _state.update {
                    it.copy(
                        activeProxy = saved,
                        proxyInput  = ProxyInput(
                            host       = saved.host,
                            port       = saved.port.toString(),
                            exclusions = saved.exclusions.joinToString(","),
                        ),
                    )
                }
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
            saveProxy(status)
            _state.update { it.copy(activeProxy = status) }
        }
    }

    fun clearProxy() = viewModelScope.launch {
        safe {
            dpm.setRecommendedGlobalProxy(admin, null)
            saveProxy(null)
            _state.update { it.copy(activeProxy = null) }
        }
    }

    // ──────────────────────── App Hiding ─────────────────────────────────────

    private var loadAppsJob: Job? = null

    fun loadApps() {
        loadAppsJob?.cancel()
        loadAppsJob = viewModelScope.launch {
            _state.update { it.copy(isLoadingApps = true) }
            val pm      = getApplication<Application>().packageManager
            val selfPkg = getApplication<Application>().packageName
            // Step 1 — fast: fetch raw list on IO thread
            val rawList = withContext(Dispatchers.IO) {
                val list = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0L))
                } else {
                    @Suppress("DEPRECATION") pm.getInstalledApplications(0)
                }
                list.filter { it.packageName != selfPkg }
            }

            // Step 2 — parallel: resolve labels + hidden state for all apps at once.
            // Sequential resolution of 100+ apps can take several seconds on a watch;
            // parallel cuts it to roughly one iteration's worth of time.
            // Sort happens on IO thread — keeps main thread free during list completion
            val apps = withContext(Dispatchers.IO) {
                coroutineScope {
                    rawList.map { info ->
                        async {
                            runCatching {
                                AppItem(
                                    packageName = info.packageName,
                                    label       = info.loadLabel(pm).toString(),
                                    isHidden    = runCatching {
                                        dpm.isApplicationHidden(admin, info.packageName)
                                    }.getOrDefault(false),
                                    isSystemApp = info.flags and ApplicationInfo.FLAG_SYSTEM != 0,
                                )
                            }.getOrNull()
                        }
                    }.awaitAll()
                }
                .filterNotNull()
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

    // ──────────────────────── DataStore helpers ───────────────────────────────

    private object ProxyKeys {
        val HOST       = stringPreferencesKey("proxy_host")
        val PORT       = intPreferencesKey("proxy_port")
        val EXCLUSIONS = stringPreferencesKey("proxy_exclusions")
    }

    private suspend fun saveProxy(proxy: ProxyStatus?) {
        dataStore.edit { prefs ->
            if (proxy != null) {
                prefs[ProxyKeys.HOST]       = proxy.host
                prefs[ProxyKeys.PORT]       = proxy.port
                prefs[ProxyKeys.EXCLUSIONS] = proxy.exclusions.joinToString(",")
            } else {
                prefs.remove(ProxyKeys.HOST)
                prefs.remove(ProxyKeys.PORT)
                prefs.remove(ProxyKeys.EXCLUSIONS)
            }
        }
    }

    private suspend fun restoreProxy(): ProxyStatus? =
        dataStore.data.map { prefs ->
            val host = prefs[ProxyKeys.HOST] ?: return@map null
            val port = prefs[ProxyKeys.PORT]?.takeIf { it in 1..65535 } ?: return@map null
            val exclusions = prefs[ProxyKeys.EXCLUSIONS]
                ?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }
                ?: emptyList()
            ProxyStatus(host, port, exclusions)
        }.first()

    // ──────────────────────── Internal helper ────────────────────────────────

    private suspend fun safe(block: suspend () -> Unit) = runCatching { block() }
}
