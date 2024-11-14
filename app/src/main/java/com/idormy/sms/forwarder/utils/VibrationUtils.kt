package com.idormy.sms.forwarder.utils

import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator

@Suppress("DEPRECATION")
class VibrationUtils(context: Context) {

    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    private val handler = Handler(Looper.getMainLooper())
    var isVibrating = false
        private set

    fun startVibration(patternString: String) {
        isVibrating = true
        val parsedPattern = parsePattern(patternString)
        vibratePattern(parsedPattern, 0)
    }

    fun stopVibration() {
        isVibrating = false
        vibrator.cancel()
    }

    private fun parsePattern(pattern: String): List<Triple<Long, Boolean, Int>> {
        val parsedPattern = mutableListOf<Triple<Long, Boolean, Int>>()
        var currentChar = pattern[0]
        var currentLength = 1L

        for (i in 1 until pattern.length) {
            if (pattern[i] == currentChar) {
                currentLength++
            } else {
                parsedPattern.add(createTriple(currentChar, currentLength))
                currentChar = pattern[i]
                currentLength = 1L
            }
        }
        parsedPattern.add(createTriple(currentChar, currentLength))
        return parsedPattern
    }

    private fun createTriple(char: Char, length: Long): Triple<Long, Boolean, Int> {
        val duration = 100L * length
        val intensity = when (char) {
            '=' -> 255
            '-' -> 128
            '_' -> 0
            else -> 0
        }
        return Triple(duration, intensity > 0, intensity)
    }

    private fun vibratePattern(parsedPattern: List<Triple<Long, Boolean, Int>>, index: Int) {
        if (isVibrating && index < parsedPattern.size) {
            val (duration, shouldVibrate, intensity) = parsedPattern[index]
            if (shouldVibrate) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val effect = VibrationEffect.createOneShot(duration, intensity)
                    vibrator.vibrate(effect)
                } else {
                    vibrator.vibrate(duration)
                }
            }
            handler.postDelayed({
                if (isVibrating) {
                    vibrator.cancel()
                    vibratePattern(parsedPattern, index + 1)
                }
            }, duration)
        }
    }
}
