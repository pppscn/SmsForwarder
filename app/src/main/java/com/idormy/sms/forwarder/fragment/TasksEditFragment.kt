package com.idormy.sms.forwarder.fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.gson.Gson
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.adapter.WidgetItemAdapter
import com.idormy.sms.forwarder.adapter.spinner.ActionAdapterItem
import com.idormy.sms.forwarder.adapter.spinner.ConditionAdapterItem
import com.idormy.sms.forwarder.core.BaseFragment
import com.idormy.sms.forwarder.database.AppDatabase
import com.idormy.sms.forwarder.database.entity.Sender
import com.idormy.sms.forwarder.database.entity.Task
import com.idormy.sms.forwarder.database.viewmodel.BaseViewModelFactory
import com.idormy.sms.forwarder.database.viewmodel.TaskViewModel
import com.idormy.sms.forwarder.databinding.FragmentTasksEditBinding
import com.idormy.sms.forwarder.entity.task.CronSetting
import com.idormy.sms.forwarder.entity.task.TaskSetting
import com.idormy.sms.forwarder.utils.*
import com.xuexiang.xaop.annotation.SingleClick
import com.xuexiang.xpage.annotation.Page
import com.xuexiang.xpage.base.XPageFragment
import com.xuexiang.xpage.core.PageOption
import com.xuexiang.xpage.model.PageInfo
import com.xuexiang.xrouter.annotation.AutoWired
import com.xuexiang.xrouter.launcher.XRouter
import com.xuexiang.xui.adapter.recyclerview.RecyclerViewHolder
import com.xuexiang.xui.utils.DensityUtils
import com.xuexiang.xui.utils.WidgetUtils
import com.xuexiang.xui.widget.actionbar.TitleBar
import io.reactivex.SingleObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.*
import java.util.*


@Page(name = "自动任务·编辑器")
@Suppress("PrivatePropertyName", "unused", "DEPRECATION", "UNUSED_PARAMETER")
class TasksEditFragment : BaseFragment<FragmentTasksEditBinding?>(), View.OnClickListener, CompoundButton.OnCheckedChangeListener, RecyclerViewHolder.OnItemClickListener<PageInfo> {

    private val TAG: String = TasksEditFragment::class.java.simpleName
    private val that = this
    var titleBar: TitleBar? = null
    private val viewModel by viewModels<TaskViewModel> { BaseViewModelFactory(context) }
    private val dialog: BottomSheetDialog by lazy {
        BottomSheetDialog(requireContext())
    }

    //触发条件列表
    private var conditionId = 0L
    private var conditionListSelected: MutableList<Sender> = mutableListOf()
    private var conditionItemMap = HashMap<Long, LinearLayout>(2)

    //执行动作列表
    private var actionId = 0L
    private var actionListSelected: MutableList<Sender> = mutableListOf()
    private var actionItemMap = HashMap<Long, LinearLayout>(2)

    @JvmField
    @AutoWired(name = KEY_RULE_ID)
    var taskId: Long = 0

    @JvmField
    @AutoWired(name = KEY_RULE_TYPE)
    var taskType: String = "sms"

    @JvmField
    @AutoWired(name = KEY_RULE_CLONE)
    var isClone: Boolean = false

    //初始化数据
    private val itemListConditions = mutableListOf(
        TaskSetting(TYPE_DINGTALK_GROUP_ROBOT, "Item 1", "Description 1"), TaskSetting(TYPE_EMAIL, "Item 2", "Description 2"), TaskSetting(TYPE_BARK, "Item 3", "Description 3")
        // ... other items
    )
    private val itemListActions = mutableListOf(
        TaskSetting(TYPE_DINGTALK_GROUP_ROBOT, "Apple", "Description Apple"), TaskSetting(TYPE_EMAIL, "Banana", "Description Banana"), TaskSetting(TYPE_BARK, "Orange", "Description Orange")
        // ... other items
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

    override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {/*when (buttonView?.id) {
        }*/
    }

    @SuppressLint("InflateParams")
    @SingleClick
    override fun onClick(v: View) {
        try {
            when (v.id) {
                R.id.layout_add_condition -> {
                    val view: View = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_task_condition_bottom_sheet, null)
                    val recyclerView: RecyclerView = view.findViewById(R.id.recyclerView)

                    WidgetUtils.initGridRecyclerView(recyclerView, 4, DensityUtils.dp2px(1f))
                    val widgetItemAdapter = WidgetItemAdapter(TASK_CONDITION_FRAGMENT_LIST)
                    widgetItemAdapter.setOnItemClickListener(that)
                    recyclerView.adapter = widgetItemAdapter

                    dialog.setContentView(view)
                    dialog.setCancelable(true)
                    dialog.setCanceledOnTouchOutside(true)
                    dialog.show()
                    WidgetUtils.transparentBottomSheetDialogBackground(dialog)
                }

                R.id.layout_add_action -> {
                    val view: View = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_task_action_bottom_sheet, null)
                    val recyclerView: RecyclerView = view.findViewById(R.id.recyclerView)

                    WidgetUtils.initGridRecyclerView(recyclerView, 4, DensityUtils.dp2px(1f))
                    val widgetItemAdapter = WidgetItemAdapter(TASK_ACTION_FRAGMENT_LIST)
                    widgetItemAdapter.setOnItemClickListener(that)
                    recyclerView.adapter = widgetItemAdapter

                    dialog.setContentView(view)
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
                    viewModel.insertOrUpdate(taskNew)
                    XToastUtils.success(R.string.tipSaveSuccess)
                    popToBack()
                    return
                }
            }
        } catch (e: Exception) {
            XToastUtils.error(e.message.toString())
            e.printStackTrace()
        }
    }

    /**
     * 动态增删ConditionItem
     *
     * @param conditionItemMap          管理item的map，用于删除指定header
     * @param layoutConditions          需要挂载item的LinearLayout
     * @param condition                 ConditionAdapterItem
     */
    @SuppressLint("SetTextI18n")
    private fun addConditionItemLinearLayout(
        conditionItemMap: MutableMap<Long, LinearLayout>, layoutConditions: LinearLayout, condition: ConditionAdapterItem
    ) {
        val layoutConditionItem = View.inflate(requireContext(), R.layout.item_add_condition, null) as LinearLayout
        val ivRemoveCondition = layoutConditionItem.findViewById<ImageView>(R.id.iv_remove_condition)
        val ivConditionImage = layoutConditionItem.findViewById<ImageView>(R.id.iv_condition_image)
        val tvConditionName = layoutConditionItem.findViewById<TextView>(R.id.tv_condition_name)

        ivConditionImage.setImageDrawable(condition.icon)
        val conditionItemId = condition.id as Long
        tvConditionName.text = "ID-$conditionItemId：${condition.title}"

        ivRemoveCondition.tag = conditionItemId
        ivRemoveCondition.setOnClickListener { view2: View ->
            val tagId = view2.tag as Long
            layoutConditions.removeView(conditionItemMap[tagId])
            conditionItemMap.remove(tagId)
        }
        layoutConditions.addView(layoutConditionItem)
        conditionItemMap[conditionItemId] = layoutConditionItem

        if (conditionItemMap.isNotEmpty()) {
            binding!!.tvAddCondition.text = getString(R.string.add_condition_continue)
            binding!!.tvAddConditionTips.visibility = View.GONE
        } else {
            binding!!.tvAddCondition.text = getString(R.string.add_condition)
            binding!!.tvAddConditionTips.visibility = View.VISIBLE
        }
    }

    /**
     * 动态增删ActionItem
     *
     * @param actionItemMap          管理item的map，用于删除指定header
     * @param layoutActions          需要挂载item的LinearLayout
     * @param action                 ActionAdapterItem
     */
    @SuppressLint("SetTextI18n")
    private fun addActionItemLinearLayout(
        actionItemMap: MutableMap<Long, LinearLayout>, layoutActions: LinearLayout, action: ActionAdapterItem
    ) {
        val layoutActionItem = View.inflate(requireContext(), R.layout.item_add_action, null) as LinearLayout
        val ivRemoveAction = layoutActionItem.findViewById<ImageView>(R.id.iv_remove_action)
        val ivActionImage = layoutActionItem.findViewById<ImageView>(R.id.iv_action_image)
        val tvActionName = layoutActionItem.findViewById<TextView>(R.id.tv_action_name)

        ivActionImage.setImageDrawable(action.icon)
        val actionItemId = action.id as Long
        tvActionName.text = "ID-$actionItemId：${action.title}"

        ivRemoveAction.tag = actionItemId
        ivRemoveAction.setOnClickListener { view2: View ->
            val tagId = view2.tag as Long
            layoutActions.removeView(actionItemMap[tagId])
            actionItemMap.remove(tagId)
        }
        layoutActions.addView(layoutActionItem)
        actionItemMap[actionItemId] = layoutActionItem

        if (actionItemMap.isNotEmpty()) {
            binding!!.tvAddAction.text = getString(R.string.add_action_continue)
            binding!!.tvAddActionTips.visibility = View.GONE
        } else {
            binding!!.tvAddAction.text = getString(R.string.add_action)
            binding!!.tvAddActionTips.visibility = View.VISIBLE
        }
    }

    //初始化表单
    private fun initForm() {
        AppDatabase.getInstance(requireContext()).taskDao().get(taskId).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(object : SingleObserver<Task> {
            override fun onSubscribe(d: Disposable) {}

            override fun onError(e: Throwable) {
                e.printStackTrace()
            }

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
            }
        })
    }

    //提交前检查表单
    private fun checkForm(): Task {
        if (conditionListSelected.isEmpty() || conditionId == 0L) {
            throw Exception(getString(R.string.new_sender_first))
        }

        if (actionListSelected.isEmpty() || actionId == 0L) {
            throw Exception(getString(R.string.new_sender_first))
        }
        return Task()
    }

    private fun testTask(task: Task) {

    }

    @SingleClick
    override fun onItemClick(itemView: View, widgetInfo: PageInfo, pos: Int) {
        try {
            dialog.dismiss()
            Log.d(TAG, "onItemClick: $widgetInfo")
            @Suppress("UNCHECKED_CAST") PageOption.to(Class.forName(widgetInfo.classPath) as Class<XPageFragment>) //跳转的fragment
                .setRequestCode(pos) //请求码，用于返回结果
                .open(this)
        } catch (e: Exception) {
            e.printStackTrace()
            XToastUtils.error(e.message.toString())
        }
    }

    override fun onFragmentResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onFragmentResult(requestCode, resultCode, data)
        if (data != null) {
            val extras = data.extras
            var backData: String? = null
            if (resultCode == KEY_BACK_CODE_CONDITION) {
                backData = extras!!.getString(KEY_BACK_DATA_CONDITION)
                if (backData == null) return
                when (requestCode) {
                    0 -> {
                        val settingVo = Gson().fromJson(backData, CronSetting::class.java)
                        val condition = ConditionAdapterItem(settingVo.expression) //TODO: 构建列表项目
                        addConditionItemLinearLayout(conditionItemMap, binding!!.layoutConditions, condition)
                    }
                }
            } else if (resultCode == KEY_BACK_CODE_ACTION) {
                backData = extras!!.getString(KEY_BACK_DATA_ACTION)
            }
            Log.d(TAG, "requestCode:$requestCode resultCode:$resultCode backData:$backData")
        }
    }
}