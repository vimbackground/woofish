package com.woofish

enum class AppLanguage(val id: String, val displayNameZh: String, val displayNameEn: String) {
    SYSTEM("system", "跟随系统", "System"),
    ZH("zh", "简体中文", "Simplified Chinese"),
    EN("en", "English", "English");

    fun getDisplayName(isZh: Boolean): String = if (isZh) displayNameZh else displayNameEn

    companion object {
        fun fromId(id: String?): AppLanguage = entries.firstOrNull { it.id == id } ?: SYSTEM
    }
}
