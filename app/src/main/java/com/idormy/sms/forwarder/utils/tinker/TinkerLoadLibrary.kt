/*
 * Tencent is pleased to support the open source community by making Tinker available.
 *
 * Copyright (C) 2016 THL A29 Limited, a Tencent company. All rights reserved.
 *
 * Licensed under the BSD 3-Clause License (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * https://opensource.org/licenses/BSD-3-Clause
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.idormy.sms.forwarder.utils.tinker

import android.annotation.SuppressLint
import android.os.Build
import com.idormy.sms.forwarder.utils.Log
import java.io.File
import java.io.IOException

/**
 * Created by zhangshaowen on 17/1/5.
 * Thanks for Android Fragmentation
 */
@Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS", "UNCHECKED_CAST", "SENSELESS_COMPARISON")
object TinkerLoadLibrary {
    private const val TAG = "Tinker.LoadLibrary"

    @SuppressLint("ObsoleteSdkInt")
    @Throws(Throwable::class)
    fun installNativeLibraryPath(classLoader: ClassLoader, folder: File?) {
        if (folder == null || !folder.exists()) {
            Log.e(TAG, String.format("installNativeLibraryPath, folder %s is illegal", folder))
            return
        }
        // android o sdk_int 26
        // for android o preview sdk_int 25
        if (Build.VERSION.SDK_INT == 25 && Build.VERSION.PREVIEW_SDK_INT != 0
            || Build.VERSION.SDK_INT > 25
        ) {
            try {
                V25.install(classLoader, folder)
            } catch (throwable: Throwable) {
                // install fail, try to treat it as v23
                // some preview N version may go here
                Log.e(
                    TAG, String.format(
                        "installNativeLibraryPath, v25 fail, sdk: %d, error: %s, try to fallback to V23",
                        Build.VERSION.SDK_INT, throwable.message
                    )
                )
                V23.install(classLoader, folder)
            }
        } else if (Build.VERSION.SDK_INT >= 23) {
            try {
                V23.install(classLoader, folder)
            } catch (throwable: Throwable) {
                // install fail, try to treat it as v14
                Log.e(
                    TAG, String.format(
                        "installNativeLibraryPath, v23 fail, sdk: %d, error: %s, try to fallback to V14",
                        Build.VERSION.SDK_INT, throwable.message
                    )
                )
                V14.install(classLoader, folder)
            }
        } else if (Build.VERSION.SDK_INT >= 14) {
            V14.install(classLoader, folder)
        } else {
            V4.install(classLoader, folder)
        }
    }

    object V4 {
        @Throws(Throwable::class)
        fun install(classLoader: ClassLoader, folder: File) {
            val addPath = folder.path
            val pathField = ShareReflectUtil.findField(classLoader, "libPath")
            val origLibPaths = pathField[classLoader] as String
            val origLibPathSplit = origLibPaths.split(":".toRegex()).toTypedArray()
            val newLibPaths = StringBuilder(addPath)
            for (origLibPath in origLibPathSplit) {
                if (origLibPath == null || addPath == origLibPath) {
                    continue
                }
                newLibPaths.append(':').append(origLibPath)
            }
            pathField[classLoader] = newLibPaths.toString()
            val libraryPathElementsFiled = ShareReflectUtil.findField(classLoader, "libraryPathElements")
            val libraryPathElements = libraryPathElementsFiled[classLoader] as MutableList<String>
            val libPathElementIt = libraryPathElements.iterator()
            while (libPathElementIt.hasNext()) {
                val libPath = libPathElementIt.next()
                if (addPath == libPath) {
                    libPathElementIt.remove()
                    break
                }
            }
            libraryPathElements.add(0, addPath)
            libraryPathElementsFiled[classLoader] = libraryPathElements
        }
    }

    object V14 {
        @Throws(Throwable::class)
        fun install(classLoader: ClassLoader, folder: File) {
            val pathListField = ShareReflectUtil.findField(classLoader, "pathList")
            val dexPathList = pathListField[classLoader]
            val nativeLibDirField = ShareReflectUtil.findField(dexPathList, "nativeLibraryDirectories")
            val origNativeLibDirs = nativeLibDirField[dexPathList] as Array<File>
            val newNativeLibDirList: MutableList<File> = ArrayList(origNativeLibDirs.size + 1)
            newNativeLibDirList.add(folder)
            for (origNativeLibDir in origNativeLibDirs) {
                if (folder != origNativeLibDir) {
                    newNativeLibDirList.add(origNativeLibDir)
                }
            }
            nativeLibDirField[dexPathList] = newNativeLibDirList.toTypedArray()
        }
    }

    object V23 {
        @Throws(Throwable::class)
        fun install(classLoader: ClassLoader, folder: File) {
            val pathListField = ShareReflectUtil.findField(classLoader, "pathList")
            val dexPathList = pathListField[classLoader]
            val nativeLibraryDirectories = ShareReflectUtil.findField(dexPathList, "nativeLibraryDirectories")
            var origLibDirs = nativeLibraryDirectories[dexPathList] as MutableList<File>
            if (origLibDirs == null) {
                origLibDirs = ArrayList(2)
            }
            val libDirIt = origLibDirs.iterator()
            while (libDirIt.hasNext()) {
                val libDir = libDirIt.next()
                if (folder == libDir) {
                    libDirIt.remove()
                    break
                }
            }
            origLibDirs.add(0, folder)
            val systemNativeLibraryDirectories = ShareReflectUtil.findField(dexPathList, "systemNativeLibraryDirectories")
            var origSystemLibDirs = systemNativeLibraryDirectories[dexPathList] as List<File>
            if (origSystemLibDirs == null) {
                origSystemLibDirs = ArrayList(2)
            }
            val newLibDirs: MutableList<File> = ArrayList(origLibDirs.size + origSystemLibDirs.size + 1)
            newLibDirs.addAll(origLibDirs)
            newLibDirs.addAll(origSystemLibDirs)
            val makeElements = ShareReflectUtil.findMethod(
                dexPathList,
                "makePathElements", MutableList::class.java, File::class.java, MutableList::class.java
            )
            val suppressedExceptions = ArrayList<IOException>()
            val elements = makeElements.invoke(dexPathList, newLibDirs, null, suppressedExceptions) as Array<Any>
            val nativeLibraryPathElements = ShareReflectUtil.findField(dexPathList, "nativeLibraryPathElements")
            nativeLibraryPathElements[dexPathList] = elements
        }
    }

    object V25 {
        @Throws(Throwable::class)
        fun install(classLoader: ClassLoader, folder: File) {
            val pathListField = ShareReflectUtil.findField(classLoader, "pathList")
            val dexPathList = pathListField[classLoader]
            val nativeLibraryDirectories = ShareReflectUtil.findField(dexPathList, "nativeLibraryDirectories")
            var origLibDirs = nativeLibraryDirectories[dexPathList] as MutableList<File>
            if (origLibDirs == null) {
                origLibDirs = ArrayList(2)
            }
            val libDirIt = origLibDirs.iterator()
            while (libDirIt.hasNext()) {
                val libDir = libDirIt.next()
                if (folder == libDir) {
                    libDirIt.remove()
                    break
                }
            }
            origLibDirs.add(0, folder)
            val systemNativeLibraryDirectories = ShareReflectUtil.findField(dexPathList, "systemNativeLibraryDirectories")
            var origSystemLibDirs = systemNativeLibraryDirectories[dexPathList] as List<File>
            if (origSystemLibDirs == null) {
                origSystemLibDirs = ArrayList(2)
            }
            val newLibDirs: MutableList<File> = ArrayList(origLibDirs.size + origSystemLibDirs.size + 1)
            newLibDirs.addAll(origLibDirs)
            newLibDirs.addAll(origSystemLibDirs)
            val makeElements = ShareReflectUtil.findMethod(dexPathList, "makePathElements", MutableList::class.java)
            val elements = makeElements.invoke(dexPathList, newLibDirs) as Array<Any>
            val nativeLibraryPathElements = ShareReflectUtil.findField(dexPathList, "nativeLibraryPathElements")
            nativeLibraryPathElements[dexPathList] = elements
        }
    }
}