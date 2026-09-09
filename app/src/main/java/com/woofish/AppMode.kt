package com.woofish

enum class AppMode(
    val id: String,
    val displayName: String,
    val defaultSubtitle: String,
    val soundNames: List<String>,
    val iconResId: Int
) {
    WOODEN_FISH(
        id = "wooden_fish",
        displayName = "木鱼模式",
        defaultSubtitle = "正念",
        soundNames = listOf("禅韵木鱼", "沉厚木鱼", "清脆木鱼"),
        iconResId = R.drawable.ic_wooden_fish
    ),
    METRONOME(
        id = "metronome",
        displayName = "节拍器模式",
        defaultSubtitle = "节拍",
        soundNames = listOf("机械节拍", "木质节拍"),
        iconResId = R.drawable.ic_metronome
    ),
    DRUM(
        id = "drum",
        displayName = "电子鼓模式",
        defaultSubtitle = "律动",
        soundNames = listOf("低音鼓", "手鼓", "非洲鼓"),
        iconResId = R.drawable.ic_drum
    ),
    EVENT(
        id = "event",
        displayName = "活动模式",
        defaultSubtitle = "加油",
        soundNames = listOf("整齐拍掌", "集体喊加油", "集体怒吼"),
        iconResId = R.drawable.ic_event
    );

    fun getTempoPresets(): List<Pair<String, Int>> = when (this) {
        WOODEN_FISH -> listOf(
            "禅修 30" to 30,
            "沉静 60" to 60,
            "舒缓 90" to 90,
            "诵经 120" to 120,
            "精进 150" to 150
        )
        METRONOME -> listOf(
            "广板 30" to 30,
            "慢板 60" to 60,
            "行板 90" to 90,
            "快板 120" to 120,
            "急板 150" to 150
        )
        DRUM -> listOf(
            "慢摇 30" to 30,
            "柔和 60" to 60,
            "律动 90" to 90,
            "动感 120" to 120,
            "狂热 150" to 150
        )
        EVENT -> listOf(
            "慢热 30" to 30,
            "齐声 60" to 60,
            "助威 90" to 90,
            "呐喊 120" to 120,
            "狂欢 150" to 150
        )
    }

    companion object {
        fun fromId(id: String): AppMode = entries.firstOrNull { it.id == id } ?: WOODEN_FISH
    }
}
