package com.idormy.sms.forwarder.fragment.action

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.gson.Gson
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.idormy.sms.forwarder.App
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.core.BaseFragment
import com.idormy.sms.forwarder.databinding.FragmentTasksActionAlarmBinding
import com.idormy.sms.forwarder.entity.MsgInfo
import com.idormy.sms.forwarder.entity.TaskSetting
import com.idormy.sms.forwarder.entity.action.AlarmSetting
import com.idormy.sms.forwarder.utils.KEY_BACK_DATA_ACTION
import com.idormy.sms.forwarder.utils.KEY_BACK_DESCRIPTION_ACTION
import com.idormy.sms.forwarder.utils.KEY_EVENT_DATA_ACTION
import com.idormy.sms.forwarder.utils.Log
import com.idormy.sms.forwarder.utils.TASK_ACTION_ALARM
import com.idormy.sms.forwarder.utils.TaskWorker
import com.idormy.sms.forwarder.utils.XToastUtils
import com.idormy.sms.forwarder.workers.ActionWorker
import com.xuexiang.xaop.annotation.SingleClick
import com.xuexiang.xpage.annotation.Page
import com.xuexiang.xrouter.annotation.AutoWired
import com.xuexiang.xrouter.launcher.XRouter
import com.xuexiang.xui.utils.CountDownButtonHelper
import com.xuexiang.xui.widget.actionbar.TitleBar
import com.xuexiang.xui.widget.dialog.materialdialog.MaterialDialog
import java.io.File
import java.util.Date

@Page(name = "Alarm")
@Suppress("PrivatePropertyName", "DEPRECATION")
class AlarmFragment : BaseFragment<FragmentTasksActionAlarmBinding?>(), View.OnClickListener {

    private val TAG: String = AlarmFragment::class.java.simpleName
    private var titleBar: TitleBar? = null
    private var mCountDownHelper: CountDownButtonHelper? = null
    private var appContext: App? = null

    @JvmField
    @AutoWired(name = KEY_EVENT_DATA_ACTION)
    var eventData: String? = null

    override fun initArgs() {
        XRouter.getInstance().inject(this)
    }

    override fun viewBindingInflate(
        inflater: LayoutInflater,
        container: ViewGroup,
    ): FragmentTasksActionAlarmBinding {
        return FragmentTasksActionAlarmBinding.inflate(inflater, container, false)
    }

    override fun initTitle(): TitleBar? {
        titleBar = super.initTitle()!!.setImmersive(false).setTitle(R.string.task_alarm)
        return titleBar
    }

    /**
     * 初始化控件
     */
    override fun initViews() {
        appContext = requireActivity().application as App
        //测试按钮增加倒计时，避免重复点击
        mCountDownHelper = CountDownButtonHelper(binding!!.btnTest, 2)
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
            val settingVo = Gson().fromJson(eventData, AlarmSetting::class.java)
            Log.d(TAG, "initViews settingVo:$settingVo")
            if (settingVo.action == "start") {
                binding!!.rgAlarmState.check(R.id.rb_start_alarm)
                binding!!.layoutAlarmSettings.visibility = View.VISIBLE
            } else {
                binding!!.rgAlarmState.check(R.id.rb_stop_alarm)
                binding!!.layoutAlarmSettings.visibility = View.GONE
            }
            binding!!.xsbVolume.setDefaultValue(settingVo.volume)
            binding!!.xsbLoopTimes.setDefaultValue(settingVo.playTimes)
            binding!!.etMusicPath.setText(settingVo.music)
        } else {
            binding!!.xsbVolume.setDefaultValue(80)
            binding!!.xsbLoopTimes.setDefaultValue(1)
        }
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
        binding!!.btnFilePicker.setOnClickListener(this)
        binding!!.xsbVolume.setOnSeekBarListener { _, _ ->
            checkSetting(true)
        }
        binding!!.xsbLoopTimes.setOnSeekBarListener { _, _ ->
            checkSetting(true)
        }
        binding!!.rgAlarmState.setOnCheckedChangeListener { _, checkedId ->
            binding!!.layoutAlarmSettings.visibility = if (checkedId == R.id.rb_start_alarm) View.VISIBLE else View.GONE
            checkSetting(true)
        }
    }

    @SingleClick
    override fun onClick(v: View) {
        try {
            when (v.id) {

                R.id.btn_file_picker -> {
                    // 申请储存权限
                    XXPermissions.with(this).permission(Permission.MANAGE_EXTERNAL_STORAGE).request(object : OnPermissionCallback {
                        @SuppressLint("SetTextI18n")
                        override fun onGranted(permissions: List<String>, all: Boolean) {
                            val downloadPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path
                            val fileList = findAudioFiles(downloadPath)
                            if (fileList.isEmpty()) {
                                XToastUtils.error(String.format(getString(R.string.download_music_first), downloadPath))
                                return
                            }
                            MaterialDialog.Builder(requireContext()).title(getString(R.string.alarm_music)).content(String.format(getString(R.string.root_directory), downloadPath)).items(fileList).itemsCallbackSingleChoice(0) { _: MaterialDialog?, _: View?, _: Int, text: CharSequence ->
                                val webPath = "$downloadPath/$text"
                                binding!!.etMusicPath.setText(webPath)
                                checkSetting(true)
                                true // allow selection
                            }.positiveText(R.string.select).negativeText(R.string.cancel).show()
                        }

                        override fun onDenied(permissions: List<String>, never: Boolean) {
                            if (never) {
                                XToastUtils.error(R.string.toast_denied_never)
                                // 如果是被永久拒绝就跳转到应用权限系统设置页面
                                XXPermissions.startPermissionActivity(requireContext(), permissions)
                            } else {
                                XToastUtils.error(R.string.toast_denied)
                            }
                            binding!!.etMusicPath.setText(getString(R.string.storage_permission_tips))
                        }
                    })
                }

                R.id.btn_test -> {
                    // 申请修改系统设置权限
                    XXPermissions.with(this).permission(Permission.WRITE_SETTINGS).request(object : OnPermissionCallback {
                        @SuppressLint("SetTextI18n")
                        override fun onGranted(permissions: List<String>, all: Boolean) {
                            mCountDownHelper?.start()
                            try {
                                val settingVo = checkSetting()
                                Log.d(TAG, settingVo.toString())
                                val taskAction = TaskSetting(TASK_ACTION_ALARM, getString(R.string.task_alarm), settingVo.description, Gson().toJson(settingVo), requestCode)
                                val taskActionsJson = Gson().toJson(arrayListOf(taskAction))
                                val msgInfo = MsgInfo("task", getString(R.string.task_alarm), settingVo.description, Date(), getString(R.string.task_alarm))
                                val actionData = Data.Builder().putLong(TaskWorker.TASK_ID, 0).putString(TaskWorker.TASK_ACTIONS, taskActionsJson).putString(TaskWorker.MSG_INFO, Gson().toJson(msgInfo)).build()
                                val actionRequest = OneTimeWorkRequestBuilder<ActionWorker>().setInputData(actionData).build()
                                WorkManager.getInstance().enqueue(actionRequest)
                            } catch (e: Exception) {
                                mCountDownHelper?.finish()
                                e.printStackTrace()
                                Log.e(TAG, "onClick error: ${e.message}")
                                XToastUtils.error(e.message.toString(), 30000)
                            }
                        }

                        override fun onDenied(permissions: List<String>, never: Boolean) {
                            if (never) {
                                XToastUtils.error(R.string.toast_denied_never)
                                // 如果是被永久拒绝就跳转到应用权限系统设置页面
                                XXPermissions.startPermissionActivity(requireContext(), permissions)
                            } else {
                                XToastUtils.error(R.string.toast_denied)
                            }
                            binding!!.tvDescription.text = getString(R.string.write_settings_permission_tips)
                        }
                    })
                    return
                }

                R.id.btn_del -> {
                    popToBack()
                    return
                }

                R.id.btn_save -> {
                    // 申请修改系统设置权限
                    XXPermissions.with(this).permission(Permission.WRITE_SETTINGS).request(object : OnPermissionCallback {
                        @SuppressLint("SetTextI18n")
                        override fun onGranted(permissions: List<String>, all: Boolean) {
                            val settingVo = checkSetting()
                            val intent = Intent()
                            intent.putExtra(KEY_BACK_DESCRIPTION_ACTION, settingVo.description)
                            intent.putExtra(KEY_BACK_DATA_ACTION, Gson().toJson(settingVo))
                            setFragmentResult(TASK_ACTION_ALARM, intent)
                            popToBack()
                        }

                        override fun onDenied(permissions: List<String>, never: Boolean) {
                            if (never) {
                                XToastUtils.error(R.string.toast_denied_never)
                                // 如果是被永久拒绝就跳转到应用权限系统设置页面
                                XXPermissions.startPermissionActivity(requireContext(), permissions)
                            } else {
                                XToastUtils.error(R.string.toast_denied)
                            }
                            binding!!.tvDescription.text = getString(R.string.write_settings_permission_tips)
                        }
                    })
                    return
                }
            }
        } catch (e: Exception) {
            XToastUtils.error(e.message.toString(), 30000)
            e.printStackTrace()
            Log.e(TAG, "onClick error: ${e.message}")
        }
    }

    //检查设置
    @Suppress("SameParameterValue")
    @SuppressLint("SetTextI18n")
    private fun checkSetting(updateView: Boolean = false): AlarmSetting {
        val volume = binding!!.xsbVolume.selectedNumber
        val loopTimes = binding!!.xsbLoopTimes.selectedNumber
        val music = binding!!.etMusicPath.text.toString().trim()
        val description = StringBuilder()
        val action = if (binding!!.rgAlarmState.checkedRadioButtonId == R.id.rb_start_alarm) {
            description.append(getString(R.string.start_alarm))
            description.append(", ").append(getString(R.string.alarm_volume)).append(":").append(volume).append("%")
            description.append(", ").append(getString(R.string.alarm_play_times)).append(":").append(loopTimes)
            if (music.isNotEmpty()) {
                description.append(", ").append(getString(R.string.alarm_music)).append(":").append(music)
            }
            "start"
        } else {
            description.append(getString(R.string.stop_alarm))
            "stop"
        }

        if (updateView) {
            binding!!.tvDescription.text = description.toString()
        }

        return AlarmSetting(description.toString(), action, volume, loopTimes, music)
    }

    private fun findAudioFiles(directoryPath: String): List<String> {
        val audioFiles = mutableListOf<String>()
        val directory = File(directoryPath)

        if (directory.exists() && directory.isDirectory) {
            directory.listFiles()?.let { files ->
                // 筛选出支持的音频文件
                files.filter { it.isFile && isSupportedAudioFile(it) }.forEach { audioFiles.add(it.name) }
            }
        }

        return audioFiles
    }

    private fun isSupportedAudioFile(file: File): Boolean {
        val supportedExtensions = listOf("mp3", "ogg", "wav")
        return supportedExtensions.any { it.equals(file.extension, ignoreCase = true) }
    }
}