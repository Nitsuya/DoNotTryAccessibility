package io.github.nitsuya.donottryaccessibility.ui.activity

import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import io.github.nitsuya.donottryaccessibility.bean.AppFiltersBean
import io.github.nitsuya.donottryaccessibility.bean.AppFiltersType
import io.github.nitsuya.donottryaccessibility.bean.AppInfoBean
import io.github.nitsuya.donottryaccessibility.data.ConfigData
import io.github.nitsuya.donottryaccessibility.databinding.ActivityAppsConfigBinding
import io.github.nitsuya.donottryaccessibility.databinding.AdapterAppInfoBinding
import io.github.nitsuya.donottryaccessibility.databinding.DiaAppsFilterBinding
import io.github.nitsuya.donottryaccessibility.ui.activity.base.BaseActivity
import io.github.nitsuya.donottryaccessibility.utils.factory.appIconOf
import io.github.nitsuya.donottryaccessibility.utils.factory.bindAdapter
import io.github.nitsuya.donottryaccessibility.utils.factory.locale

import io.github.nitsuya.donottryaccessibility.utils.factory.showDialog
import io.github.nitsuya.donottryaccessibility.utils.tool.FrameworkTool
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppsConfigActivity : BaseActivity<ActivityAppsConfigBinding>() {

    private var appFilters = AppFiltersBean()
    private var notifyDataSetChanged: (() -> Unit)? = null
    private val listData = ArrayList<AppInfoBean>()

    override fun onCreate() {
        binding.titleBackIcon.setOnClickListener { finish() }
        binding.filterIcon.setOnClickListener {
            showDialog<DiaAppsFilterBinding> {
                title = locale.filterByCondition
                binding.filtersRadioUser.isChecked = appFilters.type == AppFiltersType.USER
                binding.filtersRadioSystem.isChecked = appFilters.type == AppFiltersType.SYSTEM
                binding.filtersRadioAll.isChecked = appFilters.type == AppFiltersType.ALL
                binding.appFiltersEdit.apply {
                    requestFocus()
                    invalidate()
                    if (appFilters.name.isNotBlank()) {
                        setText(appFilters.name)
                        setSelection(appFilters.name.length)
                    }
                }
                /** 设置 [AppFiltersBean.type] */
                fun setAppFiltersType() {
                    appFilters.type = when {
                        binding.filtersRadioUser.isChecked -> AppFiltersType.USER
                        binding.filtersRadioSystem.isChecked -> AppFiltersType.SYSTEM
                        binding.filtersRadioAll.isChecked -> AppFiltersType.ALL
                        else -> error("Invalid app filters type")
                    }
                }
                confirmButton {
                    setAppFiltersType()
                    appFilters.name = binding.appFiltersEdit.text.toString().trim()
                    refreshData()
                }
                cancelButton()
                if (appFilters.name.isNotBlank())
                    neutralButton(locale.clearFilters) {
                        setAppFiltersType()
                        appFilters.name = ""
                        refreshData()
                    }
            }
        }
        binding.listView.apply {
            bindAdapter {
                onBindDatas { listData }
                onBindViews<AdapterAppInfoBinding> { binding, position ->
                    listData[position].also { bean ->
                        binding.appIcon.setImageDrawable(bean.icon)
                        binding.appNameText.text = bean.name
                        binding.pkgNameText.text = bean.packageName
                        binding.appCheck.isChecked = ConfigData.blockApps.contains(bean.packageName)
                    }
                }
            }.apply {
                notifyDataSetChanged = this::notifyDataSetChanged
            }
            setOnItemClickListener { _, _, position, _ ->
                listData[position].also { bean ->
                    ConfigData.blockApps.switch(bean.packageName)
                    notifyDataSetChanged?.invoke()
                }
            }
        }
        refreshData()
    }

    /** 刷新列表数据 */
    private fun refreshData() {
        binding.listProgressView.isVisible = true
        binding.filterIcon.isVisible = false
        binding.listView.isVisible = false
        binding.listNoDataView.isVisible = false
        binding.titleCountText.text = locale.loading
        FrameworkTool.fetchAppListData(context = this, appFilters) {
            lifecycleScope.launch(Dispatchers.IO){
                val tempsData = ArrayList<AppInfoBean>()
                runCatching {
                    it.takeIf { e -> e.isNotEmpty() }?.forEach { e ->
                        e.icon = appIconOf(e.packageName)
                        tempsData.add(e)
                    }
                }
                tempsData.sortBy { bean ->
                    !ConfigData.blockApps.contains(bean.packageName)
                }
                withContext(Dispatchers.Main){
                    listData.clear()
                    listData.addAll(tempsData)
                    notifyDataSetChanged?.invoke()
                    binding.listView.post { binding.listView.setSelection(0) }
                    binding.listProgressView.isVisible = false
                    binding.filterIcon.isVisible = true
                    binding.listView.isVisible = listData.isNotEmpty()
                    binding.listNoDataView.isVisible = listData.isEmpty()
                    binding.titleCountText.text = locale.resultCount(listData.size)
                }
            }
        }
    }

}