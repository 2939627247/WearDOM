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
import kotlinx.coroutines.delay
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

/** Mutable form-state for the proxy configuration screen. */
data class ProxyInput(
    val host: String = "",
    val port: String = "",
    /** Comma-separated exclusion list, e.g. "localhost,*.corp.local" */
    val exclusions: String = "",
)

/** Snapshot of the currently active proxy (null = no proxy set). */
data class ProxyStatus(
    val host: String,
    val port: Int,
    val exclusions: List<String> = emptyList(),
) {
    override fun toString() = "$host:$port"
}

data class UiState(
    val isDeviceOwner: Boolean    = false,
    // ── Proxy ──
    val proxyInput: ProxyInput    = ProxyInput(),
    val activeProxy: ProxyStatus? = null,
    // ── App hiding ──
    val apps: List<AppItem>       = emptyList(),
    val isLoadingApps: Boolean    = false,
    val appsFilter: String        = "",
    // ── Feedback ──
    val message: String?          = null,
)

// ─────────────────────────────── ViewModel ───────────────────────────────────

class DeviceOwnerViewModel(app: Application) : AndroidViewModel(app) {

    private val dpm: DevicePolicyManager =
        app.getSystemService(DevicePolicyManager::class.java)

    private val admin = WearDeviceAdminReceiver.componentName(app)

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        refreshOwnerStatus()
    }

    // ──────────────────────── DO status ──────────────────────────────────────

    fun refreshOwnerStatus() {
        _state.update {
            it.copy(isDeviceOwner = dpm.isDeviceOwnerApp(getApplication<Application>().packageName))
        }
    }

    // ──────────────────────── HTTP Proxy (DO API) ────────────────────────────

    /** Update the proxy form inputs without applying them. */
    fun updateProxyInput(input: ProxyInput) =
        _state.update { it.copy(proxyInput = input) }

    /**
     * Apply the current [ProxyInput] as the device-wide recommended HTTP proxy.
     *
     * Uses [DevicePolicyManager.setRecommendedGlobalProxy] — a Device Owner-only API
     * that sets a global proxy recommendation enforced across the device.
     */
    fun applyProxy() = viewModelScope.launch {
        val input = _state.value.proxyInput

        // ── Validate ──
        if (input.host.isBlank()) { toast("主机地址不能为空"); return@launch }
        val port = input.port.toIntOrNull()
        if (port == null || port !in 1..65535) { toast("端口号无效 (1–65535)"); return@launch }

        safe("应用代理") {
            val exclusions = input.exclusions
                .split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }

            // Build the ProxyInfo and push it via the DO API
            val proxyInfo = ProxyInfo.buildDirectProxy(input.host, port, exclusions)
            dpm.setRecommendedGlobalProxy(admin, proxyInfo)

            _state.update {
                it.copy(activeProxy = ProxyStatus(input.host, port, exclusions))
            }
            toast("✓ 代理已设置: ${input.host}:$port")
        }
    }

    /**
     * Clear the device-wide HTTP proxy.
     * Passing `null` to [DevicePolicyManager.setRecommendedGlobalProxy] removes it.
     */
    fun clearProxy() = viewModelScope.launch {
        safe("清除代理") {
            dpm.setRecommendedGlobalProxy(admin, null)
            _state.update { it.copy(activeProxy = null) }
            toast("✓ 代理已清除")
        }
    }

    // ──────────────────────── App Hiding (DO API) ────────────────────────────

    /**
     * Load all installed applications (excluding self) and query their
     * hidden state via [DevicePolicyManager.isApplicationHidden].
     */
    fun loadApps() = viewModelScope.launch {
        _state.update { it.copy(isLoadingApps = true) }

        val pm     = getApplication<Application>().packageManager
        val selfPkg = getApplication<Application>().packageName

        val apps = withContext(Dispatchers.IO) {
            // API 33+ uses ApplicationInfoFlags; below 33 use the int overload
            val rawList = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0L))
            } else {
                @Suppress("DEPRECATION")
                pm.getInstalledApplications(0)
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
                // User apps first, then system apps; alphabetical within each group
                .sortedWith(compareBy({ it.isSystemApp }, { it.label.lowercase() }))
        }

        _state.update { it.copy(apps = apps, isLoadingApps = false) }
    }

    /** Filter the app list by name or package name. */
    fun updateFilter(query: String) =
        _state.update { it.copy(appsFilter = query) }

    /**
     * Toggle an app's visibility using [DevicePolicyManager.setApplicationHidden].
     *
     * Device Owner exclusive: hides the app from the launcher and prevents
     * the user from launching it. The app and its data are NOT removed.
     *
     * Note: some protected system packages may return `false` (cannot be hidden).
     */
    fun toggleHidden(packageName: String) = viewModelScope.launch {
        val target = _state.value.apps
            .find { it.packageName == packageName } ?: return@launch

        safe("切换可见性") {
            val newHidden = !target.isHidden
            val success   = dpm.setApplicationHidden(admin, packageName, newHidden)

            if (success) {
                _state.update { s ->
                    s.copy(
                        apps = s.apps.map {
                            if (it.packageName == packageName) it.copy(isHidden = newHidden) else it
                        }
                    )
                }
            } else {
                toast("无法修改 "${target.label}"（受保护）")
            }
        }
    }

    // ──────────────────────── Helpers ────────────────────────────────────────

    fun clearMessage() = _state.update { it.copy(message = null) }

    private fun toast(msg: String) {
        _state.update { it.copy(message = msg) }
        // Auto-clear after 3 s so the UI composable doesn't need to manage it
        viewModelScope.launch {
            delay(3_000)
            _state.update { if (it.message == msg) it.copy(message = null) else it }
        }
    }

    private suspend fun safe(actionName: String, block: suspend () -> Unit) =
        runCatching { block() }
            .onFailure { e -> toast("$actionName 失败: ${e.message}") }
}
