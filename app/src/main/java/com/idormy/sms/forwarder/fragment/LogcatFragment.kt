package com.idormy.sms.forwarder.fragment

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.core.BaseFragment
import com.idormy.sms.forwarder.databinding.FragmentLogcatBinding
import com.idormy.sms.forwarder.utils.Log
import com.idormy.sms.forwarder.utils.XToastUtils
import com.xuexiang.xaop.annotation.SingleClick
import com.xuexiang.xpage.annotation.Page
import com.xuexiang.xui.utils.ThemeUtils
import com.xuexiang.xui.widget.actionbar.TitleBar
import com.xuexiang.xutil.system.ClipboardUtils
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.io.BufferedReader
import java.io.InputStreamReader

@Page(name = "Logcat")
class LogcatFragment : BaseFragment<FragmentLogcatBinding?>() {

    override fun viewBindingInflate(
        inflater: LayoutInflater,
        container: ViewGroup,
    ): FragmentLogcatBinding {
        return FragmentLogcatBinding.inflate(inflater, container, false)
    }

    override fun initTitle(): TitleBar {
        val titleBar = super.initTitle()!!.setImmersive(false)
        titleBar!!.setTitle(R.string.menu_logcat)
        titleBar.setActionTextColor(ThemeUtils.resolveColor(context, R.attr.colorAccent))
        titleBar.addAction(object : TitleBar.ImageAction(R.drawable.ic_copy) {
            @SingleClick
            override fun performAction(view: View) {
                ClipboardUtils.copyText(binding!!.tvLogcat.text.toString())
                XToastUtils.success(R.string.copySuccess)
            }
        })
        titleBar.addAction(object : TitleBar.ImageAction(R.drawable.ic_delete) {
            @SingleClick
            override fun performAction(view: View) {
                readLog(true)
                binding!!.tvLogcat.text = ""
            }
        })
        return titleBar
    }

    override fun initViews() {
    }

    override fun initListeners() {
        readLog(false)
    }

    private fun readLog(flush: Boolean) {
        val lst: HashSet<String> = LinkedHashSet()
        lst.add("logcat")
        lst.add("-d")
        lst.add("-v")
        lst.add("time")
        lst.add("-s")
        lst.add("GoLog,com.idormy.sms.forwarder.ForegroundService,com.idormy.sms.forwarder.server.ServerService")
        Observable.create { emitter: ObservableEmitter<String?> ->
            if (flush) {
                val lst2: HashSet<String> = LinkedHashSet()
                lst2.add("logcat")
                lst2.add("-c")
                val process = Runtime.getRuntime().exec(lst2.toTypedArray())
                process.waitFor()
            }
            val process = Runtime.getRuntime().exec(lst.toTypedArray())
            val `in` = InputStreamReader(process.inputStream)
            val bufferedReader = BufferedReader(`in`)
            var line: String?
            while (bufferedReader.readLine().also { line = it } != null) {
                emitter.onNext(line!!)
            }
            `in`.close()
            bufferedReader.close()
            emitter.onComplete()
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<String?> {
                override fun onSubscribe(d: Disposable) {}

                override fun onNext(s: String) {
                    binding!!.tvLogcat.append(s)
                    binding!!.tvLogcat.append("\r\n")
                    binding!!.svLogcat.fullScroll(View.FOCUS_DOWN)
                }

                override fun onError(e: Throwable) {
                    e.printStackTrace()
                    Log.e("LogcatFragment", "readLog error: ${e.message}")
                }

                override fun onComplete() {}

            })
    }

}