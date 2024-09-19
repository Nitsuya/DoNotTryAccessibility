/*
 * This file is created by fankes on 2022/6/3.
 */
package io.github.nitsuya.donottryaccessibility.utils.factory

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ListView
import androidx.viewbinding.ViewBinding
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.type.android.LayoutInflaterClass

/**
 * 绑定 [BaseAdapter] 到 [ListView]
 * @param initiate 方法体
 * @return [BaseAdapter]
 */
inline fun ListView.bindAdapter(initiate: BaseAdapterCreater.() -> Unit) =
    BaseAdapterCreater(context).apply(initiate).baseAdapter?.apply { adapter = this } ?: error("BaseAdapter not binded")

/**
 * [BaseAdapter] 创建类
 * @param context 实例
 */
class BaseAdapterCreater(val context: Context) {

    /** 当前 [List] 回调 */
    var listDataCallback: (() -> List<*>)? = null

    /** 当前 [BaseAdapter] */
    var baseAdapter: BaseAdapter? = null

    /**
     * 绑定 [List] 到 [ListView]
     * @param result 回调数据
     */
    fun onBindDatas(result: (() -> List<*>)) {
        listDataCallback = result
    }

    /**
     * 绑定 [BaseAdapter] 到 [ListView]
     * @param bindViews 回调 - ([VB] 每项,[Int] 下标)
     */
    inline fun <reified VB : ViewBinding> onBindViews(crossinline bindViews: (binding: VB, position: Int) -> Unit) {
        baseAdapter = object : BaseAdapter() {
            val methodInflate = VB::class.java.method {
                name = "inflate"
                param(LayoutInflaterClass)
            }.get()
            override fun getCount() = listDataCallback?.let { it() }?.size ?: 0
            override fun getItem(position: Int) = listDataCallback?.let { it() }?.get(position)
            override fun getItemId(position: Int) = position.toLong()
            override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
                var holderView = convertView
                val holder: VB
                if (convertView == null) {
                    holder = methodInflate.invoke<VB>(LayoutInflater.from(context)) ?: error("ViewHolder binding failed")
                    holder.root.tag = holder
                    holderView = holder.root
                } else {
                    holder = convertView.tag as VB
                }
                bindViews(holder, position)
                return holderView ?: error("ViewHolder binding failed")
            }
        }
    }
}