package io.github.nitsuya.donottryaccessibility.data

import android.content.Context
import android.util.Xml
import com.highcapable.yukihookapi.hook.log.YLog
import com.highcapable.yukihookapi.hook.param.PackageParam
import io.github.nitsuya.donottryaccessibility.BuildConfig
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object ConfigData {
    private const val CONFIG_FILENAME = "config.xml"
    private const val BLOCK_APPS = "_block_apps"

    private var configFile: File? = null
    private val dataMap = mutableMapOf<String, HashSet<String>>()

    val blockApps by lazy { PrefsDataSetString(BLOCK_APPS, hashSetOf(BuildConfig.APPLICATION_ID)) }

    fun refresh() = blockApps.refresh()

    private var instance: Any? = null

    fun init(instance: Any) {
        this.instance = instance
        when (instance) {
            is Context -> {
                configFile = File(instance.filesDir, CONFIG_FILENAME)
                loadConfig()
            }
            is PackageParam -> {} // Do nothing for PackageParam as it's not used for file operations
            else -> error("Unknown type for init ConfigData")
        }
    }

    private fun loadConfig() {
        if (configFile?.exists() != true) return
        try {
            FileInputStream(configFile!!).use { input ->
                val parser = Xml.newPullParser()
                parser.setInput(input, "UTF-8")
                var eventType = parser.eventType
                var currentSetName: String? = null

                while (eventType != XmlPullParser.END_DOCUMENT) {
                    when (eventType) {
                        XmlPullParser.START_TAG -> {
                            if (parser.name == "set") {
                                currentSetName = parser.getAttributeValue(null, "name")
                                currentSetName?.let { dataMap[it] = hashSetOf() }
                            } else if (parser.name == "string" && currentSetName != null) {
                                parser.next()
                                if (parser.eventType == XmlPullParser.TEXT) {
                                    dataMap[currentSetName]?.add(parser.text)
                                }
                            }
                        }
                        XmlPullParser.END_TAG -> {
                            if (parser.name == "set") currentSetName = null
                        }
                    }
                    eventType = parser.next()
                }
            }
        } catch (e: Exception) {
            YLog.error("Failed to load config: ${e.message}")
        }
    }

    private fun saveConfig() {
        try {
            FileOutputStream(configFile!!).use { output ->
                val serializer = Xml.newSerializer()
                serializer.setOutput(output, "UTF-8")
                serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true)
                serializer.startDocument("UTF-8", true)
                serializer.startTag("", "map")

                dataMap.forEach { (key, value) ->
                    serializer.startTag("", "set")
                    serializer.attribute("", "name", key)
                    value.forEach { item ->
                        serializer.startTag("", "string")
                        serializer.text(item)
                        serializer.endTag("", "string")
                    }
                    serializer.endTag("", "set")
                }

                serializer.endTag("", "map")
                serializer.endDocument()
            }
        } catch (e: Exception) {
            YLog.error("Failed to save config: ${e.message}")
        }
    }

    internal fun getStringSet(key: String, defaultValue: Set<String> = hashSetOf()): Set<String> =
        when (instance) {
            is Context -> dataMap[key] ?: defaultValue
            is PackageParam -> (instance as PackageParam).prefs.getStringSet(key, defaultValue)
            else -> defaultValue
        }

    internal fun putStringSet(key: String, value: Set<String>) {
        when (instance) {
            is Context -> {
                dataMap[key] = HashSet(value)
                saveConfig()
            }
            is PackageParam -> YLog.warn("Not support for this method in Xposed environment")
            else -> error("Unknown type for put data")
        }
    }

    data class PrefsDataSetString(
        private val key: String,
        private var data: HashSet<String> = hashSetOf()
    ) {
        init { refresh() }

        internal fun refresh() {
            data = getStringSet(key, data).toHashSet()
        }

        internal fun callRefresh() {
            when (instance) {
                is Context -> refresh()
                is PackageParam -> YLog.warn("Not support for this method")
                else -> error("Unknown type for get prefs data")
            }
        }

        fun contains(element: String) = data.contains(element)

        fun add(element: String) {
            if (data.add(element)) {
                putStringSet(key, data)
                callRefresh()
            }
        }

        fun remove(element: String) {
            if (data.remove(element)) {
                putStringSet(key, data)
                callRefresh()
            }
        }

        fun switch(element: String) {
            if (!contains(element)) add(element) else remove(element)
        }
    }
}
