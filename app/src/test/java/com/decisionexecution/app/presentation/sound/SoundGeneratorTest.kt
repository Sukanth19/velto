package com.decisionexecution.app.presentation.sound

import com.decisionexecution.app.domain.model.FocusSound
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.doubles.shouldBeBetween
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.random.Random

class SoundGeneratorTest : StringSpec({

    /** A minute of audio, in the 0.1 s chunks the player uses */
    fun minuteOf(sound: FocusSound): ShortArray {
        val generator = SoundGenerator(sound, random = Random(7))
        val chunk = ShortArray(SoundGenerator.SAMPLE_RATE / 10)
        return ShortArray(0).let { _ ->
            val all = ShortArray(SoundGenerator.SAMPLE_RATE * 60)
            for (start in all.indices step chunk.size) {
                generator.fill(chunk)
                chunk.copyInto(all, start, 0, minOf(chunk.size, all.size - start))
            }
            all
        }
    }

    fun rms(samples: ShortArray) = sqrt(samples.sumOf { (it / 32768.0).let { s -> s * s } } / samples.size)

    "off is silence" {
        minuteOf(FocusSound.OFF).all { it == 0.toShort() } shouldBe true
    }

    "every sound is audible but comfortably below clipping" {
        listOf(FocusSound.BROWN, FocusSound.RAIN, FocusSound.CAFE).forEach { sound ->
            val samples = minuteOf(sound)
            rms(samples).shouldBeBetween(0.03, 0.35, 0.0)
            // Hardly any samples pinned at the limit
            samples.count { abs(it.toInt()) >= 32000 }.toDouble().shouldBeBetween(0.0, samples.size * 0.001, 0.0)
        }
    }

    "brown noise is smooth; rain has more high-frequency hiss" {
        // Mean absolute difference between neighbouring samples, relative to level: a rough brightness measure
        fun brightness(samples: ShortArray) =
            (1 until samples.size).sumOf { abs(samples[it] - samples[it - 1]).toDouble() } / samples.size / (rms(samples) * 32768)
        val brown = brightness(minuteOf(FocusSound.BROWN))
        val rain = brightness(minuteOf(FocusSound.RAIN))
        (rain > brown * 2) shouldBe true
        minuteOf(FocusSound.CAFE).toList() shouldNotBe minuteOf(FocusSound.RAIN).toList()
    }
})
