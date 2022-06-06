package com.idormy.sms.forwarder.utils

import android.app.ActivityManager
import android.content.Context
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

@Suppress("DEPRECATION", "MemberVisibilityCanBePrivate")
class FrpcUtils private constructor() {

    companion object {

        fun waitService(serviceName: String, context: Context): Completable {
            return Completable.fromObservable(
                Observable.interval(3, 1, TimeUnit.SECONDS)
                    .takeUntil { isServiceRunning(serviceName, context) }
            )
        }

        fun isServiceRunning(serviceName: String, context: Context): Boolean {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val runningServices = am.getRunningServices(Int.MAX_VALUE) //获取运行的服务,参数表示最多返回的数量
            for (runningServiceInfo in runningServices) {
                val className = runningServiceInfo.service.className
                if (className == serviceName) {
                    return true
                }
            }
            return false
        }

        fun getStringFromRaw(context: Context, rawName: Int): Observable<String?> {
            return Observable.create { emitter: ObservableEmitter<String?> ->
                val reader = BufferedReader(InputStreamReader(context.resources.openRawResource(rawName)))
                var line: String?
                val result = StringBuilder()
                while (reader.readLine().also { line = it } != null) {
                    result.append(line).append("\n")
                }
                reader.close()
                emitter.onNext(result.toString())
                emitter.onComplete()
            }
        }

    }
}