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
        soundNames = listOf("木鱼原声", "清脆木鱼"),
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

    companion object {
        fun fromId(id: String): AppMode = entries.firstOrNull { it.id == id } ?: WOODEN_FISH
    }
}
