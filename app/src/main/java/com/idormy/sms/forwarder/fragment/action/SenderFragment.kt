package com.idormy.sms.forwarder.fragment.action

import android.annotation.SuppressLint
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.gson.Gson
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.adapter.SenderRecyclerAdapter
import com.idormy.sms.forwarder.adapter.base.ItemMoveCallback
import com.idormy.sms.forwarder.adapter.spinner.SenderSpinnerAdapter
import com.idormy.sms.forwarder.adapter.spinner.SenderSpinnerItem
import com.idormy.sms.forwarder.core.BaseFragment
import com.idormy.sms.forwarder.core.Core
import com.idormy.sms.forwarder.database.entity.Sender
import com.idormy.sms.forwarder.databinding.FragmentTasksActionSenderBinding
import com.idormy.sms.forwarder.entity.MsgInfo
import com.idormy.sms.forwarder.entity.TaskSetting
import com.idormy.sms.forwarder.entity.action.SenderSetting
import com.idormy.sms.forwarder.utils.KEY_BACK_DATA_ACTION
import com.idormy.sms.forwarder.utils.KEY_BACK_DESCRIPTION_ACTION
import com.idormy.sms.forwarder.utils.KEY_EVENT_DATA_ACTION
import com.idormy.sms.forwarder.utils.Log
import com.idormy.sms.forwarder.utils.TASK_ACTION_SENDER
import com.idormy.sms.forwarder.utils.TaskWorker
import com.idormy.sms.forwarder.utils.XToastUtils
import com.idormy.sms.forwarder.workers.ActionWorker
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
import java.util.Date

@Page(name = "Sender")
@Suppress("PrivatePropertyName", "DEPRECATION")
class SenderFragment : BaseFragment<FragmentTasksActionSenderBinding?>(), View.OnClickListener {

    private val TAG: String = SenderFragment::class.java.simpleName
    private var titleBar: TitleBar? = null
    private var mCountDownHelper: CountDownButtonHelper? = null

    //所有发送通道下拉框
    private var senderListAll = mutableListOf<Sender>()
    private val senderSpinnerList = mutableListOf<SenderSpinnerItem>()
    private lateinit var senderSpinnerAdapter: SenderSpinnerAdapter<*>

    //已选发送通道列表
    private var senderId = 0L
    private var senderListSelected = mutableListOf<Sender>()
    private lateinit var senderRecyclerView: RecyclerView
    private lateinit var senderRecyclerAdapter: SenderRecyclerAdapter

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
    @SuppressLint("NotifyDataSetChanged")
    override fun initViews() {
        //测试按钮增加倒计时，避免重复点击
        mCountDownHelper = CountDownButtonHelper(binding!!.btnTest, 1)
        mCountDownHelper!!.setOnCountDownListener(object : CountDownButtonHelper.OnCountDownListener {
            override fun onCountDown(time: Int) {
                binding!!.btnTest.text = String.format(getString(R.string.seconds_n), time)
            }

            override fun onFinished() {
                binding!!.btnTest.text = getString(R.string.test)
                //获取发送通道列表
                getSenderList()
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
        initSender()
    }

    override fun onDestroyView() {
        if (mCountDownHelper != null) mCountDownHelper!!.recycle()
        super.onDestroyView()
    }

    @SuppressLint("SetTextI18n")
    override fun initListeners() {
        binding!!.btnTest.setOnClickListener(this)
        binding!!.btnDel.setOnClickListener(this)
        binding!!.btnSave.setOnClickListener(this)
    }

    @SingleClick
    override fun onClick(v: View) {
        try {
            when (v.id) {
                R.id.btn_test -> {
                    mCountDownHelper?.start()
                    try {
                        val settingVo = checkSetting()
                        Log.d(TAG, settingVo.toString())
                        val taskAction = TaskSetting(TASK_ACTION_SENDER, getString(R.string.task_sender), settingVo.description, Gson().toJson(settingVo), requestCode)
                        val taskActionsJson = Gson().toJson(arrayListOf(taskAction))
                        val msgInfo = MsgInfo("task", getString(R.string.task_sender), settingVo.description, Date(), getString(R.string.task_sender))
                        val actionData = Data.Builder().putLong(TaskWorker.TASK_ID, 0).putString(TaskWorker.TASK_ACTIONS, taskActionsJson).putString(TaskWorker.MSG_INFO, Gson().toJson(msgInfo)).build()
                        val actionRequest = OneTimeWorkRequestBuilder<ActionWorker>().setInputData(actionData).build()
                        WorkManager.getInstance().enqueue(actionRequest)
                    } catch (e: Exception) {
                        mCountDownHelper?.finish()
                        e.printStackTrace()
                        Log.e(TAG, "onClick error: ${e.message}")
                        XToastUtils.error(e.message.toString(), 30000)
                    }
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

    //初始化发送通道
    @SuppressLint("SetTextI18n", "NotifyDataSetChanged")
    private fun initSender() {
        //初始化发送通道下拉框
        binding!!.spSender.setOnItemClickListener { _: AdapterView<*>, _: View, position: Int, _: Long ->
            try {
                val item = senderSpinnerAdapter.getItemSource(position) as SenderSpinnerItem
                senderId = item.id!!
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
                        }
                    }
                    senderRecyclerAdapter.notifyDataSetChanged()
                }
            } catch (e: Exception) {
                XToastUtils.error(e.message.toString())
            }
        }

        // 初始化已选发送通道列表 RecyclerView 和 Adapter
        senderRecyclerView = binding!!.recyclerSenders
        senderRecyclerAdapter = SenderRecyclerAdapter(senderListSelected, { position ->
            senderListSelected.removeAt(position)
            senderRecyclerAdapter.notifyItemRemoved(position)
            senderRecyclerAdapter.notifyItemRangeChanged(position, senderListSelected.size) // 更新索引
        })
        senderRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = senderRecyclerAdapter
        }
        val senderMoveCallback = ItemMoveCallback(object : ItemMoveCallback.Listener {
            override fun onItemMove(fromPosition: Int, toPosition: Int) {
                Log.d(TAG, "onItemMove: $fromPosition $toPosition")
                senderRecyclerAdapter.onItemMove(fromPosition, toPosition)
                senderListSelected = senderRecyclerAdapter.itemList
            }

            override fun onDragFinished() {
                senderListSelected = senderRecyclerAdapter.itemList
                //senderRecyclerAdapter.notifyDataSetChanged()
                Log.d(TAG, "onDragFinished: $senderListSelected")
            }
        })
        val senderTouchHelper = ItemTouchHelper(senderMoveCallback)
        senderTouchHelper.attachToRecyclerView(senderRecyclerView)
        senderRecyclerAdapter.setTouchHelper(senderTouchHelper)

        //获取发送通道列表
        getSenderList()
    }

    //获取发送通道列表
    private fun getSenderList() {
        Core.sender.getAll().subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(object : SingleObserver<List<Sender>> {
            override fun onSubscribe(d: Disposable) {}

            override fun onError(e: Throwable) {
                e.printStackTrace()
                Log.e(TAG, "getSenderList error: ${e.message}")
            }

            @SuppressLint("NotifyDataSetChanged")
            override fun onSuccess(senderList: List<Sender>) {
                if (senderList.isEmpty()) {
                    XToastUtils.error(R.string.add_sender_first)
                    return
                }

                senderSpinnerList.clear()
                senderListAll = senderList as MutableList<Sender>
                for (sender in senderList) {
                    val name = if (sender.name.length > 20) sender.name.substring(0, 19) else sender.name
                    senderSpinnerList.add(SenderSpinnerItem(name, getDrawable(sender.imageId), sender.id, sender.status))
                }
                senderSpinnerAdapter = SenderSpinnerAdapter(senderSpinnerList).setIsFilterKey(true).setFilterColor("#EF5362").setBackgroundSelector(R.drawable.selector_custom_spinner_bg)
                binding!!.spSender.setAdapter(senderSpinnerAdapter)
                //senderSpinnerAdapter.notifyDataSetChanged()

                //更新senderListSelected的状态与名称
                senderListSelected.forEach {
                    senderListAll.forEach { sender ->
                        if (it.id == sender.id) {
                            it.name = sender.name
                            it.status = sender.status
                        }
                    }
                }
                senderRecyclerAdapter.notifyDataSetChanged()

            }
        })
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