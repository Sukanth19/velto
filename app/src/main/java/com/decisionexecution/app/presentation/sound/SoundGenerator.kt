package com.decisionexecution.app.presentation.sound

import com.decisionexecution.app.domain.model.FocusSound
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * Makes endless background sound from noise and a little maths, so there are no audio files to
 * ship or download. Every call to [fill] continues where the last one stopped, so buffers join
 * up without clicks.
 */
class SoundGenerator(
    private val sound: FocusSound,
    private val sampleRate: Int = SAMPLE_RATE,
    private val random: Random = Random.Default
) {
    // Brown noise: white noise run through a leaky integrator
    private var brown = 0.0

    // Pink noise (Paul Kellet's economy filter), the soft body of rain
    private var p0 = 0.0
    private var p1 = 0.0
    private var p2 = 0.0

    // Raindrops: short bursts of filtered noise that die away
    private var dropLevel = 0.0
    private var dropLowpass = 0.0
    private val dropDecay = exp(-1.0 / (0.012 * sampleRate))

    // Café chatter: noise through a resonant band-pass whose centre wanders like voices
    private var bpLow = 0.0
    private var bpBand = 0.0
    private var chatterPhase = 0.0
    private var chatterCentre = 500.0
    private var chatterTarget = 500.0
    private var swellPhase = 0.0

    // Cups and spoons: a high ping now and then
    private var clinkLevel = 0.0
    private var clinkPhase = 0.0
    private var clinkFreq = 3000.0
    private val clinkDecay = exp(-1.0 / (0.09 * sampleRate))

    /** Fills [buffer] with 16-bit mono samples */
    fun fill(buffer: ShortArray) {
        for (i in buffer.indices) {
            val sample = when (sound) {
                FocusSound.OFF -> 0.0
                FocusSound.BROWN -> nextBrown() * 0.9
                FocusSound.RAIN -> nextRain()
                FocusSound.CAFE -> nextCafe()
            }
            buffer[i] = (sample.coerceIn(-1.0, 1.0) * Short.MAX_VALUE * LEVEL).toInt().toShort()
        }
    }

    private fun white() = random.nextDouble(-1.0, 1.0)

    private fun nextBrown(): Double {
        brown = (brown + 0.02 * white()) / 1.02
        return brown * 3.5
    }

    private fun nextPink(): Double {
        val w = white()
        p0 = 0.99765 * p0 + w * 0.0990460
        p1 = 0.96300 * p1 + w * 0.2965164
        p2 = 0.57000 * p2 + w * 1.0526913
        return (p0 + p1 + p2 + w * 0.1848) * 0.18
    }

    private fun nextRain(): Double {
        // Around 40 drops a second, each a different size
        if (random.nextDouble() < 40.0 / sampleRate) dropLevel = random.nextDouble(0.15, 0.6)
        dropLevel *= dropDecay
        dropLowpass += 0.35 * (white() - dropLowpass)
        return nextPink() * 0.55 + nextBrown() * 0.35 + dropLowpass * dropLevel
    }

    private fun nextCafe(): Double {
        // Wander the chatter's pitch towards a new target every so often
        if (random.nextDouble() < 3.0 / sampleRate) chatterTarget = random.nextDouble(300.0, 900.0)
        chatterCentre += (chatterTarget - chatterCentre) * 0.0005
        // State-variable band-pass, a gentle Q so it reads as murmur, not a whistle
        val f = 2 * sin(PI * chatterCentre / sampleRate)
        val q = 0.6
        bpLow += f * bpBand
        val high = white() - bpLow - q * bpBand
        bpBand += f * high
        // Syllable-like flutter, and slower swells as the room gets louder and quieter
        chatterPhase += 2 * PI * 4.0 / sampleRate
        swellPhase += 2 * PI * 0.07 / sampleRate
        val flutter = 0.6 + 0.4 * sin(chatterPhase + sin(chatterPhase * 0.37) * 2)
        val swell = 0.75 + 0.25 * sin(swellPhase)
        val murmur = bpBand * flutter * swell * 0.9

        if (random.nextDouble() < 0.2 / sampleRate) {
            clinkLevel = random.nextDouble(0.08, 0.2)
            clinkFreq = random.nextDouble(2500.0, 4500.0)
        }
        clinkLevel *= clinkDecay
        clinkPhase += 2 * PI * clinkFreq / sampleRate
        if (clinkPhase > 2 * PI) clinkPhase -= 2 * PI
        val clink = sin(clinkPhase) * clinkLevel

        return murmur + nextBrown() * 0.35 + clink
    }

    companion object {
        const val SAMPLE_RATE = 22_050
        /** Headroom so peaks never clip; the system volume does the rest */
        private const val LEVEL = 0.7
    }
}
