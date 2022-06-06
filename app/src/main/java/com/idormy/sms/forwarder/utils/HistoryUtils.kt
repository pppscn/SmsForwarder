package com.idormy.sms.forwarder.utils

import android.content.Context
import android.os.Parcelable
import com.tencent.mmkv.MMKV

/**
 * 转发历史工具类
 *
 * @author pppscn
 * @since 2022年5月9日
 */
@Suppress("PropertyName", "UNCHECKED_CAST", "MemberVisibilityCanBePrivate", "unused")
class HistoryUtils private constructor() {

    companion object {
        private var sMMKV: MMKV? = null

        /**
         * 初始化
         *
         * @param context
         */
        fun init(context: Context) {
            MMKV.initialize(context.applicationContext)
            sMMKV = MMKV.mmkvWithID("History")
        }

        fun getsMMKV(): MMKV? {
            if (sMMKV == null) {
                sMMKV = MMKV.mmkvWithID("History")
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
         * 清除所有键值对
         */
        fun clear() {
            getsMMKV()!!.clearAll()
        }

    }

    init {
        throw UnsupportedOperationException("u can't instantiate me...")
    }
}