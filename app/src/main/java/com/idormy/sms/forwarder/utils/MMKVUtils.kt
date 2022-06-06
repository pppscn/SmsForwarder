package com.idormy.sms.forwarder.utils

import android.content.Context
import android.os.Parcelable
import android.util.Log
import androidx.preference.PreferenceManager
import com.tencent.mmkv.MMKV

/**
 * MMKV工具类
 *
 * @author xuexiang
 * @since 2019-07-04 10:20
 */
@Suppress("PropertyName", "UNCHECKED_CAST", "MemberVisibilityCanBePrivate", "unused")
class MMKVUtils private constructor() {

    companion object {
        private var TAG: String = "MMKVUtils"
        private var sMMKV: MMKV? = null

        /**
         * 初始化
         *
         * @param context
         */
        fun init(context: Context) {
            MMKV.initialize(context.applicationContext)
            sMMKV = MMKV.defaultMMKV()
        }

        fun getsMMKV(): MMKV? {
            if (sMMKV == null) {
                sMMKV = MMKV.defaultMMKV()
            }
            return sMMKV
        }
        //=======================================键值保存==================================================//
        /**
         * 保存键值
         *
         * @param key
         * @param value
         * @return
         */
        fun put(key: String?, value: Any?): Boolean {
            when (value) {
                is Int -> {
                    return getsMMKV()!!.encode(key, (value as Int?)!!)
                }
                is Float -> {
                    return getsMMKV()!!.encode(key, (value as Float?)!!)
                }
                is String -> {
                    return getsMMKV()!!.encode(key, value as String?)
                }
                is Boolean -> {
                    return getsMMKV()!!.encode(key, (value as Boolean?)!!)
                }
                is Long -> {
                    return getsMMKV()!!.encode(key, (value as Long?)!!)
                }
                is Double -> {
                    return getsMMKV()!!.encode(key, (value as Double?)!!)
                }
                is Parcelable -> {
                    return getsMMKV()!!.encode(key, value as Parcelable?)
                }
                is ByteArray -> {
                    return getsMMKV()!!.encode(key, value as ByteArray?)
                }
                is Set<*> -> {
                    return getsMMKV()!!.encode(key, value as Set<String?>?)
                }
                else -> return false
            }
        }
        //=======================================键值获取==================================================//
        /**
         * 获取键值
         *
         * @param key
         * @param defaultValue
         * @return
         */
        operator fun get(key: String?, defaultValue: Any?): Any? {
            when (defaultValue) {
                is Int -> {
                    return getsMMKV()!!
                        .decodeInt(key, (defaultValue as Int?)!!)
                }
                is Float -> {
                    return getsMMKV()!!
                        .decodeFloat(key, (defaultValue as Float?)!!)
                }
                is String -> {
                    return getsMMKV()!!.decodeString(key, defaultValue as String?)
                }
                is Boolean -> {
                    return getsMMKV()!!
                        .decodeBool(key, (defaultValue as Boolean?)!!)
                }
                is Long -> {
                    return getsMMKV()!!
                        .decodeLong(key, (defaultValue as Long?)!!)
                }
                is Double -> {
                    return getsMMKV()!!
                        .decodeDouble(key, (defaultValue as Double?)!!)
                }
                is ByteArray -> {
                    return getsMMKV()!!.decodeBytes(key)
                }
                is Set<*> -> {
                    return getsMMKV()!!.decodeStringSet(key, defaultValue as Set<String?>?)
                }
                else -> return null
            }
        }

        /**
         * 根据key获取boolean值
         *
         * @param key
         * @param defValue
         * @return
         */
        fun getBoolean(key: String?, defValue: Boolean): Boolean {
            try {
                return getsMMKV()!!.getBoolean(key, defValue)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return defValue
        }

        /**
         * 根据key获取long值
         *
         * @param key
         * @param defValue
         * @return
         */
        fun getLong(key: String?, defValue: Long): Long {
            try {
                return getsMMKV()!!.getLong(key, defValue)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return defValue
        }

        /**
         * 根据key获取float值
         *
         * @param key
         * @param defValue
         * @return
         */
        fun getFloat(key: String?, defValue: Float): Float {
            try {
                return getsMMKV()!!.getFloat(key, defValue)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return defValue
        }

        /**
         * 根据key获取String值
         *
         * @param key
         * @param defValue
         * @return
         */
        fun getString(key: String?, defValue: String?): String? {
            try {
                return getsMMKV()!!.getString(key, defValue)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return defValue
        }

        /**
         * 根据key获取int值
         *
         * @param key
         * @param defValue
         * @return
         */
        fun getInt(key: String?, defValue: Int): Int {
            try {
                return getsMMKV()!!.getInt(key, defValue)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return defValue
        }

        /**
         * 根据key获取double值
         *
         * @param key
         * @param defValue
         * @return
         */
        fun getDouble(key: String?, defValue: Double): Double {
            try {
                return getsMMKV()!!.decodeDouble(key, defValue)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return defValue
        }

        /**
         * 获取对象
         *
         * @param key
         * @param tClass 类型
         * @param <T>
         * @return
        </T> */
        fun <T : Parcelable?> getObject(key: String?, tClass: Class<T>?): T? {
            return getsMMKV()!!.decodeParcelable(key, tClass)
        }

        /**
         * 获取对象
         *
         * @param key
         * @param tClass 类型
         * @param <T>
         * @return
        </T> */
        fun <T : Parcelable?> getObject(key: String?, tClass: Class<T>?, defValue: T): T? {
            try {
                return getsMMKV()!!.decodeParcelable(key, tClass, defValue)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return defValue
        }

        /**
         * 判断键值对是否存在
         *
         * @param key 键
         * @return 键值对是否存在
         */
        fun containsKey(key: String?): Boolean {
            return getsMMKV()!!.containsKey(key)
        }

        /**
         * 清除指定键值对
         *
         * @param key 键
         */
        fun remove(key: String?) {
            getsMMKV()!!.remove(key).apply()
        }

        /**
         * 从SP迁移数据
         */
        fun importSharedPreferences(context: Context) {
            Log.d(TAG, "从SP迁移数据")
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            val editor = preferences.edit()
            getsMMKV()!!.importFromSharedPreferences(preferences)
            editor.clear().apply()

            Log.d(TAG, "转换旧的SP配置")
            loop@ for (key: String in getsMMKV()!!.allKeys()!!) {
                when {
                    key.startsWith("tsms_msg_key_switch_") || key.startsWith("tsms_msg_key_string_enable_") || key.endsWith("battery_level_once") -> {
                        val newKey = key.replace("tsms_msg_key_switch_", "enable_")
                            .replace("tsms_msg_key_string_", "enable_")
                            .replace("enable_enable_", "enable_")
                        val value = getBoolean(key, false)
                        Log.d(TAG, String.format("oldKey=%s, newKey=%s, value=%s", key, newKey, value.toString()))
                        put(newKey, value)
                        remove(key)
                        continue@loop
                    }
                    key.endsWith("battery_level_alarm") || key.endsWith("battery_level_max") || key.endsWith("battery_level_current") || key.endsWith("battery_status") || key.endsWith("battery_cron_interval") -> {
                        val newKey = key.replace("tsms_msg_key_switch_", "")
                            .replace("tsms_msg_key_string_", "")
                            .replace("alarm", "min")
                            .replace("tsms_msg_key_", "request_")
                        val value = getInt(key, 0)
                        Log.d(TAG, String.format("oldKey=%s, newKey=%s, value=%s", key, newKey, value.toString()))
                        put(newKey, value)
                        remove(key)
                        continue@loop
                    }
                    key.startsWith("tsms_msg_key_") -> {
                        val newKey = key.replace("tsms_msg_key_string_", "")
                            .replace("add_", "")
                            .replace("tsms_msg_key_", "request_")
                        val value = getString(key, "")
                        Log.d(TAG, String.format("oldKey=%s, newKey=%s, value=%s", key, newKey, value.toString()))
                        put(newKey, value)
                        remove(key)
                        continue@loop
                    }
                }
            }

            Log.d(TAG, "转换后的数据")
            for (key: String in getsMMKV()!!.allKeys()!!) {
                when {
                    key.startsWith("enable_") -> {
                        Log.d(TAG, String.format("key=%s, value=%s", key, getBoolean(key, false).toString()))
                    }
                    key.startsWith("battery_") || key.startsWith("request_") -> {
                        Log.d(TAG, String.format("key=%s, value=%s", key, getInt(key, 0).toString()))
                    }
                    else -> {
                        Log.d(TAG, String.format("key=%s, value=%s", key, getString(key, "").toString()))
                    }
                }
            }
        }
    }

    init {
        throw UnsupportedOperationException("u can't instantiate me...")
    }
}