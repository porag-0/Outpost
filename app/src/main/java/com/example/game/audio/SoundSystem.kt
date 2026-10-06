package com.example.game.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

class SoundSystem {

    private val scope = CoroutineScope(Dispatchers.Default)
    private val sampleRate = 22050

    fun playRadioChirp() {
        scope.launch {
            playToneSequence(
                listOf(
                    Pair(1200f, 40),
                    Pair(1800f, 40),
                    Pair(2400f, 60)
                ), volume = 0.5f
            )
        }
    }

    fun playScannerPing() {
        scope.launch {
            playToneSequence(
                listOf(
                    Pair(880f, 60),
                    Pair(1320f, 100)
                ), volume = 0.45f
            )
        }
    }

    fun playGeigerClick() {
        scope.launch {
            val numSamples = (sampleRate * 0.015f).toInt()
            val samples = ShortArray(numSamples)
            for (i in 0 until numSamples) {
                val noise = ((Math.random() * 2.0 - 1.0) * Short.MAX_VALUE * 0.4).toInt()
                samples[i] = noise.toShort()
            }
            playPcmBuffer(samples)
        }
    }

    fun playAirlockHiss() {
        scope.launch {
            val durationSec = 0.4f
            val numSamples = (sampleRate * durationSec).toInt()
            val samples = ShortArray(numSamples)
            for (i in 0 until numSamples) {
                val envelope = 1f - (i.toFloat() / numSamples)
                val noise = ((Math.random() * 2.0 - 1.0) * Short.MAX_VALUE * 0.25f * envelope).toInt()
                samples[i] = noise.toShort()
            }
            playPcmBuffer(samples)
        }
    }

    fun playAlarmSiren() {
        scope.launch {
            playToneSequence(
                listOf(
                    Pair(950f, 120),
                    Pair(650f, 120),
                    Pair(950f, 120),
                    Pair(650f, 120)
                ), volume = 0.6f
            )
        }
    }

    fun playFootstep() {
        scope.launch {
            val numSamples = (sampleRate * 0.04f).toInt()
            val samples = ShortArray(numSamples)
            for (i in 0 until numSamples) {
                val env = 1f - (i.toFloat() / numSamples)
                val noise = ((Math.random() * 2.0 - 1.0) * Short.MAX_VALUE * 0.15f * env).toInt()
                samples[i] = noise.toShort()
            }
            playPcmBuffer(samples)
        }
    }

    fun playSuccessFanfare() {
        scope.launch {
            playToneSequence(
                listOf(
                    Pair(523.25f, 90), // C5
                    Pair(659.25f, 90), // E5
                    Pair(783.99f, 90), // G5
                    Pair(1046.50f, 220) // C6
                ), volume = 0.65f
            )
        }
    }

    private fun playToneSequence(tones: List<Pair<Float, Int>>, volume: Float = 0.5f) {
        try {
            var totalDurationMs = 0
            for (t in tones) totalDurationMs += t.second
            val totalSamples = (sampleRate * (totalDurationMs / 1000f)).toInt()
            val buffer = ShortArray(totalSamples)

            var offset = 0
            for ((freq, durationMs) in tones) {
                val count = (sampleRate * (durationMs / 1000f)).toInt()
                for (i in 0 until count) {
                    val angle = 2.0 * PI * i / (sampleRate / freq)
                    val envelope = sin(PI * (i.toFloat() / count)).toFloat() // Attack-decay
                    val sample = (sin(angle) * Short.MAX_VALUE * volume * envelope).toInt()
                    if (offset + i < buffer.size) {
                        buffer[offset + i] = sample.toShort()
                    }
                }
                offset += count
            }
            playPcmBuffer(buffer)
        } catch (_: Exception) {
        }
    }

    private fun playPcmBuffer(buffer: ShortArray) {
        try {
            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(buffer.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            audioTrack.write(buffer, 0, buffer.size)
            audioTrack.play()
            scope.launch {
                kotlinx.coroutines.delay((buffer.size * 1000L / sampleRate) + 200)
                audioTrack.stop()
                audioTrack.release()
            }
        } catch (_: Exception) {
        }
    }
}
