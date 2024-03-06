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
import com.idormy.sms.forwarder.adapter.TaskRecyclerAdapter
import com.idormy.sms.forwarder.adapter.base.ItemMoveCallback
import com.idormy.sms.forwarder.adapter.spinner.TaskSpinnerAdapter
import com.idormy.sms.forwarder.adapter.spinner.TaskSpinnerItem
import com.idormy.sms.forwarder.core.BaseFragment
import com.idormy.sms.forwarder.core.Core
import com.idormy.sms.forwarder.database.entity.Task
import com.idormy.sms.forwarder.databinding.FragmentTasksActionTaskBinding
import com.idormy.sms.forwarder.entity.MsgInfo
import com.idormy.sms.forwarder.entity.TaskSetting
import com.idormy.sms.forwarder.entity.action.TaskActionSetting
import com.idormy.sms.forwarder.utils.KEY_BACK_DATA_ACTION
import com.idormy.sms.forwarder.utils.KEY_BACK_DESCRIPTION_ACTION
import com.idormy.sms.forwarder.utils.KEY_EVENT_DATA_ACTION
import com.idormy.sms.forwarder.utils.Log
import com.idormy.sms.forwarder.utils.STATUS_OFF
import com.idormy.sms.forwarder.utils.TASK_ACTION_TASK
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

@Page(name = "Task")
@Suppress("PrivatePropertyName", "DEPRECATION")
class TaskActionFragment : BaseFragment<FragmentTasksActionTaskBinding?>(), View.OnClickListener {

    private val TAG: String = TaskActionFragment::class.java.simpleName
    private var titleBar: TitleBar? = null
    private var mCountDownHelper: CountDownButtonHelper? = null

    //所有自动任务下拉框
    private var taskListAll = mutableListOf<Task>()
    private val taskSpinnerList = mutableListOf<TaskSpinnerItem>()
    private lateinit var taskSpinnerAdapter: TaskSpinnerAdapter<*>

    //已选自动任务列表
    private var taskId = 0L
    private var taskListSelected = mutableListOf<Task>()
    private lateinit var taskRecyclerView: RecyclerView
    private lateinit var taskRecyclerAdapter: TaskRecyclerAdapter

    @JvmField
    @AutoWired(name = KEY_EVENT_DATA_ACTION)
    var eventData: String? = null

    override fun initArgs() {
        XRouter.getInstance().inject(this)
    }

    override fun viewBindingInflate(
        inflater: LayoutInflater,
        container: ViewGroup,
    ): FragmentTasksActionTaskBinding {
        return FragmentTasksActionTaskBinding.inflate(inflater, container, false)
    }

    override fun initTitle(): TitleBar? {
        titleBar = super.initTitle()!!.setImmersive(false).setTitle(R.string.task_task)
        return titleBar
    }

    /**
     * 初始化控件
     */
    override fun initViews() {
        //测试按钮增加倒计时，避免重复点击
        mCountDownHelper = CountDownButtonHelper(binding!!.btnTest, 1)
        mCountDownHelper!!.setOnCountDownListener(object : CountDownButtonHelper.OnCountDownListener {
            override fun onCountDown(time: Int) {
                binding!!.btnTest.text = String.format(getString(R.string.seconds_n), time)
            }

            override fun onFinished() {
                binding!!.btnTest.text = getString(R.string.test)
                //获取自动任务列表
                getTaskList()
            }
        })

        Log.d(TAG, "initViews eventData:$eventData")
        if (eventData != null) {
            val settingVo = Gson().fromJson(eventData, TaskActionSetting::class.java)
            binding!!.rgStatus.check(if (settingVo.status == 1) R.id.rb_status_enable else R.id.rb_status_disable)
            Log.d(TAG, settingVo.taskList.toString())
            settingVo.taskList.forEach {
                taskId = it.id
                taskListSelected.add(it)
            }
            Log.d(TAG, "initViews settingVo:$settingVo")
        }

        //初始化自动任务下拉框
        initTask()
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
                        val taskAction = TaskSetting(TASK_ACTION_TASK, getString(R.string.task_task), settingVo.description, Gson().toJson(settingVo), requestCode)
                        val taskActionsJson = Gson().toJson(arrayListOf(taskAction))
                        val msgInfo = MsgInfo("task", getString(R.string.task_task), settingVo.description, Date(), getString(R.string.task_task))
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
                    setFragmentResult(TASK_ACTION_TASK, intent)
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

    //初始化自动任务
    @SuppressLint("SetTextI18n", "NotifyDataSetChanged")
    private fun initTask() {
        //初始化自动任务下拉框
        binding!!.spTask.setOnItemClickListener { _: AdapterView<*>, _: View, position: Int, _: Long ->
            try {
                val item = taskSpinnerAdapter.getItemSource(position) as TaskSpinnerItem
                taskId = item.id!!
                if (taskId > 0L) {
                    taskListSelected.forEach {
                        if (taskId == it.id) {
                            XToastUtils.warning(getString(R.string.task_contains_tips))
                            return@setOnItemClickListener
                        }
                    }
                    taskListAll.forEach {
                        if (taskId == it.id) {
                            taskListSelected.add(it)
                        }
                    }
                    taskRecyclerAdapter.notifyDataSetChanged()
                }
            } catch (e: Exception) {
                XToastUtils.error(e.message.toString())
            }
        }

        // 初始化已选自动任务列表 RecyclerView 和 Adapter
        taskRecyclerView = binding!!.recyclerTasks
        taskRecyclerAdapter = TaskRecyclerAdapter(taskListSelected, { position ->
            taskListSelected.removeAt(position)
            taskRecyclerAdapter.notifyItemRemoved(position)
            taskRecyclerAdapter.notifyItemRangeChanged(position, taskListSelected.size) // 更新索引
        })
        taskRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = taskRecyclerAdapter
        }
        val taskMoveCallback = ItemMoveCallback(object : ItemMoveCallback.Listener {
            override fun onItemMove(fromPosition: Int, toPosition: Int) {
                Log.d(TAG, "onItemMove: $fromPosition $toPosition")
                taskRecyclerAdapter.onItemMove(fromPosition, toPosition)
                taskListSelected = taskRecyclerAdapter.itemList
            }

            override fun onDragFinished() {
                taskListSelected = taskRecyclerAdapter.itemList
                //taskRecyclerAdapter.notifyDataSetChanged()
                Log.d(TAG, "onDragFinished: $taskListSelected")
            }
        })
        val taskTouchHelper = ItemTouchHelper(taskMoveCallback)
        taskTouchHelper.attachToRecyclerView(taskRecyclerView)
        taskRecyclerAdapter.setTouchHelper(taskTouchHelper)

        //获取自动任务列表
        getTaskList()
    }

    //获取自动任务列表
    private fun getTaskList() {
        Core.task.getAll().subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(object : SingleObserver<List<Task>> {
            override fun onSubscribe(d: Disposable) {}

            override fun onError(e: Throwable) {
                e.printStackTrace()
                Log.e(TAG, "getTaskList error: ${e.message}")
            }

            @SuppressLint("NotifyDataSetChanged")
            override fun onSuccess(taskList: List<Task>) {
                if (taskList.isEmpty()) {
                    XToastUtils.error(R.string.add_task_first)
                    return
                }

                taskSpinnerList.clear()
                taskListAll = taskList as MutableList<Task>
                for (task in taskList) {
                    val name = if (task.name.length > 20) task.name.substring(0, 19) else task.name
                    taskSpinnerList.add(TaskSpinnerItem(name, getDrawable(if (STATUS_OFF == task.status) task.greyImageId else task.imageId), task.id, task.status))
                }
                taskSpinnerAdapter = TaskSpinnerAdapter(taskSpinnerList).setIsFilterKey(true).setFilterColor("#EF5362").setBackgroundSelector(R.drawable.selector_custom_spinner_bg)
                binding!!.spTask.setAdapter(taskSpinnerAdapter)
                //taskSpinnerAdapter.notifyDataSetChanged()

                //更新taskListSelected的状态与名称
                taskListSelected.forEach {
                    taskListAll.forEach { task ->
                        if (it.id == task.id) {
                            //it.name = task.name
                            it.status = task.status
                        }
                    }
                }
                taskRecyclerAdapter.notifyDataSetChanged()

            }
        })
    }

    //检查设置
    @SuppressLint("SetTextI18n")
    private fun checkSetting(): TaskActionSetting {
        if (taskListSelected.isEmpty() || taskId == 0L) {
            throw Exception(getString(R.string.new_task_first))
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
        description.append(getString(R.string.menu_tasks)).append(", ").append(getString(R.string.specified_task)).append(": ")
        description.append(taskListSelected.joinToString(",") { "[${it.id}]${it.name}" })

        return TaskActionSetting(description.toString(), status, taskListSelected)
    }
}