package io.github.nitsuya.donottryaccessibility.application

import androidx.appcompat.app.AppCompatDelegate
import com.highcapable.yukihookapi.hook.xposed.application.ModuleApplication
import io.github.nitsuya.donottryaccessibility.data.ConfigData
import io.github.nitsuya.donottryaccessibility.utils.factory.locale
import io.github.nitsuya.donottryaccessibility.generated.locale.AppLocale

class DefaultApplication : ModuleApplication() {

    override fun onCreate() {
        super.onCreate()
        locale = AppLocale.attach(this)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        ConfigData.init(this)
    }
}