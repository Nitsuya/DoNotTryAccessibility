@file:Suppress("unused")

package io.github.nitsuya.donottryaccessibility.utils.factory

import android.app.Activity
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.PackageInfoFlags
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.core.content.pm.PackageInfoCompat
import androidx.core.content.res.ResourcesCompat
import com.google.android.material.snackbar.Snackbar
import com.highcapable.yukihookapi.hook.factory.field
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.log.YLog
import com.highcapable.yukihookapi.hook.type.android.ApplicationInfoClass
import com.highcapable.yukihookapi.hook.type.android.ContextClass
import com.highcapable.yukihookapi.hook.type.android.IntentClass
import com.highcapable.yukihookapi.hook.type.android.UserHandleClass
import io.github.nitsuya.donottryaccessibility.BuildConfig
import io.github.nitsuya.donottryaccessibility.R
import java.io.Serializable
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 当前系统环境是否为简体中文
 * @return [Boolean]
 */
val isSystemLanguageSimplifiedChinese
    get(): Boolean {
        val locale = Locale.getDefault()
        return locale.language == "zh" && locale.country == "CN"
    }

/**
 * 系统深色模式是否开启
 * @return [Boolean] 是否开启
 */
val Context.isSystemInDarkMode get() = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

/**
 * 系统深色模式是否没开启
 * @return [Boolean] 是否开启
 */
inline val Context.isNotSystemInDarkMode get() = !isSystemInDarkMode

/**
 * dp 转换为 pxInt
 * @param context 使用的实例
 * @return [Int]
 */
fun Number.dp(context: Context) = dpFloat(context).toInt()

/**
 * dp 转换为 pxFloat
 * @param context 使用的实例
 * @return [Float]
 */
fun Number.dpFloat(context: Context) = toFloat() * context.resources.displayMetrics.density

/**
 * 获取 [Drawable]
 * @param resId 属性资源 ID
 * @return [Drawable]
 */
fun Resources.drawableOf(@DrawableRes resId: Int) = ResourcesCompat.getDrawable(this, resId, null) ?: error("Invalid resources")

/**
 * 获取颜色
 * @param resId 属性资源 ID
 * @return [Int]
 */
fun Resources.colorOf(@ColorRes resId: Int) = ResourcesCompat.getColor(this, resId, null)

/**
 * 得到 APP 安装包信息 (兼容)
 * @param packageName APP 包名
 * @param flag [PackageInfoFlags]
 * @return [PackageInfo] or null
 */
private fun Context.getPackageInfoCompat(packageName: String, flag: Number = 0) = runCatching {
    @Suppress("DEPRECATION", "KotlinRedundantDiagnosticSuppress")
    if (Build.VERSION.SDK_INT >= 33)
        packageManager?.getPackageInfo(packageName, PackageInfoFlags.of(flag.toLong()))
    else packageManager?.getPackageInfo(packageName, flag.toInt())
}.getOrNull()

/**
 * 得到 APP 版本号 (兼容 [PackageInfo.getLongVersionCode])
 * @return [Int]
 */
private val PackageInfo.versionCodeCompat get() = PackageInfoCompat.getLongVersionCode(this)

/**
 * 获取系统中全部已安装应用列表
 * @return [List]<[PackageInfo]>
 */
fun Context.listOfPackages() = runCatching {
    @Suppress("DEPRECATION", "KotlinRedundantDiagnosticSuppress")
    if (Build.VERSION.SDK_INT >= 33)
        packageManager?.getInstalledPackages(PackageInfoFlags.of(PackageManager.GET_CONFIGURATIONS.toLong()))
    else packageManager?.getInstalledPackages(PackageManager.GET_CONFIGURATIONS)
}.getOrNull() ?: emptyList()

/**
 * 得到 APP 名称
 * @param packageName APP 包名 - 默认为当前 APP
 * @return [String] 无法获取时返回 ""
 */
fun Context.appNameOf(packageName: String = getPackageName()) =
    getPackageInfoCompat(packageName)?.applicationInfo?.loadLabel(packageManager)?.toString() ?: ""

/**
 * 得到 APP 版本信息与版本号
 * @param packageName APP 包名 - 默认为当前 APP
 * @return [String] 无法获取时返回 ""
 */
fun Context.appVersionBrandOf(packageName: String = getPackageName()) =
    if (appVersionNameOf(packageName).isNotBlank()) "${appVersionNameOf(packageName)}(${appVersionCodeOf(packageName)})" else ""

/**
 * 得到 APP 版本名称
 * @param packageName APP 包名 - 默认为当前 APP
 * @return [String] 无法获取时返回 ""
 */
fun Context.appVersionNameOf(packageName: String = getPackageName()) = getPackageInfoCompat(packageName)?.versionName ?: ""

/**
 * 得到 APP 版本号
 * @param packageName APP 包名 - 默认为当前 APP
 * @return [Long] 无法获取时返回 -1
 */
fun Context.appVersionCodeOf(packageName: String = getPackageName()) = getPackageInfoCompat(packageName)?.versionCodeCompat ?: -1L

/**
 * 得到 APP 目标 SDK 版本
 * @param packageName APP 包名 - 默认为当前 APP
 * @return [Int] 无法获取时返回 -1
 */
fun Context.appTargetSdkOf(packageName: String = getPackageName()) = getPackageInfoCompat(packageName)?.applicationInfo?.targetSdkVersion ?: -1

/**
 * 得到 APP 最低 SDK 版本
 * @param packageName APP 包名 - 默认为当前 APP
 * @return [Int] 无法获取时返回 -1
 */
fun Context.appMinSdkOf(packageName: String = getPackageName()) = getPackageInfoCompat(packageName)?.applicationInfo?.minSdkVersion ?: -1

/**
 * 获取 APP CPU ABI 名称
 * @param packageName APP 包名 - 默认为当前 APP
 * @return [String] 无法获取时返回 ""
 */
fun Context.appCpuAbiOf(packageName: String = getPackageName()) = runCatching {
    ApplicationInfoClass.field { name = "primaryCpuAbi" }.get(getPackageInfoCompat(packageName)?.applicationInfo).string()
}.getOrNull() ?: ""

/**
 * 得到 APP 图标
 * @param packageName APP 包名 - 默认为当前 APP
 * @return [Drawable] 无发获取时返回 [R.drawable.ic_android]
 */
fun Context.appIconOf(packageName: String = getPackageName()) =
    getPackageInfoCompat(packageName)?.applicationInfo?.loadIcon(packageManager) ?: resources.drawableOf(R.drawable.ic_android)

/**
 * 获取 [Serializable] (兼容)
 * @param key 键值名称
 * @return [T] or null
 */
inline fun <reified T : Serializable> Intent.getSerializableExtraCompat(key: String): T? {
    @Suppress("DEPRECATION")
    return if (Build.VERSION.SDK_INT >= 33)
        getSerializableExtra(key, T::class.java)
    else getSerializableExtra(key) as? T?
}

/**
 * [List]<[T]> 转换为 [ArrayList]<[T]>
 * @return [ArrayList]<[T]>
 */
fun <T> List<T>.toArrayList() = toMutableList() as ArrayList<T>

/**
 * 计算与当前时间戳相差的友好时间
 * @param now 刚刚
 * @param second 秒前
 * @param minute 分钟前
 * @param hour 小时前
 * @param day 天前
 * @param month 月前
 * @param year 年前
 * @return [String] 友好时间
 */
fun Long.difference(now: String, second: String, minute: String, hour: String, day: String, month: String, year: String) =
    ((System.currentTimeMillis() - this) / 1000).toInt().let { diff ->
        when (diff) {
            in 0..10 -> now
            in 11..20 -> "10 $second"
            in 21..30 -> "20 $second"
            in 31..40 -> "30 $second"
            in 41..50 -> "40 $second"
            in 51..59 -> "50 $second"
            in 60..3599 -> "${(diff / 60).coerceAtLeast(1)} $minute"
            in 3600..86399 -> "${diff / 3600} $hour"
            in 86400..2591999 -> "${diff / 86400} $day"
            in 2592000..31103999 -> "${diff / 2592000} $month"
            else -> "${diff / 31104000} $year"
        }
    }

/**
 * 保留小数
 * @param count 要保留的位数 - 默认 2 位 - 最多 7 位
 * @return [String] 得到的字符串数字 - 格式化失败返回原始数字的字符串
 */
fun Number.decimal(count: Int = 2) = runCatching {
    DecimalFormat(
        when (count) {
            0 -> "0"
            1 -> "0.0"
            2 -> "0.00"
            3 -> "0.000"
            4 -> "0.0000"
            5 -> "0.00000"
            6 -> "0.000000"
            7 -> "0.0000000"
            else -> "0.0"
        }
    ).apply { roundingMode = RoundingMode.HALF_UP }.format(this) ?: toString()
}.getOrNull() ?: this

/**
 * [Long] 转换为 UTC 时间
 * @return [String]
 */
fun Long.toUtcTime() = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.ROOT).format(Date(this)) ?: ""

/**
 * 弹出 [Toast]
 * @param msg 提示内容
 */
fun Context.toast(msg: String) {
    runCatching {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }.onFailure { YLog.warn(msg) }
}

/**
 * 弹出 [Snackbar]
 * @param msg 提示内容
 * @param actionText 按钮文本 - 不写默认取消按钮
 * @param callback 按钮事件回调
 */
fun Context.snake(msg: String, actionText: String = "", callback: () -> Unit = {}) =
    Snackbar.make((this as Activity).findViewById(android.R.id.content), msg, Snackbar.LENGTH_LONG).apply {
        if (actionText.isBlank()) return@apply
        setActionTextColor(if (isSystemInDarkMode) Color.BLACK else Color.WHITE)
        setAction(actionText) { callback() }
    }.show()


/**
 * 跳转到指定页面
 *
 * [T] 为指定的 [Activity]
 * @param isOutSide 是否从外部启动
 * @param initiate [Intent] 方法体
 */
inline fun <reified T : Activity> Context.navigate(isOutSide: Boolean = false, initiate: Intent.() -> Unit = {}) = runCatching {
    startActivity((if (isOutSide) Intent() else Intent(if (this is Service) applicationContext else this, T::class.java)).apply {
        flags = if (this@navigate !is Activity) Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        else Intent.FLAG_ACTIVITY_NEW_TASK
        if (isOutSide) component = ComponentName(BuildConfig.APPLICATION_ID, T::class.java.name)
        initiate(this)
    })
}.onFailure { toast(msg = "Start ${T::class.java.name} failed") }

/**
 * 跳转 APP 自身设置界面
 * @param packageName 包名
 */
fun Context.openSelfSetting(packageName: String = this.packageName) = runCatching {
    startActivity(Intent().apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
        action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        data = Uri.fromParts("package", packageName, null)
    })
}.onFailure { toast(msg = "Cannot open \"$packageName\"") }

/**
 * 启动系统浏览器
 * @param url 网址
 * @param packageName 指定包名 - 可不填
 */
fun Context.openBrowser(url: String, packageName: String = "") = runCatching {
    startActivity(Intent().apply {
        if (packageName.isNotBlank()) setPackage(packageName)
        action = Intent.ACTION_VIEW
        data = Uri.parse(url)
        /** 防止顶栈一样重叠在自己的 APP 中 */
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    })
}.onFailure {
    if (packageName.isNotBlank()) snake(msg = "Cannot start \"$packageName\"")
    else snake(msg = "Start system browser failed")
}

/**
 * 当前 APP 是否可被启动
 * @param packageName 包名
 */
fun Context.isAppCanOpened(packageName: String = this.packageName) =
    runCatching { packageManager?.getLaunchIntentForPackage(packageName) != null }.getOrNull() ?: false

/**
 * 启动指定 APP
 * @param packageName APP 包名 - 默认为当前 APP
 * @param userId APP 用户 ID - 默认 0
 */
fun Context.openApp(packageName: String = getPackageName(), userId: Int = 0) = runCatching {
    ContextClass.method {
        name = "startActivityAsUser"
        param(IntentClass, UserHandleClass)
    }.get(this).call(packageManager.getLaunchIntentForPackage(packageName), UserHandleClass.method { name = "of" }.get().call(userId))
}.onFailure { toast(msg = "Cannot start \"$packageName\"${if (userId > 0) " for user $userId" else ""}") }


/**
 * 隐藏或显示启动器图标
 *
 * - 你可能需要 LSPosed 的最新版本以开启高版本系统中隐藏 APP 桌面图标功能
 * @param isShow 是否显示
 */
fun Context.hideOrShowLauncherIcon(isShow: Boolean) {
    packageManager?.setComponentEnabledSetting(
        ComponentName(packageName, "${BuildConfig.APPLICATION_ID}.Home"),
        if (isShow) PackageManager.COMPONENT_ENABLED_STATE_DISABLED else PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
        PackageManager.DONT_KILL_APP
    )
}

/**
 * 获取启动器图标状态
 * @return [Boolean] 是否显示
 */
val Context.isLauncherIconShowing
    get() = packageManager?.getComponentEnabledSetting(
        ComponentName(packageName, "${BuildConfig.APPLICATION_ID}.Home")
    ) != PackageManager.COMPONENT_ENABLED_STATE_DISABLED