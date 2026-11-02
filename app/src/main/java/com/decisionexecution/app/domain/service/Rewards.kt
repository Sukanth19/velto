package com.decisionexecution.app.domain.service

import com.decisionexecution.app.domain.model.ThemePalette

/** Kinds of companion plant. New ones unlock as you level up. */
enum class PlantVariety(val label: String, val unlockLevel: Int) {
    BLOSSOM("Blossom", 1),
    SUNFLOWER("Sunflower", 4),
    CACTUS("Cactus", 6),
    BONSAI("Bonsai", 9)
}

/**
 * The level each palette unlocks at. Wallpaper and Violet are there from the start; the rest come
 * one every level or two, interleaved with the plants, so most level-ups bring something new.
 */
val ThemePalette.unlockLevel: Int
    get() = when (this) {
        ThemePalette.WALLPAPER, ThemePalette.VIOLET -> 1
        ThemePalette.OCEAN -> 2
        ThemePalette.FOREST -> 3
        ThemePalette.SUNSET -> 5
        ThemePalette.ROSE -> 7
        ThemePalette.MONO -> 10
    }

fun ThemePalette.isUnlocked(level: Int): Boolean = level >= unlockLevel
fun PlantVariety.isUnlocked(level: Int): Boolean = level >= unlockLevel

/** A reward earned at a level, as shown when celebrating */
data class Reward(val level: Int, val description: String)

private val paletteNames = mapOf(
    ThemePalette.OCEAN to "Ocean",
    ThemePalette.FOREST to "Forest",
    ThemePalette.SUNSET to "Sunset",
    ThemePalette.ROSE to "Rose",
    ThemePalette.MONO to "Graphite"
)

val ALL_REWARDS: List<Reward> = (
    ThemePalette.entries.filter { it.unlockLevel > 1 }.map { Reward(it.unlockLevel, "${paletteNames.getValue(it)} colours") } +
        PlantVariety.entries.filter { it.unlockLevel > 1 }.map { Reward(it.unlockLevel, "${it.label} plant") }
    ).sortedBy { it.level }

/** Rewards for reaching any level above [fromLevel] up to and including [toLevel] */
fun rewardsBetween(fromLevel: Int, toLevel: Int): List<Reward> = ALL_REWARDS.filter { it.level in (fromLevel + 1)..toLevel }

/** The next thing to look forward to, or null once everything is unlocked */
fun nextReward(level: Int): Reward? = ALL_REWARDS.firstOrNull { it.level > level }
