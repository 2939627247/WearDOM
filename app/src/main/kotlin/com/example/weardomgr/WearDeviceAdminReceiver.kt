package com.example.weardomgr

import android.app.admin.DeviceAdminReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * The Device Admin Receiver that must be declared in the manifest.
 *
 * To activate Device Owner mode (required for all DO APIs), run once via ADB:
 *
 *   adb shell dpm set-device-owner \
 *       com.example.weardomgr/.WearDeviceAdminReceiver
 *
 * Prerequisites for the above command:
 *   - No Google accounts added to the device
 *   - No other device owners present
 *   - On a real watch, developer options enabled
 */
class WearDeviceAdminReceiver : DeviceAdminReceiver() {

    companion object {
        private const val TAG = "WearAdminReceiver"

        /** Helper to obtain the ComponentName without requiring a live instance. */
        fun componentName(context: Context): ComponentName =
            ComponentName(context.packageName, WearDeviceAdminReceiver::class.java.name)
    }

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Log.i(TAG, "Device Admin enabled")
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Log.w(TAG, "Device Admin disabled — DO features no longer available")
    }

    /**
     * Fired when zero-touch / NFC / QR-code provisioning completes.
     * Good place to apply initial DO policy (proxy, kiosk, etc.).
     */
    override fun onProfileProvisioningComplete(context: Context, intent: Intent) {
        super.onProfileProvisioningComplete(context, intent)
        Log.i(TAG, "Profile provisioning complete — DO active")
    }

    override fun onLockTaskModeEntering(context: Context, intent: Intent, pkg: String) {
        super.onLockTaskModeEntering(context, intent, pkg)
        Log.i(TAG, "Lock-task mode entering: $pkg")
    }

    override fun onLockTaskModeExiting(context: Context, intent: Intent) {
        super.onLockTaskModeExiting(context, intent)
        Log.i(TAG, "Lock-task mode exiting")
    }
}
