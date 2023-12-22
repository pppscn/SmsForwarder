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
import com.idormy.sms.forwarder.adapter.spinner.SenderAdapterItem
import com.idormy.sms.forwarder.adapter.spinner.SenderSpinnerAdapter
import com.idormy.sms.forwarder.core.BaseFragment
import com.idormy.sms.forwarder.core.Core
import com.idormy.sms.forwarder.database.entity.Sender
import com.idormy.sms.forwarder.databinding.FragmentTasksActionSenderBinding
import com.idormy.sms.forwarder.entity.action.SenderSetting
import com.idormy.sms.forwarder.utils.KEY_BACK_DATA_ACTION
import com.idormy.sms.forwarder.utils.KEY_BACK_DESCRIPTION_ACTION
import com.idormy.sms.forwarder.utils.KEY_EVENT_DATA_ACTION
import com.idormy.sms.forwarder.utils.KEY_TEST_ACTION
import com.idormy.sms.forwarder.utils.Log
import com.idormy.sms.forwarder.utils.STATUS_OFF
import com.idormy.sms.forwarder.utils.TASK_ACTION_SENDER
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

@Page(name = "Sender")
@Suppress("PrivatePropertyName")
class SenderFragment : BaseFragment<FragmentTasksActionSenderBinding?>(), View.OnClickListener {

    private val TAG: String = SenderFragment::class.java.simpleName
    private var titleBar: TitleBar? = null
    private var mCountDownHelper: CountDownButtonHelper? = null

    //当前发送通道
    private var senderId = 0L
    private var senderListSelected: MutableList<Sender> = mutableListOf()
    private var senderItemMap = HashMap<Long, LinearLayout>(2)

    //发送通道列表
    private var senderListAll: MutableList<Sender> = mutableListOf()
    private val senderSpinnerList = ArrayList<SenderAdapterItem>()
    private lateinit var senderSpinnerAdapter: SenderSpinnerAdapter<*>

    @JvmField
    @AutoWired(name = KEY_EVENT_DATA_ACTION)
    var eventData: String? = null

    override fun initArgs() {
        XRouter.getInstance().inject(this)
    }

    override fun viewBindingInflate(
        inflater: LayoutInflater,
        container: ViewGroup,
    ): FragmentTasksActionSenderBinding {
        return FragmentTasksActionSenderBinding.inflate(inflater, container, false)
    }

    override fun initTitle(): TitleBar? {
        titleBar = super.initTitle()!!.setImmersive(false).setTitle(R.string.task_sender)
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
            val settingVo = Gson().fromJson(eventData, SenderSetting::class.java)
            binding!!.rgStatus.check(if (settingVo.status == 1) R.id.rb_status_enable else R.id.rb_status_disable)
            Log.d(TAG, settingVo.senderList.toString())
            settingVo.senderList.forEach {
                senderId = it.id
                senderListSelected.add(it)
            }
            Log.d(TAG, "initViews settingVo:$settingVo")
        }

        //初始化发送通道下拉框
        initSenderSpinner()
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
                    setFragmentResult(TASK_ACTION_SENDER, intent)
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
    private fun initSenderSpinner() {
        Core.sender.getAll().subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(object : SingleObserver<List<Sender>> {
            override fun onSubscribe(d: Disposable) {}

            override fun onError(e: Throwable) {
                e.printStackTrace()
                Log.e(TAG, "initSenderSpinner error: ${e.message}")
            }

            override fun onSuccess(senderList: List<Sender>) {
                if (senderList.isEmpty()) {
                    XToastUtils.error(R.string.add_sender_first)
                    return
                }

                senderListAll = senderList as MutableList<Sender>
                for (sender in senderList) {
                    val name = if (sender.name.length > 20) sender.name.substring(0, 19) else sender.name
                    senderSpinnerList.add(SenderAdapterItem(name, getDrawable(sender.imageId), sender.id, sender.status))
                }
                senderSpinnerAdapter = SenderSpinnerAdapter(senderSpinnerList)
                    .setIsFilterKey(true).setFilterColor("#EF5362").setBackgroundSelector(R.drawable.selector_custom_spinner_bg)
                binding!!.spSender.setAdapter(senderSpinnerAdapter)

                if (senderListSelected.isNotEmpty()) {
                    for (sender in senderListSelected) {
                        for (senderItem in senderSpinnerList) {
                            if (sender.id == senderItem.id) {
                                addSenderItemLinearLayout(senderItemMap, binding!!.layoutSenders, senderItem)
                            }
                        }
                    }
                }
            }
        })
        binding!!.spSender.setOnItemClickListener { _: AdapterView<*>, _: View, position: Int, _: Long ->
            try {
                val sender = senderSpinnerAdapter.getItemSource(position) as SenderAdapterItem
                senderId = sender.id!!
                if (senderId > 0L) {
                    senderListSelected.forEach {
                        if (senderId == it.id) {
                            XToastUtils.warning(getString(R.string.sender_contains_tips))
                            return@setOnItemClickListener
                        }
                    }
                    senderListAll.forEach {
                        if (senderId == it.id) {
                            senderListSelected.add(it)
                            addSenderItemLinearLayout(senderItemMap, binding!!.layoutSenders, sender)
                        }
                    }

                    /*if (STATUS_OFF == sender.status) {
                        XToastUtils.warning(getString(R.string.sender_disabled_tips))
                    }*/
                }
            } catch (e: Exception) {
                XToastUtils.error(e.message.toString())
            }
        }
    }

    /**
     * 动态增删Sender
     *
     * @param senderItemMap          管理item的map，用于删除指定header
     * @param layoutSenders          需要挂载item的LinearLayout
     * @param sender                 SenderAdapterItem
     */
    @SuppressLint("SetTextI18n")
    private fun addSenderItemLinearLayout(
        senderItemMap: MutableMap<Long, LinearLayout>, layoutSenders: LinearLayout, sender: SenderAdapterItem
    ) {
        val layoutSenderItem = View.inflate(requireContext(), R.layout.item_add_sender, null) as LinearLayout
        val ivRemoveSender = layoutSenderItem.findViewById<ImageView>(R.id.iv_remove_sender)
        val ivSenderImage = layoutSenderItem.findViewById<ImageView>(R.id.iv_sender_image)
        val ivSenderStatus = layoutSenderItem.findViewById<ImageView>(R.id.iv_sender_status)
        val tvSenderName = layoutSenderItem.findViewById<TextView>(R.id.tv_sender_name)

        ivSenderImage.setImageDrawable(sender.icon)
        ivSenderStatus.setImageDrawable(getDrawable(if (STATUS_OFF == sender.status) R.drawable.ic_stop else R.drawable.ic_start))
        val senderItemId = sender.id as Long
        tvSenderName.text = "ID-$senderItemId：${sender.title}"

        ivRemoveSender.tag = senderItemId
        ivRemoveSender.setOnClickListener { view2: View ->
            val tagId = view2.tag as Long
            layoutSenders.removeView(senderItemMap[tagId])
            senderItemMap.remove(tagId)
            //senderListSelected.removeIf { it.id == tagId }
            for (it in senderListSelected) {
                if (it.id == tagId) {
                    senderListSelected -= it
                    break
                }
            }
            Log.d(TAG, senderListSelected.count().toString())
            Log.d(TAG, senderListSelected.toString())
            if (senderListSelected.isEmpty()) senderId = 0L
        }
        layoutSenders.addView(layoutSenderItem)
        senderItemMap[senderItemId] = layoutSenderItem
    }

    //检查设置
    @SuppressLint("SetTextI18n")
    private fun checkSetting(): SenderSetting {
        if (senderListSelected.isEmpty() || senderId == 0L) {
            throw Exception(getString(R.string.new_sender_first))
        }

        val description = StringBuilder()
        val status: Int
        if (binding!!.rgStatus.checkedRadioButtonId == R.id.rb_status_enable) {
            status = 1
            description.append(getString(R.string.enable))
        } else {
            status = 0
            description.append(getString(R.string.disable))
        }
        description.append(getString(R.string.menu_senders)).append(", ").append(getString(R.string.specified_sender)).append(": ")
        description.append(senderListSelected.joinToString(",") { it.name })

        return SenderSetting(description.toString(), status, senderListSelected)
    }
}