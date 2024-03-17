package com.idormy.sms.forwarder.fragment

import android.content.Intent
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView.RecycledViewPool
import com.alibaba.android.vlayout.VirtualLayoutManager
import com.idormy.sms.forwarder.App
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.adapter.FrpcPagingAdapter
import com.idormy.sms.forwarder.core.BaseFragment
import com.idormy.sms.forwarder.database.entity.Frpc
import com.idormy.sms.forwarder.database.viewmodel.BaseViewModelFactory
import com.idormy.sms.forwarder.database.viewmodel.FrpcViewModel
import com.idormy.sms.forwarder.databinding.FragmentFrpcsBinding
import com.idormy.sms.forwarder.service.ForegroundService
import com.idormy.sms.forwarder.utils.ACTION_START
import com.idormy.sms.forwarder.utils.EVENT_FRPC_DELETE_CONFIG
import com.idormy.sms.forwarder.utils.EVENT_FRPC_RUNNING_ERROR
import com.idormy.sms.forwarder.utils.EVENT_FRPC_RUNNING_SUCCESS
import com.idormy.sms.forwarder.utils.EVENT_FRPC_UPDATE_CONFIG
import com.idormy.sms.forwarder.utils.FRPC_LIB_VERSION
import com.idormy.sms.forwarder.utils.FrpcUtils
import com.idormy.sms.forwarder.utils.INTENT_FRPC_APPLY_FILE
import com.idormy.sms.forwarder.utils.INTENT_FRPC_EDIT_FILE
import com.idormy.sms.forwarder.utils.XToastUtils
import com.jeremyliao.liveeventbus.LiveEventBus
import com.scwang.smartrefresh.layout.api.RefreshLayout
import com.xuexiang.xaop.annotation.SingleClick
import com.xuexiang.xpage.annotation.Page
import com.xuexiang.xpage.base.XPageActivity
import com.xuexiang.xpage.core.PageOption
import com.xuexiang.xui.utils.ThemeUtils
import com.xuexiang.xui.utils.WidgetUtils
import com.xuexiang.xui.widget.actionbar.TitleBar
import com.xuexiang.xui.widget.dialog.LoadingDialog
import com.xuexiang.xutil.system.ClipboardUtils
import frpclib.Frpclib
import io.reactivex.CompletableObserver
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Page(name = "Frp内网穿透")
class FrpcFragment : BaseFragment<FragmentFrpcsBinding?>(), FrpcPagingAdapter.OnItemClickListener {

    private var titleBar: TitleBar? = null
    private var adapter = FrpcPagingAdapter(this)
    private val viewModel by viewModels<FrpcViewModel> { BaseViewModelFactory(context) }

    override fun viewBindingInflate(
        inflater: LayoutInflater,
        container: ViewGroup,
    ): FragmentFrpcsBinding {
        return FragmentFrpcsBinding.inflate(inflater, container, false)
    }

    override fun initTitle(): TitleBar? {
        titleBar = super.initTitle()!!.setImmersive(false)
        titleBar!!.setTitle(R.string.menu_frpc)
        titleBar!!.setActionTextColor(ThemeUtils.resolveColor(context, R.attr.colorAccent))
        titleBar!!.addAction(object : TitleBar.ImageAction(R.drawable.ic_logcat) {
            @SingleClick
            override fun performAction(view: View) {
                openNewPage(LogcatFragment::class.java)
            }
        })
        titleBar!!.addAction(object : TitleBar.ImageAction(R.drawable.ic_add) {
            @SingleClick
            override fun performAction(view: View) {
                FrpcUtils.getStringFromRaw(context!!, R.raw.frpc).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(object : Observer<String?> {
                    override fun onSubscribe(d: Disposable) {}
                    override fun onNext(content: String) {
                        LiveEventBus.get<Frpc>(INTENT_FRPC_EDIT_FILE).post(Frpc(config = content))
                        PageOption.to(FrpcEditFragment::class.java).setNewActivity(true).open((context as XPageActivity?)!!)
                    }

                    override fun onError(e: Throwable) {
                        e.message?.let { XToastUtils.error(it) }
                    }

                    override fun onComplete() {}
                })
            }
        })
        return titleBar
    }

    /**
     * 初始化控件
     */
    override fun initViews() {
        val virtualLayoutManager = VirtualLayoutManager(requireContext())
        binding!!.recyclerView.layoutManager = virtualLayoutManager
        val viewPool = RecycledViewPool()
        binding!!.recyclerView.setRecycledViewPool(viewPool)
        viewPool.setMaxRecycledViews(0, 10)

        binding!!.recyclerView.adapter = adapter
    }

    override fun initListeners() {
        //下拉刷新
        binding!!.refreshLayout.setOnRefreshListener { refreshLayout: RefreshLayout ->
            //adapter.refresh()
            lifecycleScope.launch {
                viewModel.allFrpc.collectLatest { adapter.submitData(it) }
            }
            refreshLayout.finishRefresh()
        }
        binding!!.refreshLayout.autoRefresh()

        //更新时间
        LiveEventBus.get(EVENT_FRPC_UPDATE_CONFIG, Frpc::class.java).observe(this) {
            adapter.refresh()
        }

        //删除事件
        LiveEventBus.get(EVENT_FRPC_DELETE_CONFIG, Frpc::class.java).observe(this) {
            adapter.refresh()
        }

        //运行出错时间
        LiveEventBus.get(EVENT_FRPC_RUNNING_ERROR, String::class.java).observe(this) {
            XToastUtils.error(getString(R.string.frpc_failed_to_run))
            adapter.refresh()
        }

        //运行成功
        LiveEventBus.get(EVENT_FRPC_RUNNING_SUCCESS, String::class.java).observe(this) {
            adapter.refresh()
        }
    }

    override fun onItemClicked(view: View?, item: Frpc) {
        when (val id = view?.id) {
            R.id.iv_copy -> {
                ClipboardUtils.copyText(item.uid)
                XToastUtils.info(String.format(getString(R.string.copied_to_clipboard), item.uid))
            }

            R.id.iv_play -> {
                if (!App.FrpclibInited) {
                    XToastUtils.error(String.format(getString(R.string.frpclib_download_title), FRPC_LIB_VERSION))
                    return
                }

                if (!ForegroundService.isRunning) {
                    val serviceIntent = Intent(requireContext(), ForegroundService::class.java)
                    serviceIntent.action = ACTION_START
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        requireContext().startForegroundService(serviceIntent)
                    } else {
                        requireContext().startService(serviceIntent)
                    }
                }

                if (Frpclib.isRunning(item.uid)) {
                    Frpclib.close(item.uid)
                    item.connecting = false
                    LiveEventBus.get<Frpc>(EVENT_FRPC_UPDATE_CONFIG).post(item)
                    return
                }

                FrpcUtils.waitService(ForegroundService::class.java.name, requireContext()).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(object : CompletableObserver {
                    var mLoadingDialog: LoadingDialog = WidgetUtils.getLoadingDialog(context!!).setIconScale(0.4f).setLoadingSpeed(8)

                    override fun onSubscribe(d: Disposable) {
                        mLoadingDialog.setLoadingIcon(R.drawable.ic_menu_frpc)
                        mLoadingDialog.updateMessage(R.string.tipWaitService)
                        mLoadingDialog.show()
                    }

                    override fun onComplete() {
                        mLoadingDialog.dismiss()
                        mLoadingDialog.recycle()
                        LiveEventBus.get<String>(INTENT_FRPC_APPLY_FILE).postAcrossProcess(item.uid)
                        item.connecting = true
                        LiveEventBus.get<Frpc>(EVENT_FRPC_UPDATE_CONFIG).post(item)
                    }

                    override fun onError(e: Throwable) {
                        mLoadingDialog.dismiss()
                        mLoadingDialog.recycle()
                        e.message?.let { XToastUtils.error(it) }
                        item.connecting = false
                        LiveEventBus.get<Frpc>(EVENT_FRPC_UPDATE_CONFIG).post(item)
                    }
                })
            }

            else -> {
                if (!App.FrpclibInited) {
                    XToastUtils.error(String.format(getString(R.string.frpclib_download_title), FRPC_LIB_VERSION))
                    return
                }

                //编辑或删除需要先停止客户端
                if (Frpclib.isRunning(item.uid)) {
                    XToastUtils.warning(R.string.tipServiceRunning)
                    return
                }
                when (id) {
                    R.id.iv_edit -> {
                        LiveEventBus.get<Frpc>(INTENT_FRPC_EDIT_FILE).post(item)
                        openNewPage(FrpcEditFragment::class.java)
                    }

                    R.id.iv_delete -> {
                        try {
                            viewModel.delete(item)
                            LiveEventBus.get<Frpc>(EVENT_FRPC_DELETE_CONFIG).post(item)
                            XToastUtils.success(getString(R.string.successfully_deleted))
                        } catch (e: Exception) {
                            e.message?.let { XToastUtils.error(it) }
                        }
                    }
                }
            }
        }
    }

    override fun onItemRemove(view: View?, id: Int) {}
}