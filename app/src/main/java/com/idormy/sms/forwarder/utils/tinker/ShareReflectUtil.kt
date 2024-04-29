package com.idormy.sms.forwarder.utils.tinker

import android.annotation.SuppressLint
import android.content.Context
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Method

@Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS", "UNCHECKED_CAST", "unused")
object ShareReflectUtil {
    /**
     * Locates a given field anywhere in the class inheritance hierarchy.
     *
     * @param instance an object to search the field into.
     * @param name     field name
     * @return a field object
     * @throws NoSuchFieldException if the field cannot be located
     */
    @Throws(NoSuchFieldException::class)
    fun findField(instance: Any, name: String): Field {
        var clazz: Class<*>? = instance.javaClass
        while (clazz != null) {
            try {
                val field = clazz.getDeclaredField(name)
                if (!field.isAccessible) {
                    field.isAccessible = true
                }
                return field
            } catch (e: NoSuchFieldException) {
                // ignore and search next
            }
            clazz = clazz.superclass
        }
        throw NoSuchFieldException("Field " + name + " not found in " + instance.javaClass)
    }

    @Throws(NoSuchFieldException::class)
    fun findField(originClazz: Class<*>, name: String): Field {
        var clazz: Class<*>? = originClazz
        while (clazz != null) {
            try {
                val field = clazz.getDeclaredField(name)
                if (!field.isAccessible) {
                    field.isAccessible = true
                }
                return field
            } catch (e: NoSuchFieldException) {
                // ignore and search next
            }
            clazz = clazz.superclass
        }
        throw NoSuchFieldException("Field $name not found in $originClazz")
    }

    /**
     * Locates a given method anywhere in the class inheritance hierarchy.
     *
     * @param instance       an object to search the method into.
     * @param name           method name
     * @param parameterTypes method parameter types
     * @return a method object
     * @throws NoSuchMethodException if the method cannot be located
     */
    @Throws(NoSuchMethodException::class)
    fun findMethod(instance: Any, name: String, vararg parameterTypes: Class<*>?): Method {
        var clazz: Class<*>? = instance.javaClass
        while (clazz != null) {
            try {
                val method = clazz.getDeclaredMethod(name, *parameterTypes)
                if (!method.isAccessible) {
                    method.isAccessible = true
                }
                return method
            } catch (e: NoSuchMethodException) {
                // ignore and search next
            }
            clazz = clazz.superclass
        }
        throw NoSuchMethodException(
            "Method "
                    + name
                    + " with parameters "
                    + listOf(*parameterTypes)
                    + " not found in " + instance.javaClass
        )
    }

    /**
     * Locates a given method anywhere in the class inheritance hierarchy.
     *
     * @param clazz          a class to search the method into.
     * @param name           method name
     * @param parameterTypes method parameter types
     * @return a method object
     * @throws NoSuchMethodException if the method cannot be located
     */
    @Throws(NoSuchMethodException::class)
    fun findMethod(clazz: Class<*>?, name: String, vararg parameterTypes: Class<*>?): Method {
        var tClazz = clazz
        while (tClazz != null) {
            try {
                val method = tClazz.getDeclaredMethod(name, *parameterTypes)
                if (!method.isAccessible) {
                    method.isAccessible = true
                }
                return method
            } catch (e: NoSuchMethodException) {
                // ignore and search next
            }
            tClazz = tClazz.superclass
        }
        throw NoSuchMethodException(
            "Method "
                    + name
                    + " with parameters "
                    + listOf(*parameterTypes)
                    + " not found in tClazz"
        )
    }

    /**
     * Locates a given constructor anywhere in the class inheritance hierarchy.
     *
     * @param instance       an object to search the constructor into.
     * @param parameterTypes constructor parameter types
     * @return a constructor object
     * @throws NoSuchMethodException if the constructor cannot be located
     */
    @Throws(NoSuchMethodException::class)
    fun findConstructor(instance: Any, vararg parameterTypes: Class<*>?): Constructor<*> {
        var clazz: Class<*>? = instance.javaClass
        while (clazz != null) {
            try {
                val constructor = clazz.getDeclaredConstructor(*parameterTypes)
                if (!constructor.isAccessible) {
                    constructor.isAccessible = true
                }
                return constructor
            } catch (e: NoSuchMethodException) {
                // ignore and search next
            }
            clazz = clazz.superclass
        }
        throw NoSuchMethodException(
            "Constructor"
                    + " with parameters "
                    + listOf(*parameterTypes)
                    + " not found in " + instance.javaClass
        )
    }

    /**
     * Replace the value of a field containing a non null array, by a new array containing the
     * elements of the original array plus the elements of extraElements.
     *
     * @param instance      the instance whose field is to be modified.
     * @param fieldName     the field to modify.
     * @param extraElements elements to append at the end of the array.
     */
    @Throws(NoSuchFieldException::class, IllegalArgumentException::class, IllegalAccessException::class)
    fun expandFieldArray(instance: Any, fieldName: String, extraElements: Array<Any?>) {
        val jlrField = findField(instance, fieldName)
        val original = jlrField[instance] as Array<Any>
        val combined = java.lang.reflect.Array.newInstance(original.javaClass.componentType, original.size + extraElements.size) as Array<Any>

        // NOTE: changed to copy extraElements first, for patch load first
        System.arraycopy(extraElements, 0, combined, 0, extraElements.size)
        System.arraycopy(original, 0, combined, extraElements.size, original.size)
        jlrField[instance] = combined
    }

    /**
     * Replace the value of a field containing a non null array, by a new array containing the
     * elements of the original array plus the elements of extraElements.
     *
     * @param instance      the instance whose field is to be modified.
     * @param fieldName     the field to modify.
     */
    @Throws(NoSuchFieldException::class, IllegalArgumentException::class, IllegalAccessException::class)
    fun reduceFieldArray(instance: Any, fieldName: String, reduceSize: Int) {
        if (reduceSize <= 0) {
            return
        }
        val jlrField = findField(instance, fieldName)
        val original = jlrField[instance] as Array<Any>
        val finalLength = original.size - reduceSize
        if (finalLength <= 0) {
            return
        }
        val combined = java.lang.reflect.Array.newInstance(original.javaClass.componentType, finalLength) as Array<Any>
        System.arraycopy(original, reduceSize, combined, 0, finalLength)
        jlrField[instance] = combined
    }

    @SuppressLint("PrivateApi")
    fun getActivityThread(
        context: Context?,
        activityThread: Class<*>?,
    ): Any? {
        var tActivityThread = activityThread
        return try {
            if (tActivityThread == null) {
                tActivityThread = Class.forName("android.app.ActivityThread")
            }
            val m = tActivityThread!!.getMethod("currentActivityThread")
            m.isAccessible = true
            var currentActivityThread = m.invoke(null)
            if (currentActivityThread == null && context != null) {
                // In older versions of Android (prior to frameworks/base 66a017b63461a22842)
                // the currentActivityThread was built on thread locals, so we'll need to try
                // even harder
                val mLoadedApk = context.javaClass.getField("mLoadedApk")
                mLoadedApk.isAccessible = true
                val apk = mLoadedApk[context]
                val mActivityThreadField = apk.javaClass.getDeclaredField("mActivityThread")
                mActivityThreadField.isAccessible = true
                currentActivityThread = mActivityThreadField[apk]
            }
            currentActivityThread
        } catch (ignore: Throwable) {
            null
        }
    }

    /**
     * Handy method for fetching hidden integer constant value in system classes.
     *
     * @param clazz
     * @param fieldName
     * @return
     */
    fun getValueOfStaticIntField(clazz: Class<*>, fieldName: String, defVal: Int): Int {
        return try {
            val field = findField(clazz, fieldName)
            field.getInt(null)
        } catch (thr: Throwable) {
            defVal
        }
    }
}