package com.woofish

interface IPreferences {
    fun getString(key: String, def: String?): String?
    fun putString(key: String, value: String?)
    fun getInt(key: String, def: Int): Int
    fun putInt(key: String, value: Int)
    fun getLong(key: String, def: Long): Long
    fun putLong(key: String, value: Long)
    fun getFloat(key: String, def: Float): Float
    fun putFloat(key: String, value: Float)
    fun getBoolean(key: String, def: Boolean): Boolean
    fun putBoolean(key: String, value: Boolean)
    fun remove(key: String)
}
