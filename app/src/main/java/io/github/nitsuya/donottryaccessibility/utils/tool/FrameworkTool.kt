/*
 * This file is created by fankes on 2022/5/12.
 */
package io.github.nitsuya.donottryaccessibility.utils.tool

import android.content.Context
import com.highcapable.yukihookapi.hook.factory.dataChannel
import com.highcapable.yukihookapi.hook.param.PackageParam
import com.highcapable.yukihookapi.hook.xposed.channel.data.ChannelData
import io.github.nitsuya.donottryaccessibility.bean.AppFiltersBean
import io.github.nitsuya.donottryaccessibility.bean.AppInfoBean
import io.github.nitsuya.donottryaccessibility.utils.PackageName


object FrameworkTool {

    private const val CALL_REFRESH_HOST_PREFS_DATA = "call_refresh_host_prefs_data"

    private val CALL_APP_LIST_DATA_GET_RESULT = ChannelData<List<AppInfoBean>>("call_app_info_list_data_get_result")
    private val CALL_APP_LIST_DATA_GET = ChannelData<AppFiltersBean>("call_app_info_list_data_get")

    object Host {

        private var instance: PackageParam? = null

        fun with(instance: PackageParam, initiate: Host.() -> Unit) = apply { this.instance = instance }.apply(initiate)

        fun onRefreshFrameworkPrefsData(callback: () -> Unit) = instance?.dataChannel?.wait(CALL_REFRESH_HOST_PREFS_DATA) { callback() }

        fun onPushAppListData(result: (AppFiltersBean) -> List<AppInfoBean>) {
            instance?.dataChannel?.with { wait(CALL_APP_LIST_DATA_GET) { put(CALL_APP_LIST_DATA_GET_RESULT, result(it)) } }
        }
    }

    fun checkingActivated(context: Context, result: (Boolean) -> Unit) = context.dataChannel(PackageName.SYSTEM_FRAMEWORK).checkingVersionEquals(result = result)

    fun refreshFrameworkPrefsData(context: Context) = context.dataChannel(PackageName.SYSTEM_FRAMEWORK).put(CALL_REFRESH_HOST_PREFS_DATA)

    fun fetchAppListData(context: Context, appFilters: AppFiltersBean, result: (List<AppInfoBean>) -> Unit) {
        context.dataChannel(PackageName.SYSTEM_FRAMEWORK).with {
            wait(CALL_APP_LIST_DATA_GET_RESULT) { result(it) }
            put(CALL_APP_LIST_DATA_GET, appFilters)
        }
    }
}