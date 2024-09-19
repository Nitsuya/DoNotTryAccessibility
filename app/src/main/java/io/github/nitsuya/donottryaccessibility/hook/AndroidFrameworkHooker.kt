package io.github.nitsuya.donottryaccessibility.hook

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.AttributionSource
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.os.Binder
import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.log.YLog
import com.highcapable.yukihookapi.hook.type.android.BundleClass
import com.highcapable.yukihookapi.hook.type.java.IntType
import com.highcapable.yukihookapi.hook.type.java.ListClass
import com.highcapable.yukihookapi.hook.type.java.LongType
import com.highcapable.yukihookapi.hook.type.java.StringClass
import io.github.nitsuya.donottryaccessibility.bean.AppFiltersType
import io.github.nitsuya.donottryaccessibility.bean.AppInfoBean
import io.github.nitsuya.donottryaccessibility.data.ConfigData
import io.github.nitsuya.donottryaccessibility.utils.factory.appNameOf
import io.github.nitsuya.donottryaccessibility.utils.factory.listOfPackages
import io.github.nitsuya.donottryaccessibility.utils.tool.FrameworkTool
import java.util.WeakHashMap
import java.util.stream.Collectors

object AndroidFrameworkHooker : YukiBaseHooker() {

    override fun onHook() {
        registerLifecycle()
//        "com.android.server.am.ActivityManagerService".toClass().method{
//            name = "systemReady"
//        }.hook {
//            after {
//                YLog.debug(">>>>----  SystemReady Hook Start  ----<<<<")
//                removeSelf()
//                YLog.debug(">>>>----  SystemReady Hook End  ----<<<<")
//            }
//        }
    }

    private fun hookAccessibility() {
        val packageManager = systemContext.packageManager
        val uidToNameCache = WeakHashMap<Int, String>(1024)
        val pkgName = {
            Binder.getCallingUid().let { uid ->
                uidToNameCache.computeIfAbsent(uid) { _ ->
                    packageManager.getNameForUid(uid)
                }
            }
        }
        "com.android.server.accessibility.AccessibilityManagerService".toClass().apply {
            method {
                name = "addClient"
                paramCount = 2
                param {
                    it[0].name == "android.view.accessibility.IAccessibilityManagerClient"
                    it[1] == IntType /* userId */
                }
                returnType = LongType
            }.hook {
                before {
                    if (!ConfigData.blockApps.contains(pkgName())) return@before
                    result = 0L
                }
            }
            method {
                name = "getEnabledAccessibilityServiceList"
                param(IntType/* feedbackType */, IntType/* userId */)
                returnType = ListClass
            }.hook {
                before {
                    if (!ConfigData.blockApps.contains(pkgName())) return@before
                    result = listOf<AccessibilityServiceInfo>()
                }
            }
//            method {
//                name = "getInstalledAccessibilityServiceList"
//                param(IntType/* userId */)
//                returnType = ParceledListSlice::class.java
//            }.hook {
//                before {
//                    if (!blackList.contains(pkgName())) return@before
//                    result = ParceledListSlice.emptyList<AccessibilityServiceInfo>()
//                }
//            }
        }
        "android.content.ContentProvider\$Transport".toClass().apply {
            method {
                name = "call"
                param(AttributionSource::class.java, StringClass/* 1.authority */, StringClass/* 2.method */, StringClass/* 3.arg */, BundleClass/* 4.extras */)
                returnType = BundleClass
            }.hook {
                val secureKeys = mapOf<String, Bundle.() -> Unit>(
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES to {
                        putString(Settings.NameValueTable.VALUE, "")
                        putInt("_generation_index" /* Settings.CALL_METHOD_GENERATION_INDEX_KEY */, -1)
                    },
                    Settings.Secure.ACCESSIBILITY_ENABLED to {
                        putString(Settings.NameValueTable.VALUE, "0")
                        putInt("_generation_index" /* Settings.CALL_METHOD_GENERATION_INDEX_KEY */, -1)
                    }
                )
                after {
                    if(args(1).string() != Settings.AUTHORITY && args(2).string() != "GET_secure" /* Settings.CALL_METHOD_GET_SECURE */) return@after
                    secureKeys[args(3).string()]?.also { method ->
                        if (!ConfigData.blockApps.contains(pkgName())) return@after
                        result<Bundle>()?.apply(method)
                    }
                }
            }
        }
    }

    private fun registerLifecycle() {
        onAppLifecycle {
            onCreate {
                hookAccessibility()
            }
        }
        FrameworkTool.Host.with(instance = this) {
            onRefreshFrameworkPrefsData {
                /** 必要的延迟防止 Sp 存储不刷新 */
                SystemClock.sleep(500)
                /** 刷新存储类 */
                ConfigData.refresh()
                if (prefs.isPreferencesAvailable.not()) YLog.warn("Cannot refreshing app errors config data, preferences is not available")
            }
            onPushAppListData { filters ->
                appContext?.let { context ->
                    var info = context.listOfPackages().stream().filter { it.packageName.let { e -> e != "android" } }
                    if (filters.name.isNotBlank()) {
                        info = info.filter {
                            it.packageName.contains(filters.name) || context.appNameOf(it.packageName).contains(filters.name)
                        }
                    }
                    fun PackageInfo.isSystemApp() = (applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                    when (filters.type) {
                        AppFiltersType.USER -> info.filter { it.isSystemApp().not() }
                        AppFiltersType.SYSTEM -> info.filter { it.isSystemApp() }
                        AppFiltersType.ALL -> info
                    }.sorted (
                        Comparator.comparing(PackageInfo::lastUpdateTime).reversed()
                    ).map {
                        AppInfoBean(name = context.appNameOf(it.packageName), packageName = it.packageName)
                    }.collect(Collectors.toList())
                } ?: listOf()
            }
        }
    }
}