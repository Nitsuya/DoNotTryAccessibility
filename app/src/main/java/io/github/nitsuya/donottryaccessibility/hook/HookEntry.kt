package io.github.nitsuya.donottryaccessibility.hook

 import com.highcapable.yukihookapi.annotation.xposed.InjectYukiHookWithXposed
import com.highcapable.yukihookapi.hook.factory.configs
import com.highcapable.yukihookapi.hook.factory.encase
import com.highcapable.yukihookapi.hook.xposed.proxy.IYukiHookXposedInit
import io.github.nitsuya.donottryaccessibility.data.ConfigData

@InjectYukiHookWithXposed(entryClassName = "DoNotTryAccessibility", isUsingResourcesHook = false)
class HookEntry : IYukiHookXposedInit {

    override fun onInit() = configs {
        debugLog {
            tag = "DoNotTryAccessibility"
            elements(TAG, PRIORITY)
        }
        isDebug = false
    }

    override fun onHook() = encase {
        loadSystem {
            ConfigData.init(this)

            loadHooker(AndroidFrameworkHooker)

        }

    }

}