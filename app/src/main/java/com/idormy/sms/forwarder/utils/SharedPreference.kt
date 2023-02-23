package com.idormy.sms.forwarder.utils

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import java.io.*
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

@Suppress("unused", "UNCHECKED_CAST")
class SharedPreference<T>(private val name: String, private val default: T) : ReadWriteProperty<Any?, T> {

    companion object {
        lateinit var preference: SharedPreferences

        fun init(context: Context) {
            preference = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val directBootContext: Context = context.createDeviceProtectedStorageContext()
                directBootContext.getSharedPreferences(context.packageName, Context.MODE_PRIVATE)
            } else {
                context.getSharedPreferences(context.packageName, Context.MODE_PRIVATE)
            }
        }

        //删除全部数据
        fun clearPreference() = preference.edit().clear().apply()

        //根据key删除存储数据
        fun clearPreference(key: String) = preference.edit().remove(key).commit()

        //导出全部数据
        fun exportPreference(): String {
            return serialize(preference.all)
        }

        //导入全部数据
        fun importPreference(data: String) {
            val map = deSerialization<Map<String, Any>>(data)
            val editor = preference.edit()
            for ((key, value) in map) {
                when (value) {
                    is Long -> editor.putLong(key, value)
                    is Int -> editor.putInt(key, value)
                    is String -> editor.putString(key, value)
                    is Boolean -> editor.putBoolean(key, value)
                    is Float -> editor.putFloat(key, value)
                    else -> editor.putString(key, serialize(value))
                }
            }
            editor.apply()
        }

        /**
         * 序列化对象
         * @throws IOException
         */
        @Throws(IOException::class)
        private fun <T> serialize(obj: T): String {
            val byteArrayOutputStream = ByteArrayOutputStream()
            val objectOutputStream = ObjectOutputStream(
                byteArrayOutputStream
            )
            objectOutputStream.writeObject(obj)
            var serStr = byteArrayOutputStream.toString("ISO-8859-1")
            serStr = java.net.URLEncoder.encode(serStr, "UTF-8")
            objectOutputStream.close()
            byteArrayOutputStream.close()
            return serStr
        }

        /**
         * 反序列化对象
         * @param str
         * @throws IOException
         * @throws ClassNotFoundException
         */
        @Throws(IOException::class, ClassNotFoundException::class)
        private fun <T> deSerialization(str: String): T {
            val redStr = java.net.URLDecoder.decode(str, "UTF-8")
            val byteArrayInputStream = ByteArrayInputStream(
                redStr.toByteArray(charset("ISO-8859-1"))
            )
            val objectInputStream = ObjectInputStream(
                byteArrayInputStream
            )
            val obj = objectInputStream.readObject() as T
            objectInputStream.close()
            byteArrayInputStream.close()
            return obj
        }
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        return putPreference(name, value)
    }

    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return getPreference(name, default)
    }

    /**
     * 查找数据 返回给调用方法一个具体的对象
     * 如果查找不到类型就采用反序列化方法来返回类型
     * default是默认对象 以防止会返回空对象的异常
     * 即如果name没有查找到value 就返回默认的序列化对象，然后经过反序列化返回
     */
    private fun getPreference(name: String, default: T): T = with(preference) {
        val res: Any = when (default) {
            is Long -> getLong(name, default)
            is String -> this.getString(name, default)!!
            is Int -> getInt(name, default)
            is Boolean -> getBoolean(name, default)
            is Float -> getFloat(name, default)
            //else -> throw IllegalArgumentException("This type can be get from Preferences")
            else -> deSerialization(getString(name, serialize(default)).toString())
        }
        return res as T
    }

    private fun putPreference(name: String, value: T) = with(preference.edit()) {
        when (value) {
            is Long -> putLong(name, value)
            is Int -> putInt(name, value)
            is String -> putString(name, value)
            is Boolean -> putBoolean(name, value)
            is Float -> putFloat(name, value)
            //else -> throw IllegalArgumentException("This type can be saved into Preferences")
            else -> putString(name, serialize(value))
        }.apply()
    }
}