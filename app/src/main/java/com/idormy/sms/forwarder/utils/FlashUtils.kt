@file:Suppress("DEPRECATION")

package com.idormy.sms.forwarder.utils

import android.content.Context
import android.hardware.Camera
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Handler
import android.os.Looper

class FlashUtils(context: Context) {
    private var cameraManager: CameraManager? = null
    private var cameraId: String? = null
    private var legacyCamera: Camera? = null
    private var legacyParams: Camera.Parameters? = null
    private var handler: Handler? = null
    private val duration = 100L // 闪烁持续时间
    var isFlashing = false
        private set

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            try {
                cameraId = cameraManager?.cameraIdList?.get(0) // 获取后置摄像头 ID
            } catch (e: CameraAccessException) {
                e.printStackTrace()
            }
        } else {
            try {
                legacyCamera = Camera.open()
                legacyParams = legacyCamera?.parameters
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 按照模式控制闪光灯
     * @param pattern 例如 "XXOOXXOO" （X-开，O-关）
     * @param repeatTimes 闪烁的重复次数，0 表示无限循环
     */
    fun startFlashing(pattern: String, repeatTimes: Int) {
        if (isFlashing) return
        isFlashing = true
        handler = Handler(Looper.getMainLooper())

        val sequence = pattern.toCharArray()
        var index = 0
        var repeatCount = 0

        val runnable = object : Runnable {
            override fun run() {
                if (!isFlashing) return

                val shouldFlash = sequence[index] == 'X' || sequence[index] == '1'
                setFlashlight(shouldFlash)
                index++

                if (index >= sequence.size) {
                    index = 0
                    repeatCount++
                    if (repeatTimes != 0 && repeatCount >= repeatTimes) {
                        stopFlashing()
                        return
                    }
                }

                handler?.postDelayed(this, duration)
            }
        }

        handler?.post(runnable)
    }

    /**
     * 关闭闪光灯并停止模式
     */
    fun stopFlashing() {
        isFlashing = false
        handler?.removeCallbacksAndMessages(null)
        setFlashlight(false) // 确保停止后灯是关闭的
    }

    /**
     * 设置闪光灯状态，兼容 Android 4.4+
     */
    private fun setFlashlight(enable: Boolean) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                cameraManager?.setTorchMode(cameraId!!, enable)
            } else {
                legacyParams?.flashMode = if (enable) Camera.Parameters.FLASH_MODE_TORCH else Camera.Parameters.FLASH_MODE_OFF
                legacyCamera?.parameters = legacyParams
                if (enable) legacyCamera?.startPreview() else legacyCamera?.stopPreview()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 释放旧 API 资源
     */
    fun release() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            legacyCamera?.stopPreview()
            legacyCamera?.release()
            legacyCamera = null
        }
    }

}
