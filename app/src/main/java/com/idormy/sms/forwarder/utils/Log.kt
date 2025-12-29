package com.idormy.sms.forwarder.utils

import android.content.Context
import android.util.Log

object Log {
    private const val DEFAULT_TAG = "SmsForwarder"

    fun init(context: Context) {
        // Simple log initialization
    }

    fun d(tag: String, msg: String) {
        Log.d(tag, msg)
    }

    fun i(tag: String, msg: String) {
        Log.i(tag, msg)
    }

    fun e(tag: String, msg: String) {
        Log.e(tag, msg)
    }
}
