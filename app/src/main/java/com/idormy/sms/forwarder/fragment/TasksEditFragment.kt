package com.idormy.sms.forwarder.fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.gson.Gson
import com.idormy.sms.forwarder.App
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.adapter.TaskSettingAdapter
import com.idormy.sms.forwarder.adapter.WidgetItemAdapter
import com.idormy.sms.forwarder.adapter.base.ItemMoveCallback
import com.idormy.sms.forwarder.core.BaseFragment
import com.idormy.sms.forwarder.core.Core
import com.idormy.sms.forwarder.database.entity.Task
import com.idormy.sms.forwarder.databinding.FragmentTasksEditBinding
import com.idormy.sms.forwarder.entity.TaskSetting
import com.idormy.sms.forwarder.entity.condition.CronSetting
import com.idormy.sms.forwarder.service.LocationService
import com.idormy.sms.forwarder.utils.*
import com.idormy.sms.forwarder.utils.task.CronJobScheduler
import com.xuexiang.xaop.annotation.SingleClick
import com.xuexiang.xpage.annotation.Page
import com.xuexiang.xpage.base.XPageFragment
import com.xuexiang.xpage.core.PageOption
import com.xuexiang.xpage.enums.CoreAnim
import com.xuexiang.xpage.model.PageInfo
import com.xuexiang.xrouter.annotation.AutoWired
import com.xuexiang.xrouter.launcher.XRouter
import com.xuexiang.xui.adapter.recyclerview.RecyclerViewHolder
import com.xuexiang.xui.utils.DensityUtils
import com.xuexiang.xui.utils.WidgetUtils
import com.xuexiang.xui.widget.actionbar.TitleBar
import com.xuexiang.xui.widget.alpha.XUIAlphaTextView
import com.xuexiang.xui.widget.dialog.materialdialog.DialogAction
import com.xuexiang.xui.widget.dialog.materialdialog.MaterialDialog
import gatewayapps.crondroid.CronExpression
import io.reactivex.SingleObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.*
import java.util.*


@Page(name = "自动任务·编辑器")
@Suppress("PrivatePropertyName", "DEPRECATION", "UNUSED_PARAMETER", "EmptyMethod", "NotifyDataSetChanged")
class TasksEditFragment : BaseFragment<FragmentTasksEditBinding?>(), View.OnClickListener, RecyclerViewHolder.OnItemClickListener<PageInfo> {

    private val TAG: String = TasksEditFragment::class.java.simpleName
    private val that = this
    private var titleBar: TitleBar? = null
    private val dialog: BottomSheetDialog by lazy { BottomSheetDialog(requireContext()) }

    @JvmField
    @AutoWired(name = KEY_TASK_ID)
    var taskId: Long = 0

    @JvmField
    @AutoWired(name = KEY_TASK_TYPE)
    var taskType: Int = 0

    @JvmField
    @AutoWired(name = KEY_TASK_CLONE)
    var isClone: Boolean = false

    private lateinit var conditionsRecyclerView: RecyclerView
    private lateinit var conditionsAdapter: TaskSettingAdapter
    private var conditionsList = mutableListOf<TaskSetting>()

    private lateinit var actionsRecyclerView: RecyclerView
    private lateinit var actionsAdapter: TaskSettingAdapter
    private var actionsList = mutableListOf<TaskSetting>()

    private var TASK_CONDITION_FRAGMENT_LIST = listOf(
        PageInfo(
            getString(R.string.task_cron),
            "com.idormy.sms.forwarder.fragment.condition.CronFragment",
            "",
            CoreAnim.slide,
            R.drawable.auto_task_icon_custom_time,
        ),
        PageInfo(
            getString(R.string.task_to_address),
            "com.idormy.sms.forwarder.fragment.condition.ToAddressFragment",
            "",
            CoreAnim.slide,
            R.drawable.auto_task_icon_to_address,
        ),
        PageInfo(
            getString(R.string.task_leave_address),
            "com.idormy.sms.forwarder.fragment.condition.LeaveAddressFragment",
            "",
            CoreAnim.slide,
            R.drawable.auto_task_icon_leave_address,
        ),
        PageInfo(
            getString(R.string.task_network),
            "com.idormy.sms.forwarder.fragment.condition.NetworkFragment",
            "",
            CoreAnim.slide,
            R.drawable.auto_task_icon_network
        ),
        PageInfo(
            getString(R.string.task_sim),
            "com.idormy.sms.forwarder.fragment.condition.SimFragment",
            "",
            CoreAnim.slide,
            R.drawable.auto_task_icon_sim
        ),
        PageInfo(
            getString(R.string.task_battery),
            "com.idormy.sms.forwarder.fragment.condition.BatteryFragment",
            "",
            CoreAnim.slide,
            R.drawable.auto_task_icon_battery
        ),
        PageInfo(
            getString(R.string.task_charge),
            "com.idormy.sms.forwarder.fragment.condition.ChargeFragment",
            "",
            CoreAnim.slide,
            R.drawable.auto_task_icon_charge
        ),
        PageInfo(
            getString(R.string.task_lock_screen),
            "com.idormy.sms.forwarder.fragment.condition.LockScreenFragment",
            "",
            CoreAnim.slide,
            R.drawable.auto_task_icon_lock_screen
        ),
        PageInfo(
            getString(R.string.task_sms),
            "com.idormy.sms.forwarder.fragment.condition.MsgFragment",
            "sms",
            CoreAnim.slide,
            R.drawable.auto_task_icon_sms
        ),
        PageInfo(
            getString(R.string.task_call),
            "com.idormy.sms.forwarder.fragment.condition.MsgFragment",
            "call",
            CoreAnim.slide,
            R.drawable.auto_task_icon_incall
        ),
        PageInfo(
            getString(R.string.task_app),
            "com.idormy.sms.forwarder.fragment.condition.MsgFragment",
            "app",
            CoreAnim.slide,
            R.drawable.auto_task_icon_start_activity
        ),
        PageInfo(
            getString(R.string.task_bluetooth),
            "com.idormy.sms.forwarder.fragment.condition.BluetoothFragment",
            "",
            CoreAnim.slide,
            R.drawable.auto_task_icon_bluetooth
        ),
    )

    private var TASK_ACTION_FRAGMENT_LIST = listOf(
        PageInfo(
            getString(R.string.task_sendsms),
            "com.idormy.sms.forwarder.fragment.action.SendSmsFragment",
            "",
            CoreAnim.slide,
            R.drawable.auto_task_icon_sms
        ),
        PageInfo(
            getString(R.string.task_notification),
            "com.idormy.sms.forwarder.fragment.action.NotificationFragment",
            "",
            CoreAnim.slide,
            R.drawable.auto_task_icon_notification,
        ),
        PageInfo(
            getString(R.string.task_cleaner),
            "com.idormy.sms.forwarder.fragment.action.CleanerFragment",
            "",
            CoreAnim.slide,
            R.drawable.auto_task_icon_cleaner
        ),
        PageInfo(
            getString(R.string.task_settings),
            "com.idormy.sms.forwarder.fragment.action.SettingsFragment",
            "",
            CoreAnim.slide,
            R.drawable.auto_task_icon_settings
        ),
        PageInfo(
            getString(R.string.task_frpc),
            "com.idormy.sms.forwarder.fragment.action.FrpcFragment",
            "",
            CoreAnim.slide,
            R.drawable.auto_task_icon_frpc
        ),
        PageInfo(
            getString(R.string.task_http_server),
            "com.idormy.sms.forwarder.fragment.action.HttpServerFragment",
            "",
            CoreAnim.slide,
            R.drawable.auto_task_icon_http_server
        ),
        PageInfo(
            getString(R.string.task_rule),
            "com.idormy.sms.forwarder.fragment.action.RuleFragment",
            "",
            CoreAnim.slide,
            R.drawable.auto_task_icon_rule
        ),
        PageInfo(
            getString(R.string.task_sender),
            "com.idormy.sms.forwarder.fragment.action.SenderFragment",
            "",
            CoreAnim.slide,
            R.drawable.auto_task_icon_sender
        ),
        PageInfo(
            getString(R.string.task_alarm),
            "com.idormy.sms.forwarder.fragment.action.AlarmFragment",
            "",
            CoreAnim.slide,
            R.drawable.auto_task_icon_alarm
        ),
        PageInfo(
            getString(R.string.task_resend),
            "com.idormy.sms.forwarder.fragment.action.ResendFragment",
            "",
            CoreAnim.slide,
            R.drawable.auto_task_icon_resend
        ),
        PageInfo(
            getString(R.string.task_task),
            "com.idormy.sms.forwarder.fragment.action.TaskActionFragment",
            "",
            CoreAnim.slide,
            R.drawable.auto_task_icon_task
        ),
    )

    override fun initArgs() {
        XRouter.getInstance().inject(this)
    }

    override fun viewBindingInflate(
        inflater: LayoutInflater,
        container: ViewGroup,
    ): FragmentTasksEditBinding {
        return FragmentTasksEditBinding.inflate(inflater, container, false)
    }

    override fun initTitle(): TitleBar? {
        titleBar = super.initTitle()!!.setImmersive(false)
        titleBar!!.setTitle(R.string.menu_tasks)
        return titleBar
    }

    /**
     * 初始化控件
     */
    override fun initViews() {
        conditionsRecyclerView = findViewById(R.id.recycler_conditions)
        actionsRecyclerView = findViewById(R.id.recycler_actions)

        // 初始化 RecyclerView 和 Adapter
        initRecyclerViews()

        // 设置拖动排序
        val conditionsCallback = ItemMoveCallback(object : ItemMoveCallback.Listener {
            override fun onItemMove(fromPosition: Int, toPosition: Int) {
                Log.d(TAG, "onItemMove: $fromPosition $toPosition")
                conditionsAdapter.onItemMove(fromPosition, toPosition)
                conditionsList = conditionsAdapter.itemList
            }

            override fun onDragFinished() {
                //conditionsList保持与adapter一致
                conditionsList = conditionsAdapter.itemList
                Log.d(TAG, "onDragFinished: $conditionsList")
                //conditionsAdapter.notifyDataSetChanged()
            }
        })

        val itemTouchHelperConditions = ItemTouchHelper(conditionsCallback)
        itemTouchHelperConditions.attachToRecyclerView(conditionsRecyclerView)
        conditionsAdapter.setTouchHelper(itemTouchHelperConditions)

        val actionsCallback = ItemMoveCallback(object : ItemMoveCallback.Listener {
            override fun onItemMove(fromPosition: Int, toPosition: Int) {
                Log.d(TAG, "onItemMove: $fromPosition $toPosition")
                actionsAdapter.onItemMove(fromPosition, toPosition)
                actionsList = actionsAdapter.itemList
            }

            override fun onDragFinished() {
                //actionsList保持与adapter一致
                actionsList = actionsAdapter.itemList
                Log.d(TAG, "onDragFinished: $actionsList")
                //actionsAdapter.notifyDataSetChanged()
            }
        })

        val itemTouchHelperActions = ItemTouchHelper(actionsCallback)
        itemTouchHelperActions.attachToRecyclerView(actionsRecyclerView)
        actionsAdapter.setTouchHelper(itemTouchHelperActions)

        if (taskId <= 0) { //新增
            titleBar?.setSubTitle(getString(R.string.add_task))
            binding!!.btnDel.setText(R.string.discard)
        } else { //编辑 & 克隆
            binding!!.btnDel.setText(R.string.del)
            initForm()
        }
    }

    override fun initListeners() {
        binding!!.layoutAddCondition.setOnClickListener(this)
        binding!!.layoutAddAction.setOnClickListener(this)
        binding!!.btnTest.setOnClickListener(this)
        binding!!.btnDel.setOnClickListener(this)
        binding!!.btnSave.setOnClickListener(this)
    }

    @SuppressLint("InflateParams")
    @SingleClick
    override fun onClick(v: View) {
        try {
            when (v.id) {
                R.id.layout_add_condition -> {
                    val bottomSheet: View = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_task_condition_bottom_sheet, null)
                    val tvTitle: TextView = bottomSheet.findViewById(R.id.tv_title)
                    tvTitle.text = if (conditionsList.isEmpty()) getString(R.string.select_task_trigger) else getString(R.string.select_task_condition)

                    val recyclerView: RecyclerView = bottomSheet.findViewById(R.id.recyclerView)
                    WidgetUtils.initGridRecyclerView(recyclerView, 4, DensityUtils.dp2px(1f))
                    val widgetItemAdapter = WidgetItemAdapter(TASK_CONDITION_FRAGMENT_LIST)
                    widgetItemAdapter.setOnItemClickListener(that)
                    recyclerView.adapter = widgetItemAdapter

                    val bottomSheetCloseButton: XUIAlphaTextView = bottomSheet.findViewById(R.id.bottom_sheet_close_button)
                    bottomSheetCloseButton.setOnClickListener { dialog.dismiss() }

                    dialog.setContentView(bottomSheet)
                    dialog.setCancelable(true)
                    dialog.setCanceledOnTouchOutside(true)
                    dialog.show()
                    WidgetUtils.transparentBottomSheetDialogBackground(dialog)
                }

                R.id.layout_add_action -> {
                    val bottomSheet: View = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_task_action_bottom_sheet, null)
                    val recyclerView: RecyclerView = bottomSheet.findViewById(R.id.recyclerView)

                    WidgetUtils.initGridRecyclerView(recyclerView, 4, DensityUtils.dp2px(1f))
                    val widgetItemAdapter = WidgetItemAdapter(TASK_ACTION_FRAGMENT_LIST)
                    widgetItemAdapter.setOnItemClickListener(that)
                    recyclerView.adapter = widgetItemAdapter

                    val bottomSheetCloseButton: XUIAlphaTextView = bottomSheet.findViewById(R.id.bottom_sheet_close_button)
                    bottomSheetCloseButton.setOnClickListener { dialog.dismiss() }

                    dialog.setContentView(bottomSheet)
                    dialog.setCancelable(true)
                    dialog.setCanceledOnTouchOutside(true)
                    dialog.show()
                    WidgetUtils.transparentBottomSheetDialogBackground(dialog)
                }

                R.id.btn_test -> {
                    val taskNew = checkForm()
                    testTask(taskNew)
                    return
                }

                R.id.btn_del -> {
                    if (taskId <= 0 || isClone) {
                        popToBack()
                        return
                    }

                    //TODO: 删除前确认
                    return
                }

                R.id.btn_save -> {
                    val taskNew = checkForm()
                    if (isClone) taskNew.id = 0
                    Log.d(TAG, taskNew.toString())
                    //保存任务
                    if (taskNew.id > 0) {
                        Core.task.update(taskNew)
                    } else {
                        taskNew.id = Core.task.insert(taskNew)
                    }
                    //应用任务
                    applyTask(taskNew)
                    XToastUtils.success(R.string.tipSaveSuccess)
                    popToBack()
                    return
                }
            }
        } catch (e: Exception) {
            XToastUtils.error(e.message.toString())
            e.printStackTrace()
            Log.e(TAG, "onClick error: ${e.message}")
        }
    }

    //初始化表单
    private fun initForm() {
        Core.task.get(taskId).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(object : SingleObserver<Task> {
            override fun onSubscribe(d: Disposable) {}

            override fun onError(e: Throwable) {
                e.printStackTrace()
                Log.e(TAG, "initForm error: ${e.message}")
            }

            @SuppressLint("NotifyDataSetChanged")
            override fun onSuccess(task: Task) {
                Log.d(TAG, task.toString())
                if (isClone) {
                    titleBar?.setSubTitle(getString(R.string.clone_task))
                    binding!!.btnDel.setText(R.string.discard)
                } else {
                    titleBar?.setSubTitle(getString(R.string.edit_task))
                }
                binding!!.etName.setText(task.name)
                binding!!.sbStatus.isChecked = task.status == STATUS_ON
                try {
                    if (task.conditions.isNotEmpty()) {
                        val conditionList = Gson().fromJson(task.conditions, Array<TaskSetting>::class.java).toMutableList()
                        for (condition in conditionList) {
                            this@TasksEditFragment.conditionsList.add(condition)
                        }
                        Log.d(TAG, "conditionsList: ${this@TasksEditFragment.conditionsList}")
                        conditionsAdapter.notifyDataSetChanged()
                        binding!!.layoutAddCondition.visibility = if (this@TasksEditFragment.conditionsList.size >= MAX_SETTING_NUM) View.GONE else View.VISIBLE
                    }
                    if (task.actions.isNotEmpty()) {
                        val actionList = Gson().fromJson(task.actions, Array<TaskSetting>::class.java).toMutableList()
                        for (action in actionList) {
                            this@TasksEditFragment.actionsList.add(action)
                        }
                        Log.d(TAG, "actionsList: ${this@TasksEditFragment.actionsList}")
                        actionsAdapter.notifyDataSetChanged()
                        binding!!.layoutAddAction.visibility = if (this@TasksEditFragment.actionsList.size >= MAX_SETTING_NUM) View.GONE else View.VISIBLE
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Log.e(TAG, "initForm error: ${e.message}")
                    XToastUtils.error(e.message.toString())
                }
            }
        })
    }

    //提交前检查表单
    private fun checkForm(): Task {
        val taskName = binding!!.etName.text.toString().trim()
        if (taskName.isEmpty()) {
            throw Exception(getString(R.string.invalid_task_name))
        }
        if (conditionsList.size <= 0) {
            throw Exception(getString(R.string.invalid_conditions))
        }
        if (actionsList.size <= 0) {
            throw Exception(getString(R.string.invalid_actions))
        }

        //短信广播/通话广播/APP通知 类型条件只能放在第一个
        for (i in 1 until conditionsList.size) {
            if (conditionsList[i].type == TASK_CONDITION_SMS || conditionsList[i].type == TASK_CONDITION_CALL || conditionsList[i].type == TASK_CONDITION_APP) {
                throw Exception(getString(R.string.msg_condition_must_be_trigger))
            }
        }

        val lastExecTime = Date()
        // 将毫秒部分设置为 0，避免因为毫秒部分不同导致的任务重复执行
        lastExecTime.time = lastExecTime.time / 1000 * 1000
        var nextExecTime = lastExecTime
        val firstCondition = conditionsList[0]
        taskType = firstCondition.type

        when (taskType) {
            TASK_CONDITION_CRON -> {
                //检查定时任务的时间设置
                val cronSetting = Gson().fromJson(firstCondition.setting, CronSetting::class.java)
                if (cronSetting.expression.isEmpty()) {
                    throw Exception(getString(R.string.invalid_cron))
                }
                val cronExpression = CronExpression(cronSetting.expression)
                nextExecTime = cronExpression.getNextValidTimeAfter(lastExecTime)
            }
        }

        //拼接任务描述
        val description = StringBuilder()
        description.append(getString(R.string.task_conditions)).append(" ")
        description.append(conditionsList.map { it.description }.toTypedArray().joinToString(","))
        description.append(" ").append(getString(R.string.task_actions)).append(" ")
        description.append(actionsList.map { it.description }.toTypedArray().joinToString(","))

        val status = if (binding!!.sbStatus.isChecked) STATUS_ON else STATUS_OFF
        return Task(
            taskId, taskType, taskName, description.toString(), Gson().toJson(conditionsList), Gson().toJson(actionsList), status, lastExecTime, nextExecTime
        )
    }

    //测试任务
    private fun testTask(task: Task) {
    }

    //应用任务
    private fun applyTask(task: Task) {
        when (task.type) {
            //定时任务
            TASK_CONDITION_CRON -> {
                if (task.id <= 0) return
                //取消旧任务的定时器 & 设置新的定时器
                CronJobScheduler.cancelTask(task.id)
                CronJobScheduler.scheduleTask(task)
            }
        }
    }

    @SingleClick
    override fun onItemClick(itemView: View, widgetInfo: PageInfo, pos: Int) {
        try {
            dialog.dismiss()
            Log.d(TAG, "onItemClick: $widgetInfo")
            //判断点击的是条件还是动作
            if (widgetInfo.classPath.contains(".condition.")) {
                val typeCondition = pos + KEY_BACK_CODE_CONDITION
                //短信广播、通话广播、APP通知 类型条件必须作为触发提交
                if ((typeCondition == TASK_CONDITION_SMS || typeCondition == TASK_CONDITION_CALL || typeCondition == TASK_CONDITION_APP) && actionsList.isNotEmpty()) {
                    XToastUtils.error(getString(R.string.msg_condition_must_be_trigger))
                    return
                }
                //判断是否已经添加过该类型条件
                for (item in conditionsList) {
                    //注意：TASK_CONDITION_XXX 枚举值 等于 TASK_CONDITION_FRAGMENT_LIST 索引加上 KEY_BACK_CODE_CONDITION，不可改变
                    if (item.type == typeCondition) {
                        XToastUtils.error(getString(R.string.condition_already_exists))
                        return
                    }

                    //必须开启定位服务，才能使用进入地点 或 离开地点 类型条件
                    if ((typeCondition == TASK_CONDITION_TO_ADDRESS || typeCondition == TASK_CONDITION_LEAVE_ADDRESS) && !App.LocationClient.isStarted()) {
                        MaterialDialog.Builder(requireContext())
                            .iconRes(R.drawable.auto_task_icon_location)
                            .title(R.string.enable_location)
                            .content(R.string.enable_location_dialog)
                            .cancelable(false)
                            .positiveText(R.string.lab_yes)
                            .negativeText(R.string.lab_no).onPositive { _: MaterialDialog?, _: DialogAction? ->
                                SettingUtils.enableLocation = true
                                val serviceIntent = Intent(requireContext(), LocationService::class.java)
                                serviceIntent.action = ACTION_START
                                requireContext().startService(serviceIntent)
                            }.show()
                        return
                    }

                    //进入地点 或 离开地点 类型条件互斥
                    if ((typeCondition == TASK_CONDITION_TO_ADDRESS || typeCondition == TASK_CONDITION_LEAVE_ADDRESS) && (item.type == TASK_CONDITION_TO_ADDRESS || item.type == TASK_CONDITION_LEAVE_ADDRESS)) {
                        XToastUtils.error(getString(R.string.only_one_location_condition))
                        return
                    }

                    //短信广播、通话广播、APP通知 类型条件互斥
                    if ((typeCondition == TASK_CONDITION_SMS || typeCondition == TASK_CONDITION_CALL || typeCondition == TASK_CONDITION_APP) && (item.type == TASK_CONDITION_SMS || item.type == TASK_CONDITION_CALL || item.type == TASK_CONDITION_APP)) {
                        XToastUtils.error(getString(R.string.only_one_msg_condition))
                        return
                    }
                }
            } else {
                val typeAction = pos + KEY_BACK_CODE_ACTION
                //判断是否已经添加过该类型动作
                for (item in actionsList) {
                    //注意：TASK_ACTION_XXX 枚举值 等于 TASK_ACTION_FRAGMENT_LIST 索引加上 KEY_BACK_CODE_ACTION，不可改变
                    if (item.type == typeAction) {
                        XToastUtils.error(getString(R.string.action_already_exists))
                        return
                    }
                }
            }

            @Suppress("UNCHECKED_CAST") PageOption.to(Class.forName(widgetInfo.classPath) as Class<XPageFragment>) //跳转的fragment
                .setRequestCode(0) //requestCode: 0 新增 、>0 编辑（itemListXxx 的索引加1）
                .putString(KEY_EVENT_PARAMS_CONDITION, widgetInfo.params)
                .open(this)
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(TAG, "onItemClick error: ${e.message}")
            XToastUtils.error(e.message.toString())
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onFragmentResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onFragmentResult(requestCode, resultCode, data)
        Log.d(TAG, "requestCode:$requestCode resultCode:$resultCode data:$data")
        if (data != null) {
            val extras = data.extras ?: return

            val description: String?
            var setting: String? = null
            if (resultCode in KEY_BACK_CODE_CONDITION..KEY_BACK_CODE_CONDITION + 999) {
                setting = extras.getString(KEY_BACK_DATA_CONDITION) ?: return
                //注意：TASK_CONDITION_XXX 枚举值 等于 TASK_CONDITION_FRAGMENT_LIST 索引加上 KEY_BACK_CODE_CONDITION，不可改变
                val widgetInfoIndex = resultCode - KEY_BACK_CODE_CONDITION
                if (widgetInfoIndex >= TASK_CONDITION_FRAGMENT_LIST.size) return
                val widgetInfo = TASK_CONDITION_FRAGMENT_LIST[widgetInfoIndex]
                description = extras.getString(KEY_BACK_DESCRIPTION_CONDITION) ?: widgetInfo.name.toString()
                val taskSetting = TaskSetting(resultCode, widgetInfo.name, description, setting, requestCode)
                //requestCode: 等于 conditionsList 的索引加1
                if (requestCode == 0) {
                    taskSetting.position = conditionsList.size
                    conditionsList.add(taskSetting)
                    conditionsAdapter.notifyItemInserted(conditionsList.size - 1)
                } else {
                    conditionsList[requestCode - 1] = taskSetting
                    conditionsAdapter.notifyItemChanged(requestCode - 1)
                }
                //conditionsAdapter.notifyDataSetChanged()
                binding!!.layoutAddCondition.visibility = if (conditionsList.size >= MAX_SETTING_NUM) View.GONE else View.VISIBLE
            } else if (resultCode in KEY_BACK_CODE_ACTION..KEY_BACK_CODE_ACTION + 999) {
                setting = extras.getString(KEY_BACK_DATA_ACTION) ?: return
                //注意：TASK_ACTION_XXX 枚举值 等于 TASK_ACTION_FRAGMENT_LIST 索引加上 KEY_BACK_CODE_ACTION，不可改变
                val widgetInfoIndex = resultCode - KEY_BACK_CODE_ACTION
                if (widgetInfoIndex >= TASK_ACTION_FRAGMENT_LIST.size) return
                val widgetInfo = TASK_ACTION_FRAGMENT_LIST[widgetInfoIndex]
                description = extras.getString(KEY_BACK_DESCRIPTION_ACTION) ?: widgetInfo.name.toString()
                val taskSetting = TaskSetting(resultCode, widgetInfo.name, description, setting, requestCode)
                //requestCode: 等于 actionsList 的索引加1
                if (requestCode == 0) {
                    taskSetting.position = actionsList.size
                    actionsList.add(taskSetting)
                    actionsAdapter.notifyItemInserted(actionsList.size - 1)
                } else {
                    actionsList[requestCode - 1] = taskSetting
                    actionsAdapter.notifyItemChanged(requestCode - 1)
                }
                //actionsAdapter.notifyDataSetChanged()
                binding!!.layoutAddAction.visibility = if (actionsList.size >= MAX_SETTING_NUM) View.GONE else View.VISIBLE
            }
            Log.d(TAG, "requestCode:$requestCode resultCode:$resultCode setting:$setting")
        }
    }

    private fun initRecyclerViews() {
        conditionsAdapter = TaskSettingAdapter(conditionsList, { position -> removeCondition(position) }, { position -> editCondition(position) })

        actionsAdapter = TaskSettingAdapter(actionsList, { position -> removeAction(position) }, { position -> editAction(position) })

        conditionsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = conditionsAdapter
        }

        actionsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = actionsAdapter
        }
    }

    private fun editCondition(position: Int) {
        // 实现编辑条件项目的逻辑
        // 根据 position 对特定项目进行编辑
        val condition = conditionsList[position]
        Log.d(TAG, "editCondition: $position, $condition")

        val widgetInfoIndex = condition.type - KEY_BACK_CODE_CONDITION
        //判断是否存在
        if (widgetInfoIndex < 0 || widgetInfoIndex >= TASK_CONDITION_FRAGMENT_LIST.size) return
        val widgetInfo = TASK_CONDITION_FRAGMENT_LIST[condition.type - KEY_BACK_CODE_CONDITION]
        @Suppress("UNCHECKED_CAST")
        PageOption.to(Class.forName(widgetInfo.classPath) as Class<XPageFragment>) //跳转的fragment
            .setRequestCode(position + 1) //requestCode: 0 新增 、>0 编辑（conditionsList 的索引加1）
            .putString(KEY_EVENT_DATA_CONDITION, condition.setting)
            .putString(KEY_EVENT_PARAMS_CONDITION, widgetInfo.params)
            .open(this)
    }

    private fun removeCondition(position: Int) {
        conditionsList.removeAt(position)
        conditionsAdapter.notifyItemRemoved(position)
        conditionsAdapter.notifyItemRangeChanged(position, conditionsList.size) // 更新索引
        binding!!.layoutAddCondition.visibility = if (conditionsList.size >= MAX_SETTING_NUM) View.GONE else View.VISIBLE
    }

    private fun editAction(position: Int) {
        // 实现编辑操作项目的逻辑
        // 根据 position 对特定项目进行编辑
        val action = actionsList[position]
        Log.d(TAG, "editAction: $position, $action")

        val widgetInfoIndex = action.type - KEY_BACK_CODE_ACTION
        //判断是否存在
        if (widgetInfoIndex < 0 || widgetInfoIndex >= TASK_ACTION_FRAGMENT_LIST.size) return
        val widgetInfo = TASK_ACTION_FRAGMENT_LIST[action.type - KEY_BACK_CODE_ACTION]
        @Suppress("UNCHECKED_CAST")
        PageOption.to(Class.forName(widgetInfo.classPath) as Class<XPageFragment>) //跳转的fragment
            .setRequestCode(position + 1) //requestCode: 0 新增 、>0 编辑（actionsList 的索引加1）
            .putString(KEY_EVENT_DATA_ACTION, action.setting)
            .open(this)
    }

    private fun removeAction(position: Int) {
        actionsList.removeAt(position)
        actionsAdapter.notifyItemRemoved(position)
        actionsAdapter.notifyItemRangeChanged(position, actionsList.size) // 更新索引
        binding!!.layoutAddAction.visibility = if (actionsList.size >= MAX_SETTING_NUM) View.GONE else View.VISIBLE
    }
}