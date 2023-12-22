package com.idormy.sms.forwarder.fragment.action

import android.annotation.SuppressLint
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.google.gson.Gson
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.adapter.spinner.FrpcAdapterItem
import com.idormy.sms.forwarder.adapter.spinner.FrpcSpinnerAdapter
import com.idormy.sms.forwarder.core.BaseFragment
import com.idormy.sms.forwarder.core.Core
import com.idormy.sms.forwarder.database.entity.Frpc
import com.idormy.sms.forwarder.databinding.FragmentTasksActionFrpcBinding
import com.idormy.sms.forwarder.entity.action.FrpcSetting
import com.idormy.sms.forwarder.utils.KEY_BACK_DATA_ACTION
import com.idormy.sms.forwarder.utils.KEY_BACK_DESCRIPTION_ACTION
import com.idormy.sms.forwarder.utils.KEY_EVENT_DATA_ACTION
import com.idormy.sms.forwarder.utils.KEY_TEST_ACTION
import com.idormy.sms.forwarder.utils.Log
import com.idormy.sms.forwarder.utils.STATUS_ON
import com.idormy.sms.forwarder.utils.TASK_ACTION_FRPC
import com.idormy.sms.forwarder.utils.XToastUtils
import com.jeremyliao.liveeventbus.LiveEventBus
import com.xuexiang.xaop.annotation.SingleClick
import com.xuexiang.xpage.annotation.Page
import com.xuexiang.xrouter.annotation.AutoWired
import com.xuexiang.xrouter.launcher.XRouter
import com.xuexiang.xui.utils.CountDownButtonHelper
import com.xuexiang.xui.widget.actionbar.TitleBar
import com.xuexiang.xutil.resource.ResUtils.getDrawable
import io.reactivex.SingleObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers

@Page(name = "Frpc")
@Suppress("PrivatePropertyName")
class FrpcFragment : BaseFragment<FragmentTasksActionFrpcBinding?>(), View.OnClickListener {

    private val TAG: String = FrpcFragment::class.java.simpleName
    private var titleBar: TitleBar? = null
    private var mCountDownHelper: CountDownButtonHelper? = null

    //当前发送通道
    private var frpcUid = ""
    private var frpcListSelected: MutableList<Frpc> = mutableListOf()
    private var frpcItemMap = HashMap<String, LinearLayout>(2)

    //发送通道列表
    private var frpcListAll: MutableList<Frpc> = mutableListOf()
    private val frpcSpinnerList = ArrayList<FrpcAdapterItem>()
    private lateinit var frpcSpinnerAdapter: FrpcSpinnerAdapter<*>

    @JvmField
    @AutoWired(name = KEY_EVENT_DATA_ACTION)
    var eventData: String? = null

    override fun initArgs() {
        XRouter.getInstance().inject(this)
    }

    override fun viewBindingInflate(
        inflater: LayoutInflater,
        container: ViewGroup,
    ): FragmentTasksActionFrpcBinding {
        return FragmentTasksActionFrpcBinding.inflate(inflater, container, false)
    }

    override fun initTitle(): TitleBar? {
        titleBar = super.initTitle()!!.setImmersive(false).setTitle(R.string.task_frpc)
        return titleBar
    }

    /**
     * 初始化控件
     */
    override fun initViews() {
        //测试按钮增加倒计时，避免重复点击
        mCountDownHelper = CountDownButtonHelper(binding!!.btnTest, 3)
        mCountDownHelper!!.setOnCountDownListener(object : CountDownButtonHelper.OnCountDownListener {
            override fun onCountDown(time: Int) {
                binding!!.btnTest.text = String.format(getString(R.string.seconds_n), time)
            }

            override fun onFinished() {
                binding!!.btnTest.text = getString(R.string.test)
            }
        })

        Log.d(TAG, "initViews eventData:$eventData")
        if (eventData != null) {
            val settingVo = Gson().fromJson(eventData, FrpcSetting::class.java)
            binding!!.rgFrpcState.check(if (settingVo.action == "start") R.id.rb_start_server else R.id.rb_stop_server)
            Log.d(TAG, "initViews settingVo:$settingVo")
        }

        //初始化发送通道下拉框
        initFrpcSpinner()
    }

    @SuppressLint("SetTextI18n")
    override fun initListeners() {
        binding!!.btnTest.setOnClickListener(this)
        binding!!.btnDel.setOnClickListener(this)
        binding!!.btnSave.setOnClickListener(this)
        LiveEventBus.get(KEY_TEST_ACTION, String::class.java).observe(this) {
            mCountDownHelper?.finish()

            if (it == "success") {
                XToastUtils.success("测试通过", 30000)
            } else {
                XToastUtils.error(it, 30000)
            }
        }
    }

    @SingleClick
    override fun onClick(v: View) {
        try {
            when (v.id) {
                R.id.btn_test -> {
                    mCountDownHelper?.start()
                    Thread {
                        try {
                            val settingVo = checkSetting()
                            Log.d(TAG, settingVo.toString())
                            LiveEventBus.get(KEY_TEST_ACTION, String::class.java).post("success")
                        } catch (e: Exception) {
                            LiveEventBus.get(KEY_TEST_ACTION, String::class.java).post(e.message.toString())
                            e.printStackTrace()
                            Log.e(TAG, "onClick error: ${e.message}")
                        }
                    }.start()
                    return
                }

                R.id.btn_del -> {
                    popToBack()
                    return
                }

                R.id.btn_save -> {
                    val settingVo = checkSetting()
                    val intent = Intent()
                    intent.putExtra(KEY_BACK_DESCRIPTION_ACTION, settingVo.description)
                    intent.putExtra(KEY_BACK_DATA_ACTION, Gson().toJson(settingVo))
                    setFragmentResult(TASK_ACTION_FRPC, intent)
                    popToBack()
                    return
                }
            }
        } catch (e: Exception) {
            XToastUtils.error(e.message.toString(), 30000)
            e.printStackTrace()
            Log.e(TAG, "onClick error: ${e.message}")
        }
    }

    //初始化发送通道下拉框
    @SuppressLint("SetTextI18n")
    private fun initFrpcSpinner() {
        Core.frpc.getAll().subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(object : SingleObserver<List<Frpc>> {
            override fun onSubscribe(d: Disposable) {}

            override fun onError(e: Throwable) {
                e.printStackTrace()
                Log.e(TAG, "initFrpcSpinner error: ${e.message}")
            }

            override fun onSuccess(frpcList: List<Frpc>) {
                if (frpcList.isEmpty()) {
                    XToastUtils.error(R.string.add_frpc_first)
                    return
                }

                frpcListAll = frpcList as MutableList<Frpc>
                for (frpc in frpcList) {
                    val name = if (frpc.name.length > 20) frpc.name.substring(0, 19) else frpc.name
                    frpcSpinnerList.add(FrpcAdapterItem(name, getDrawable(R.drawable.auto_task_icon_frpc), frpc.uid, frpc.autorun))
                }
                frpcSpinnerAdapter = FrpcSpinnerAdapter(frpcSpinnerList)
                    .setIsFilterKey(true).setFilterColor("#EF5362").setBackgroundSelector(R.drawable.selector_custom_spinner_bg)
                binding!!.spFrpc.setAdapter(frpcSpinnerAdapter)

                if (frpcListSelected.isNotEmpty()) {
                    for (frpc in frpcListSelected) {
                        for (frpcItem in frpcSpinnerList) {
                            if (frpc.uid == frpcItem.uid) {
                                addFrpcItemLinearLayout(frpcItemMap, binding!!.layoutFrpcs, frpcItem)
                            }
                        }
                    }
                }
            }
        })
        binding!!.spFrpc.setOnItemClickListener { _: AdapterView<*>, _: View, position: Int, _: Long ->
            try {
                val frpc = frpcSpinnerAdapter.getItemSource(position) as FrpcAdapterItem
                frpcUid = frpc.uid
                if (frpcUid.isNotEmpty()) {
                    frpcListSelected.forEach {
                        if (frpcUid == it.uid) {
                            XToastUtils.warning(getString(R.string.frpc_contains_tips))
                            return@setOnItemClickListener
                        }
                    }
                    frpcListAll.forEach {
                        if (frpcUid == it.uid) {
                            frpcListSelected.add(it)
                            addFrpcItemLinearLayout(frpcItemMap, binding!!.layoutFrpcs, frpc)
                        }
                    }

                    /*if (STATUS_OFF == frpc.status) {
                        XToastUtils.warning(getString(R.string.frpc_disabled_tips))
                    }*/
                }
            } catch (e: Exception) {
                XToastUtils.error(e.message.toString())
            }
        }
    }

    /**
     * 动态增删Frpc
     *
     * @param frpcItemMap          管理item的map，用于删除指定header
     * @param layoutFrpcs          需要挂载item的LinearLayout
     * @param frpc                 FrpcAdapterItem
     */
    @SuppressLint("SetTextI18n")
    private fun addFrpcItemLinearLayout(
        frpcItemMap: MutableMap<String, LinearLayout>, layoutFrpcs: LinearLayout, frpc: FrpcAdapterItem
    ) {
        val layoutFrpcItem = View.inflate(requireContext(), R.layout.item_add_frpc, null) as LinearLayout
        val ivRemoveFrpc = layoutFrpcItem.findViewById<ImageView>(R.id.iv_remove_frpc)
        val ivFrpcImage = layoutFrpcItem.findViewById<ImageView>(R.id.iv_frpc_image)
        val ivFrpcStatus = layoutFrpcItem.findViewById<ImageView>(R.id.iv_frpc_status)
        val tvFrpcName = layoutFrpcItem.findViewById<TextView>(R.id.tv_frpc_name)

        ivFrpcImage.setImageDrawable(frpc.icon)
        ivFrpcStatus.setImageDrawable(getDrawable(if (STATUS_ON == frpc.autorun) R.drawable.ic_autorun else R.drawable.ic_manual))
        val frpcItemId = frpc.uid
        tvFrpcName.text = frpc.title

        ivRemoveFrpc.tag = frpcItemId
        ivRemoveFrpc.setOnClickListener { view2: View ->
            val tagId = view2.tag
            layoutFrpcs.removeView(frpcItemMap[tagId])
            frpcItemMap.remove(tagId)
            //frpcListSelected.removeIf { it.uid == tagId }
            for (it in frpcListSelected) {
                if (it.uid == tagId) {
                    frpcListSelected -= it
                    break
                }
            }
            Log.d(TAG, frpcListSelected.count().toString())
            Log.d(TAG, frpcListSelected.toString())
            if (frpcListSelected.isEmpty()) frpcUid = ""
        }
        layoutFrpcs.addView(layoutFrpcItem)
        frpcItemMap[frpcItemId] = layoutFrpcItem
    }

    //检查设置
    @SuppressLint("SetTextI18n")
    private fun checkSetting(): FrpcSetting {
        val description = StringBuilder()
        val action = if (binding!!.rgFrpcState.checkedRadioButtonId == R.id.rb_start_server) {
            description.append(getString(R.string.enable))
            "start"
        } else {
            description.append(getString(R.string.disable))
            "stop"
        }
        if (frpcListSelected.isEmpty()) {
            description.append(getString(R.string.all_auto_started_frpc))
        } else {
            description.append(getString(R.string.specified_frpc)).append(": ")
            description.append(frpcListSelected.joinToString(",") { it.name })
        }

        return FrpcSetting(description.toString(), action, frpcListSelected)
    }
}