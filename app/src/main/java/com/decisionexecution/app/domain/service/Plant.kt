package com.decisionexecution.app.domain.service

/** Grows with level, so it only ever gets bigger */
enum class PlantStage(val label: String, val fromLevel: Int) {
    SEED("Seed", 1),
    SPROUT("Sprout", 3),
    SAPLING("Sapling", 5),
    BLOOM("In bloom", 8),
    TREE("Little tree", 12)
}

/** Follows recent activity, so it perks up again as soon as you do something */
enum class PlantHealth { THRIVING, OKAY, THIRSTY, WILTING }

data class PlantState(
    val stage: PlantStage,
    val health: PlantHealth,
    val daysSinceActive: Int,
    val variety: PlantVariety = PlantVariety.BLOSSOM
) {
    val message: String
        get() = when (health) {
            PlantHealth.THRIVING -> when (stage) {
                PlantStage.SEED -> "Something's stirring. Keep going!"
                PlantStage.TREE -> "Look at us. A whole tree!"
                else -> "Soaking up all that progress"
            }
            PlantHealth.OKAY -> "Doing fine. A little win today would help"
            PlantHealth.THIRSTY -> "Getting thirsty… finish something to water me"
            PlantHealth.WILTING -> "I've been waiting $daysSinceActive days. Even one task helps"
        }
}

fun plantStageFor(level: Int): PlantStage = PlantStage.entries.last { level >= it.fromLevel }

fun plantHealthFor(daysSinceActive: Int): PlantHealth = when {
    daysSinceActive <= 0 -> PlantHealth.THRIVING
    daysSinceActive == 1 -> PlantHealth.OKAY
    daysSinceActive <= 3 -> PlantHealth.THIRSTY
    else -> PlantHealth.WILTING
}

/** A variety the user picked but hasn't unlocked (say, after an undo cost a level) falls back to the default */
fun plantFor(level: Int, daysSinceActive: Int, variety: PlantVariety = PlantVariety.BLOSSOM): PlantState =
    PlantState(
        plantStageFor(level),
        plantHealthFor(daysSinceActive),
        daysSinceActive,
        variety.takeIf { it.isUnlocked(level) } ?: PlantVariety.BLOSSOM
    )
