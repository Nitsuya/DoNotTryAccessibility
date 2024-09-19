/*
 * This file is created by fankes on 2022/6/8.
 */
package io.github.nitsuya.donottryaccessibility.bean

import android.graphics.drawable.Drawable
import java.io.Serializable

data class AppInfoBean(
    var icon: Drawable? = null,
    var name: String,
    var packageName: String
) : Serializable