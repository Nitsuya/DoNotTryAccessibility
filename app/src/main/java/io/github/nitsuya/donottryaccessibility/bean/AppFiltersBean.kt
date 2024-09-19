/*
 * This file is created by fankes on 2022/6/4.
 */
package io.github.nitsuya.donottryaccessibility.bean

import java.io.Serializable

data class AppFiltersBean(
    var name: String = "",
    var type: AppFiltersType = AppFiltersType.USER
) : Serializable