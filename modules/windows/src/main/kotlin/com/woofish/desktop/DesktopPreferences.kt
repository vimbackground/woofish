package com.woofish.desktop

import com.woofish.IPreferences
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Properties

class DesktopPreferences : IPreferences {
    private val properties = Properties()
    private val file: File

    init {
        val appData = System.getenv("APPDATA")
        val dir = if (!appData.isNullOrBlank()) {
            File(appData, "woofish")
        } else {
            File(System.getProperty("user.home"), ".woofish")
        }
        if (!dir.exists()) {
            dir.mkdirs()
        }
        file = File(dir, "preferences.properties")
        if (file.exists()) {
            try {
                FileInputStream(file).use { properties.load(it) }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    @Synchronized
    private fun save() {
        try {
            FileOutputStream(file).use { properties.store(it, "woofish preferences") }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun getString(key: String, def: String?): String? = properties.getProperty(key, def)

    override fun putString(key: String, value: String?) {
        if (value != null) properties.setProperty(key, value) else properties.remove(key)
        save()
    }

    override fun getInt(key: String, def: Int): Int = properties.getProperty(key)?.toIntOrNull() ?: def

    override fun putInt(key: String, value: Int) {
        properties.setProperty(key, value.toString())
        save()
    }

    override fun getLong(key: String, def: Long): Long = properties.getProperty(key)?.toLongOrNull() ?: def

    override fun putLong(key: String, value: Long) {
        properties.setProperty(key, value.toString())
        save()
    }

    override fun getFloat(key: String, def: Float): Float = properties.getProperty(key)?.toFloatOrNull() ?: def

    override fun putFloat(key: String, value: Float) {
        properties.setProperty(key, value.toString())
        save()
    }

    override fun getBoolean(key: String, def: Boolean): Boolean = properties.getProperty(key)?.toBooleanStrictOrNull() ?: def

    override fun putBoolean(key: String, value: Boolean) {
        properties.setProperty(key, value.toString())
        save()
    }

    override fun remove(key: String) {
        properties.remove(key)
        save()
    }
}
