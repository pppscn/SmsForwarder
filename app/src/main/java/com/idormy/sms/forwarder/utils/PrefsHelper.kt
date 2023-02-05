@file:Suppress("DEPRECATION")

package com.idormy.sms.forwarder.utils

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager

/**
 * Created by aykutasil on 8.12.2016.
 */

@Suppress("unused")
class PrefsHelper private constructor() {

    lateinit var preference: SharedPreferences

    val prefEditor: SharedPreferences.Editor
        get() = preference.edit()

    constructor(context: Context, prefName: String) : this() {
        preference = context.getSharedPreferences(prefName, Context.MODE_PRIVATE)
    }

    constructor(context: Context) : this() {
        preference = getDefaultPreference(context)
    }

    companion object {

        private val DEFAULT_STRING_VALUE: String? = null
        private const val DEFAULT_INT_VALUE = 0
        private const val DEFAULT_BOOLEAN_VALUE = false

        fun getDefaultPreference(context: Context): SharedPreferences {
            return PreferenceManager.getDefaultSharedPreferences(context)
        }

        fun writePrefString(context: Context, key: String, value: String?) {
            PrefsHelper(context).prefEditor.putString(key, value).commit()
        }

        fun readPrefString(context: Context, key: String): String? {
            return PrefsHelper(context).preference.getString(key, DEFAULT_STRING_VALUE)
        }

        fun writePrefInt(context: Context, key: String, value: Int) {
            PrefsHelper(context).prefEditor.putInt(key, value).commit()
        }

        fun readPrefInt(context: Context, key: String): Int {
            return PrefsHelper(context).preference.getInt(key, DEFAULT_INT_VALUE)
        }

        fun writePrefBool(context: Context, key: String, value: Boolean) {
            PrefsHelper(context).prefEditor.putBoolean(key, value).commit()
        }

        fun readPrefBool(context: Context, key: String): Boolean {
            return PrefsHelper(context).preference.getBoolean(key, DEFAULT_BOOLEAN_VALUE)
        }

        fun clearPreference(context: Context) {
            PrefsHelper(context).preference.edit().clear().apply()
        }
    }
}