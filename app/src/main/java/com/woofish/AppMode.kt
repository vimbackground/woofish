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
        soundNames = listOf("沉厚木鱼", "清脆木鱼", "禅韵木鱼"),
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
        soundNames = listOf("低音鼓", "中式大鼓", "非洲鼓"),
        iconResId = R.drawable.ic_drum
    );

    fun getTempoPresets(): List<Pair<String, Int>> = when (this) {
        WOODEN_FISH -> listOf(
            "禅修 30" to 30,
            "沉静 60" to 60,
            "舒缓 90" to 90,
            "适中 120" to 120,
            "清心 150" to 150
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
    }

    companion object {
        fun fromId(id: String): AppMode = entries.firstOrNull { it.id == id } ?: METRONOME
    }
}
