package com.woofish

import org.jetbrains.compose.resources.DrawableResource
import woofish.shared.generated.resources.Res
import woofish.shared.generated.resources.ic_drum
import woofish.shared.generated.resources.ic_metronome
import woofish.shared.generated.resources.ic_pomodoro
import woofish.shared.generated.resources.ic_wooden_fish

enum class AppMode(
    val id: String,
    val displayName: String,
    val defaultSubtitle: String,
    val soundNames: List<String>,
    val icon: DrawableResource
) {
    WOODEN_FISH(
        id = "wooden_fish",
        displayName = "木鱼模式",
        defaultSubtitle = "正念",
        soundNames = listOf("禅韵木鱼", "沉厚木鱼", "清脆木鱼"),
        icon = Res.drawable.ic_wooden_fish
    ),
    METRONOME(
        id = "metronome",
        displayName = "节拍器模式",
        defaultSubtitle = "节拍",
        soundNames = listOf("机械节拍", "木质节拍", "真实心跳"),
        icon = Res.drawable.ic_metronome
    ),
    DRUM(
        id = "drum",
        displayName = "电子鼓模式",
        defaultSubtitle = "律动",
        soundNames = listOf("低音鼓", "手鼓", "非洲鼓"),
        icon = Res.drawable.ic_drum
    ),
    POMODORO(
        id = "pomodoro",
        displayName = "番茄钟模式",
        defaultSubtitle = "专注",
        soundNames = listOf("滴答音"),
        icon = Res.drawable.ic_pomodoro
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
        POMODORO -> listOf(
            "2分钟" to 2,
            "5分钟" to 5,
            "10分钟" to 10,
            "25+5分钟" to 25,
            "50+10分钟" to 50
        )
    }

    companion object {
        fun fromId(id: String): AppMode = entries.firstOrNull { it.id == id } ?: WOODEN_FISH
    }
}
