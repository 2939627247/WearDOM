# WearDOMgr — Wear OS Device Owner 管理器

Wear OS 企业级应用，使用 **Device Owner (DO)** API 实现：

| 功能 | DO API |
|------|--------|
| HTTP 全局代理 | `DevicePolicyManager.setRecommendedGlobalProxy()` |
| 应用隐藏/显示 | `DevicePolicyManager.setApplicationHidden()` / `isApplicationHidden()` |

---

## 技术栈

| 组件 | 版本 |
|------|------|
| Android Gradle Plugin | 8.10.0 |
| Kotlin | 2.2.0 |
| Wear Compose Material3 | **1.6.2** |
| Wear Compose Foundation | 1.6.2 |
| Wear Compose Navigation | 1.6.2 |
| Compose BOM | 2025.06.00 |
| compileSdk / targetSdk | **36 (Android 16)** |
| minSdk | 30 (Wear OS 3) |

---

## 项目结构

```
app/src/main/
├── kotlin/com/example/weardomgr/
│   ├── WearDeviceAdminReceiver.kt   # DeviceAdminReceiver + ComponentName helper
│   ├── DeviceOwnerViewModel.kt      # 全部 DO 业务逻辑 + StateFlow UI state
│   ├── MainActivity.kt             # Activity + SwipeDismissableNavHost
│   ├── Theme.kt                    # Wear Material3 主题
│   ├── MainScreen.kt               # 主屏：DO 状态 + 导航
│   ├── ProxyScreen.kt              # HTTP 代理配置
│   └── AppHideScreen.kt            # 应用隐藏列表
├── res/xml/device_admin.xml         # Device Admin 策略声明
└── AndroidManifest.xml
```

---

## 激活 Device Owner

> ⚠️ DO 只能通过以下方式激活，**无法在运行时动态获取**。

### 方式一：ADB（推荐用于开发/测试）

```bash
# 前置条件：
# 1. 设备未添加任何 Google 账号（或已出厂重置）
# 2. 未存在其他 Device Owner
# 3. 已开启开发者选项 + USB 调试

adb shell dpm set-device-owner \
    com.example.weardomgr/.WearDeviceAdminReceiver
```

成功输出：
```
Success: Device owner set to package com.example.weardomgr
```

### 方式二：NFC / QR 码零接触配置（生产环境）

通过 Android Enterprise 零接触注册或 DPC 设备配置器进行自动化部署，
`AndroidManifest.xml` 中已声明 `PROFILE_PROVISIONING_COMPLETE` intent。

### 撤销 Device Owner（测试后清理）

```bash
adb shell dpm remove-active-admin \
    com.example.weardomgr/.WearDeviceAdminReceiver
```

---

## HTTP 代理功能

**API**：`DevicePolicyManager.setRecommendedGlobalProxy(ComponentName, ProxyInfo)`

- 构造 `ProxyInfo.buildDirectProxy(host, port, exclusionList)`
- 传入 `null` 清除代理
- 代理对所有网络生效（Wi-Fi + 蜂窝）
- 适用于企业内网/Squid/Clash 等中间代理场景

---

## 应用隐藏功能

**API**：`DevicePolicyManager.setApplicationHidden(ComponentName, packageName, hidden)`

- `hidden = true`：从启动器隐藏，用户无法启动，**数据完整保留**
- `hidden = false`：立即恢复可见
- `isApplicationHidden()`：查询当前隐藏状态
- 部分受保护的系统包（如系统 UI）返回 `false`，UI 会提示"受保护"
- 需要 `QUERY_ALL_PACKAGES` 权限枚举全部已安装应用

---

## 构建与安装

```bash
# Debug 安装到已连接的手表
./gradlew :app:installDebug

# Release 构建（需配置签名）
./gradlew :app:assembleRelease
```

---

## 注意事项

1. **Emulator**：Wear OS 模拟器默认无 Google 账号，可直接通过 ADB 激活 DO。
2. **真机**：若已登录账号，需先出厂重置（企业设备推荐使用零接触注册）。
3. **minSdk 30**：针对 Wear OS 3.x；`SplitToggleButton` 等 Material3 组件需要此最低版本。
4. **on-screen keyboard**：Wear OS 3+ 内置键盘，`BasicTextField` 点击即弹出，无需额外处理。
