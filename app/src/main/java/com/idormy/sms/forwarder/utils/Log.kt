package com.idormy.sms.forwarder.utils

import android.content.Context
import android.os.Build
import com.idormy.sms.forwarder.App
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.util.Log as AndroidLog

@Suppress("unused", "MemberVisibilityCanBePrivate")
object Log {
    const val ASSERT = 7
    const val DEBUG = 3
    const val ERROR = 6
    const val INFO = 4
    const val VERBOSE = 2
    const val WARN = 5

    private const val TAG = "Logger"
    private var logFile: File? = null
    private lateinit var appContext: Context
    private var initDate: String = ""

    fun init(context: Context) {
        appContext = context
        createLogFile()
    }

    private fun createLogFile() {
        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        if (currentDate != initDate || logFile == null || !logFile!!.exists()) {
            initDate = currentDate
            val logPath = appContext.cacheDir.absolutePath + "/logs"
            val logDir = File(logPath)
            if (!logDir.exists()) logDir.mkdirs()
            logFile = File(logPath, "log_$currentDate.txt")
        }
    }

    fun logToFile(level: String, tag: String, message: String) {
        if (Build.DEVICE == null) return
        
        if (!::appContext.isInitialized) {
            throw IllegalStateException("Log not initialized. Call init(context) first.")
        }

        if (!App.isDebug) return

        Thread {
            try {
                createLogFile()
                logFile?.let { file ->
                    try {
                        val logTimeStamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date())
                        val logWriter = FileWriter(file, true)
                        logWriter.append("$logTimeStamp | $level | $tag | $message\n\n")
                        logWriter.close()
                    } catch (e: Exception) {
                        AndroidLog.e(TAG, "Error writing to file: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                AndroidLog.e(TAG, "Error writing to file: ${e.message}")
            }
        }.start()
    }

    fun v(tag: String, message: String) {
        AndroidLog.v(tag, message)
        logToFile("V", tag, message)
    }

    fun v(tag: String, message: String, throwable: Throwable) {
        val logMessage = "${message}\n${getStackTraceString(throwable)}"
        AndroidLog.v(tag, logMessage)
        logToFile("V", tag, logMessage)
    }

    fun d(tag: String, message: String) {
        AndroidLog.d(tag, message)
        logToFile("D", tag, message)
    }

    fun d(tag: String, message: String, throwable: Throwable) {
        val logMessage = "${message}\n${getStackTraceString(throwable)}"
        AndroidLog.d(tag, logMessage)
        logToFile("D", tag, logMessage)
    }

    fun i(tag: String, message: String) {
        AndroidLog.d(tag, message)
        logToFile("I", tag, message)
    }

    fun i(tag: String, message: String, throwable: Throwable) {
        val logMessage = "${message}\n${getStackTraceString(throwable)}"
        AndroidLog.d(tag, logMessage)
        logToFile("I", tag, logMessage)
    }

    fun w(tag: String, message: String) {
        AndroidLog.w(tag, message)
        logToFile("W", tag, message)
    }

    fun w(tag: String, throwable: Throwable) {
        val logMessage = getStackTraceString(throwable)
        AndroidLog.w(tag, logMessage)
        logToFile("W", tag, logMessage)
    }

    fun w(tag: String, message: String, throwable: Throwable) {
        val logMessage = "${message}\n${getStackTraceString(throwable)}"
        AndroidLog.w(tag, logMessage)
        logToFile("W", tag, logMessage)
    }

    fun e(tag: String, message: String) {
        AndroidLog.e(tag, message)
        logToFile("E", tag, message)
    }

    fun e(tag: String, message: String, throwable: Throwable) {
        val logMessage = "${message}\n${getStackTraceString(throwable)}"
        AndroidLog.e(tag, logMessage)
        logToFile("E", tag, logMessage)
    }

    fun wtf(tag: String, message: String) {
        AndroidLog.wtf(tag, message)
        logToFile("WTF", tag, message)
    }

    fun wtf(tag: String, throwable: Throwable) {
        val logMessage = getStackTraceString(throwable)
        AndroidLog.wtf(tag, logMessage)
        logToFile("WTF", tag, logMessage)
    }

    fun wtf(tag: String, message: String, throwable: Throwable) {
        val logMessage = "${message}\n${getStackTraceString(throwable)}"
        AndroidLog.wtf(tag, logMessage)
        logToFile("WTF", tag, logMessage)
    }

    fun getStackTraceString(throwable: Throwable): String {
        return AndroidLog.getStackTraceString(throwable)
    }

    fun isLoggable(tag: String?, level: Int): Boolean {
        return AndroidLog.isLoggable(tag, level)
    }

    fun println(priority: Int, tag: String, message: String) {
        AndroidLog.println(priority, tag, message)
        logToFile("P", tag, message)
    }
}
