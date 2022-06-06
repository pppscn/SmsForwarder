/*
 * Copyright 2018 Zhenjie Yan.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.idormy.sms.forwarder.utils

import android.util.Log

/**
 * Created by Zhenjie Yan on 2018/9/12.
 */
@Suppress("unused")
object Logger {
    private const val TAG = "AndServer"
    private const val DEBUG = true
    fun i(obj: Any?) {
        if (DEBUG) {
            Log.i(TAG, obj?.toString() ?: "null")
        }
    }

    fun d(obj: Any?) {
        if (DEBUG) {
            Log.d(TAG, obj?.toString() ?: "null")
        }
    }

    fun v(obj: Any?) {
        if (DEBUG) {
            Log.v(TAG, obj?.toString() ?: "null")
        }
    }

    fun w(obj: Any?) {
        if (DEBUG) {
            Log.w(TAG, obj?.toString() ?: "null")
        }
    }

    fun e(obj: Any?) {
        if (DEBUG) {
            Log.e(TAG, obj?.toString() ?: "null")
        }
    }
}