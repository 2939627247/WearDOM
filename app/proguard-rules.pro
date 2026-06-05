# Keep Device Owner / Admin receiver (referenced by manifest)
-keep class com.example.weardomgr.WearDeviceAdminReceiver { *; }
-keep class * extends android.app.admin.DeviceAdminReceiver { *; }

# Keep all DevicePolicyManager-related classes
-keep class android.app.admin.** { *; }

# Keep data classes used in StateFlow (needed for reflection-free builds)
-keep class com.example.weardomgr.AppItem     { *; }
-keep class com.example.weardomgr.ProxyInput  { *; }
-keep class com.example.weardomgr.ProxyStatus { *; }
-keep class com.example.weardomgr.UiState     { *; }
