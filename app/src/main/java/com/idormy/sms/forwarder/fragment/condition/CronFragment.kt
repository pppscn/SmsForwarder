package com.idormy.sms.forwarder.fragment.condition

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RadioGroup
import com.google.gson.Gson
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.core.BaseFragment
import com.idormy.sms.forwarder.databinding.FragmentTasksConditionCronBinding
import com.idormy.sms.forwarder.entity.condition.CronSetting
import com.idormy.sms.forwarder.utils.KEY_BACK_DATA_CONDITION
import com.idormy.sms.forwarder.utils.KEY_BACK_DESCRIPTION_CONDITION
import com.idormy.sms.forwarder.utils.KEY_EVENT_DATA_CONDITION
import com.idormy.sms.forwarder.utils.KEY_TEST_CONDITION
import com.idormy.sms.forwarder.utils.Log
import com.idormy.sms.forwarder.utils.TASK_CONDITION_CRON
import com.idormy.sms.forwarder.utils.XToastUtils
import com.jeremyliao.liveeventbus.LiveEventBus
import com.xuexiang.xaop.annotation.SingleClick
import com.xuexiang.xpage.annotation.Page
import com.xuexiang.xrouter.annotation.AutoWired
import com.xuexiang.xrouter.launcher.XRouter
import com.xuexiang.xui.utils.CountDownButtonHelper
import com.xuexiang.xui.widget.actionbar.TitleBar
import com.xuexiang.xui.widget.flowlayout.FlowTagLayout
import gatewayapps.crondroid.CronExpression
import net.redhogs.cronparser.CronExpressionDescriptor
import net.redhogs.cronparser.Options
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Page(name = "Cron")
@Suppress("PrivatePropertyName")
class CronFragment : BaseFragment<FragmentTasksConditionCronBinding?>(), View.OnClickListener {

    private val TAG: String = CronFragment::class.java.simpleName
    private var titleBar: TitleBar? = null
    private var mCountDownHelper: CountDownButtonHelper? = null

    @JvmField
    @AutoWired(name = KEY_EVENT_DATA_CONDITION)
    var eventData: String? = null

    private val secondsList: List<String> = (0..59).map { String.format("%02d", it) }
    private var selectedSecondList = ""

    private val minutesList: List<String> = (0..59).map { String.format("%02d", it) }
    private var selectedMinuteList = ""

    private val hoursList: List<String> = (0..23).map { String.format("%02d", it) }
    private var selectedHourList = ""

    private val dayList: List<String> = (1..31).map { String.format("%d", it) }
    private var selectedDayList = ""

    //private val monthList = listOf("JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC")
    private val monthList: List<String> = (1..12).map { String.format("%d", it) }
    private var selectedMonthList = ""

    //private val weekList = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
    private val weekList: List<String> = (1..7).map { String.format("%d", it) }
    private var selectedWeekList = ""

    private val yearList: List<String> = (2020..2099).map { String.format("%d", it) }
    private var selectedYearList = ""

    private val regexNum = Regex("\\d+")
    private var second = "*"
    private var minute = "*"
    private var hour = "*"
    private var day = "*"
    private var month = "*"
    private var week = "?"
    private var year = "*"
    private var expression = "$second $minute $hour $day $month $week $year"
    private var description = ""

    override fun initArgs() {
        XRouter.getInstance().inject(this)
    }

    override fun viewBindingInflate(
        inflater: LayoutInflater,
        container: ViewGroup,
    ): FragmentTasksConditionCronBinding {
        return FragmentTasksConditionCronBinding.inflate(inflater, container, false)
    }

    override fun initTitle(): TitleBar? {
        titleBar = super.initTitle()!!.setImmersive(false).setTitle(R.string.task_cron)
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
            val settingVo = Gson().fromJson(eventData, CronSetting::class.java)
            binding!!.tvDescription.text = settingVo.description
            expression = settingVo.expression
            Log.d(TAG, "initViews expression:$expression")

            val fields = expression.split(" ")
            second = fields.getOrNull(0) ?: "*"
            minute = fields.getOrNull(1) ?: "*"
            hour = fields.getOrNull(2) ?: "*"
            day = fields.getOrNull(3) ?: "*"
            month = fields.getOrNull(4) ?: "*"
            week = fields.getOrNull(5) ?: "?"
            year = fields.getOrNull(6) ?: "*"
        }

        //初始化输入提示
        initSecondInputHelper()
        initMinuteInputHelper()
        initHourInputHelper()
        initDayInputHelper()
        initMonthInputHelper()
        initWeekInputHelper()
        initYearInputHelper()
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
        LiveEventBus.get(KEY_TEST_CONDITION, String::class.java).observe(this) {
            mCountDownHelper?.finish()
            switchInputHelper(binding!!.layoutCronExpressionCheck)
            if (it == "success") {
                //生成最近10次运行时间
                val nextTimeList = mutableListOf<String>()
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val cronExpression = CronExpression(expression)
                var nextDate = Date()
                var times = 0
                for (i in 0 until 10) {
                    nextDate = cronExpression.getNextValidTimeAfter(nextDate) ?: break
                    nextTimeList.add(dateFormat.format(nextDate))
                    times++
                }
                binding!!.tvDescription.text = description
                //TODO：低版本Android解析Cron表达式会报错，暂时不处理
                binding!!.tvCronExpressionCheckTips.text = if (expression == description) expression else "$expression\n$description"
                binding!!.tvNextTimeList.text = String.format(getString(R.string.next_execution_times), times.toString(), nextTimeList.joinToString("\n"))
                binding!!.tvNextTimeList.visibility = View.VISIBLE
                binding!!.separatorCronExpressionCheck.visibility = View.VISIBLE
            } else {
                binding!!.tvCronExpressionCheckTips.text = String.format(getString(R.string.invalid_cron_expression), it.toString())
                binding!!.tvNextTimeList.text = ""
                binding!!.tvNextTimeList.visibility = View.GONE
                binding!!.separatorCronExpressionCheck.visibility = View.GONE
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
                            LiveEventBus.get(KEY_TEST_CONDITION, String::class.java).post("success")
                        } catch (e: Exception) {
                            LiveEventBus.get(KEY_TEST_CONDITION, String::class.java).post(e.message.toString())
                            e.printStackTrace()
                            Log.e(TAG, "onClick error:$e")
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
                    intent.putExtra(KEY_BACK_DESCRIPTION_CONDITION, description)
                    intent.putExtra(KEY_BACK_DATA_CONDITION, Gson().toJson(settingVo))
                    setFragmentResult(TASK_CONDITION_CRON, intent)
                    popToBack()
                    return
                }
            }
        } catch (e: Exception) {
            XToastUtils.error(e.message.toString(), 30000)
            e.printStackTrace()
            Log.e(TAG, "onClick error:$e")
        }
    }

    //切换输入提示
    private fun switchInputHelper(layout: LinearLayout) {
        binding!!.layoutSecondType.visibility = View.GONE
        binding!!.layoutMinuteType.visibility = View.GONE
        binding!!.layoutHourType.visibility = View.GONE
        binding!!.layoutDayType.visibility = View.GONE
        binding!!.layoutMonthType.visibility = View.GONE
        binding!!.layoutWeekType.visibility = View.GONE
        binding!!.layoutYearType.visibility = View.GONE
        binding!!.layoutCronExpressionCheck.visibility = View.GONE
        layout.visibility = View.VISIBLE
    }

    //初始化输入提示--秒
    @SuppressLint("SetTextI18n")
    private fun initSecondInputHelper() {
        binding!!.etSecond.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                switchInputHelper(binding!!.layoutSecondType)
            } else {
                afterSecondChanged()
            }
        }
        binding!!.etSecond.setText(second)
        afterSecondChanged()

        //秒类型
        binding!!.rgSecondType.setOnCheckedChangeListener { _: RadioGroup?, checkedId: Int ->
            when (checkedId) {
                R.id.rb_second_type_cyclic -> {
                    var secondCyclicFrom = binding!!.etSecondCyclicFrom.text.toString().trim()
                    if (secondCyclicFrom.isEmpty()) {
                        secondCyclicFrom = "00"
                        binding!!.etSecondCyclicFrom.setText(secondCyclicFrom)
                    }
                    var secondCyclicTo = binding!!.etSecondCyclicTo.text.toString().trim()
                    if (secondCyclicTo.isEmpty()) {
                        secondCyclicTo = "59"
                        binding!!.etSecondCyclicTo.setText(secondCyclicTo)
                    }
                    second = "$secondCyclicFrom-$secondCyclicTo"
                }

                R.id.rb_second_type_interval -> {
                    var secondIntervalStart = binding!!.etSecondIntervalStart.text.toString().trim()
                    if (secondIntervalStart.isEmpty()) {
                        secondIntervalStart = "0"
                        binding!!.etSecondIntervalStart.setText(secondIntervalStart)
                    }
                    var secondInterval = binding!!.etSecondInterval.text.toString().trim()
                    if (secondInterval.isEmpty()) {
                        secondInterval = "2"
                        binding!!.etSecondInterval.setText(secondInterval)
                    }
                    second = "$secondIntervalStart/$secondInterval"
                }

                R.id.rb_second_type_assigned -> {
                    if (selectedSecondList.isEmpty()) {
                        selectedSecondList = "00"
                        binding!!.flowlayoutMultiSelectSecond.setSelectedItems("00")
                    }
                    second = selectedSecondList
                }

                else -> {
                    second = "*"
                }
            }
            binding!!.etSecond.setText(second)
        }

        //初始化输入提示--秒--周期
        val secondCyclicWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                val secondCyclicFrom = binding!!.etSecondCyclicFrom.text.toString().trim()
                val secondCyclicTo = binding!!.etSecondCyclicTo.text.toString().trim()
                if (secondCyclicFrom.isNotEmpty() && secondCyclicTo.isNotEmpty()) {
                    second = "$secondCyclicFrom-$secondCyclicTo"
                    binding!!.etSecond.setText(second)
                    binding!!.rbSecondTypeCyclic.isChecked = true
                }
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
        }
        binding!!.etSecondCyclicFrom.addTextChangedListener(secondCyclicWatcher)
        binding!!.etSecondCyclicTo.addTextChangedListener(secondCyclicWatcher)

        //初始化输入提示--秒--间隔
        val secondIntervalWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                val secondIntervalStart = binding!!.etSecondIntervalStart.text.toString().trim()
                val secondInterval = binding!!.etSecondInterval.text.toString().trim()
                if (secondIntervalStart.isNotEmpty() && secondInterval.isNotEmpty()) {
                    second = "$secondIntervalStart/$secondInterval"
                    binding!!.etSecond.setText(second)
                    binding!!.rbSecondTypeInterval.isChecked = true
                }
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
        }
        binding!!.etSecondIntervalStart.addTextChangedListener(secondIntervalWatcher)
        binding!!.etSecondInterval.addTextChangedListener(secondIntervalWatcher)

        //初始化输入提示--秒--指定
        binding!!.flowlayoutMultiSelectSecond.setTagCheckedMode(FlowTagLayout.FLOW_TAG_CHECKED_MULTI)
        binding!!.flowlayoutMultiSelectSecond.setItems(secondsList)
        binding!!.flowlayoutMultiSelectSecond.setOnTagSelectListener { parent, position, selectedList ->
            selectedSecondList = getSelectedItems(parent, selectedList, 1)
            Log.d(TAG, "position:$position, selectedSecondList:$selectedSecondList")
            if (selectedSecondList.isEmpty()) {
                binding!!.rbSecondTypeAll.isChecked = true
                second = "*"
            } else {
                binding!!.rbSecondTypeAssigned.isChecked = true
                second = selectedSecondList
            }
            binding!!.etSecond.setText(second)
        }
    }

    private fun afterSecondChanged() {
        second = binding!!.etSecond.text.toString().trim()
        try {
            //判断cronExpression是否有效
            expression = "$second $minute $hour $day $month $week $year"
            Log.d(TAG, "afterSecondChanged expression:$expression")
            CronExpression.validateExpression(expression)

            when {
                second == "*" -> {
                    binding!!.rbSecondTypeAll.isChecked = true
                }

                second.contains("/") -> {
                    val secondsArray = second.split("/")
                    binding!!.etSecondIntervalStart.setText(secondsArray.getOrNull(0) ?: "0")
                    binding!!.etSecondInterval.setText(secondsArray.getOrNull(1) ?: "1")
                    binding!!.rbSecondTypeInterval.isChecked = true
                }

                second.contains(",") -> {
                    val secondsList = restoreMergedItems(second, "%02d")
                    Log.d(TAG, "secondsList:$secondsList")
                    binding!!.flowlayoutMultiSelectSecond.setSelectedItems(secondsList)
                    binding!!.rbSecondTypeAssigned.isChecked = true
                    selectedSecondList = secondsList.joinToString(",")
                }

                second.contains("-") -> {
                    val secondsArray = second.split("-")
                    binding!!.etSecondCyclicFrom.setText(secondsArray.getOrNull(0) ?: "00")
                    binding!!.etSecondCyclicTo.setText(secondsArray.getOrNull(1) ?: "59")
                    binding!!.rbSecondTypeCyclic.isChecked = true
                }

                regexNum.matches(second) && secondsList.indexOf(String.format("%02d", second.toInt())) != -1 -> {
                    binding!!.flowlayoutMultiSelectSecond.setSelectedItems(String.format("%02d", second.toInt()))
                    binding!!.rbSecondTypeAssigned.isChecked = true
                    selectedSecondList = second
                }

                else -> {
                    second = "*"
                    binding!!.etSecond.setText(second)
                    binding!!.rbSecondTypeAll.isChecked = true
                }
            }
        } catch (e: Exception) {
            second = "*"
            binding!!.etSecond.setText(second)
            binding!!.rbSecondTypeAll.isChecked = true
            XToastUtils.error("Cron表达式无效：" + e.message, 30000)
        }
    }

    //初始化输入提示--分
    @SuppressLint("SetTextI18n")
    private fun initMinuteInputHelper() {
        binding!!.etMinute.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                switchInputHelper(binding!!.layoutMinuteType)
            } else {
                afterMinuteChanged()
            }
        }
        binding!!.etMinute.setText(minute)
        afterMinuteChanged()

        //分类型
        binding!!.rgMinuteType.setOnCheckedChangeListener { _: RadioGroup?, checkedId: Int ->
            when (checkedId) {
                R.id.rb_minute_type_cyclic -> {
                    var minuteCyclicFrom = binding!!.etMinuteCyclicFrom.text.toString().trim()
                    if (minuteCyclicFrom.isEmpty()) {
                        minuteCyclicFrom = "00"
                        binding!!.etMinuteCyclicFrom.setText(minuteCyclicFrom)
                    }
                    var minuteCyclicTo = binding!!.etMinuteCyclicTo.text.toString().trim()
                    if (minuteCyclicTo.isEmpty()) {
                        minuteCyclicTo = "59"
                        binding!!.etMinuteCyclicTo.setText(minuteCyclicTo)
                    }
                    minute = "$minuteCyclicFrom-$minuteCyclicTo"
                }

                R.id.rb_minute_type_interval -> {
                    var minuteIntervalStart = binding!!.etMinuteIntervalStart.text.toString().trim()
                    if (minuteIntervalStart.isEmpty()) {
                        minuteIntervalStart = "0"
                        binding!!.etMinuteIntervalStart.setText(minuteIntervalStart)
                    }
                    var minuteInterval = binding!!.etMinuteInterval.text.toString().trim()
                    if (minuteInterval.isEmpty()) {
                        minuteInterval = "2"
                        binding!!.etMinuteInterval.setText(minuteInterval)
                    }
                    minute = "$minuteIntervalStart/$minuteInterval"
                }

                R.id.rb_minute_type_assigned -> {
                    if (selectedMinuteList.isEmpty()) {
                        selectedMinuteList = "00"
                        binding!!.flowlayoutMultiSelectMinute.setSelectedItems("00")
                    }
                    minute = selectedMinuteList
                }

                else -> {
                    minute = "*"
                }
            }
            binding!!.etMinute.setText(minute)
        }

        //初始化输入提示--分--周期
        val minuteCyclicWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                val minuteCyclicFrom = binding!!.etMinuteCyclicFrom.text.toString().trim()
                val minuteCyclicTo = binding!!.etMinuteCyclicTo.text.toString().trim()
                if (minuteCyclicFrom.isNotEmpty() && minuteCyclicTo.isNotEmpty()) {
                    minute = "$minuteCyclicFrom-$minuteCyclicTo"
                    binding!!.etMinute.setText(minute)
                    binding!!.rbMinuteTypeCyclic.isChecked = true
                }
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
        }
        binding!!.etMinuteCyclicFrom.addTextChangedListener(minuteCyclicWatcher)
        binding!!.etMinuteCyclicTo.addTextChangedListener(minuteCyclicWatcher)

        //初始化输入提示--分--间隔
        val minuteIntervalWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                val minuteIntervalStart = binding!!.etMinuteIntervalStart.text.toString().trim()
                val minuteInterval = binding!!.etMinuteInterval.text.toString().trim()
                if (minuteIntervalStart.isNotEmpty() && minuteInterval.isNotEmpty()) {
                    minute = "$minuteIntervalStart/$minuteInterval"
                    binding!!.etMinute.setText(minute)
                    binding!!.rbMinuteTypeInterval.isChecked = true
                }
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
        }
        binding!!.etMinuteIntervalStart.addTextChangedListener(minuteIntervalWatcher)
        binding!!.etMinuteInterval.addTextChangedListener(minuteIntervalWatcher)

        //初始化输入提示--分--指定
        binding!!.flowlayoutMultiSelectMinute.setTagCheckedMode(FlowTagLayout.FLOW_TAG_CHECKED_MULTI)
        binding!!.flowlayoutMultiSelectMinute.setItems(minutesList)
        binding!!.flowlayoutMultiSelectMinute.setOnTagSelectListener { parent, position, selectedList ->
            selectedMinuteList = getSelectedItems(parent, selectedList, 1)
            Log.d(TAG, "position:$position, selectedMinutesList:$selectedMinuteList")
            if (selectedMinuteList.isEmpty()) {
                binding!!.rbMinuteTypeAll.isChecked = true
                minute = "*"
            } else {
                binding!!.rbMinuteTypeAssigned.isChecked = true
                minute = selectedMinuteList
            }
            binding!!.etMinute.setText(minute)
        }
    }

    private fun afterMinuteChanged() {
        minute = binding!!.etMinute.text.toString().trim()
        try {
            //判断cronExpression是否有效
            expression = "$second $minute $hour $day $month $week $year"
            Log.d(TAG, "afterMinuteChanged expression:$expression")
            CronExpression.validateExpression(expression)

            when {
                minute == "*" -> {
                    binding!!.rbMinuteTypeAll.isChecked = true
                }

                minute.contains("/") -> {
                    val minutesArray = minute.split("/")
                    binding!!.etMinuteIntervalStart.setText(minutesArray.getOrNull(0) ?: "0")
                    binding!!.etMinuteInterval.setText(minutesArray.getOrNull(1) ?: "1")
                    binding!!.rbMinuteTypeInterval.isChecked = true
                }

                minute.contains(",") -> {
                    val minutesList = restoreMergedItems(minute, "%02d")
                    Log.d(TAG, "minutesList:$minutesList")
                    binding!!.flowlayoutMultiSelectMinute.setSelectedItems(minutesList)
                    binding!!.rbMinuteTypeAssigned.isChecked = true
                    selectedMinuteList = minutesList.joinToString(",")
                }

                minute.contains("-") -> {
                    val minutesArray = minute.split("-")
                    binding!!.etMinuteCyclicFrom.setText(minutesArray.getOrNull(0) ?: "00")
                    binding!!.etMinuteCyclicTo.setText(minutesArray.getOrNull(1) ?: "59")
                    binding!!.rbMinuteTypeCyclic.isChecked = true
                }

                regexNum.matches(minute) && minutesList.indexOf(String.format("%02d", minute.toInt())) != -1 -> {
                    binding!!.flowlayoutMultiSelectMinute.setSelectedItems(String.format("%02d", minute.toInt()))
                    binding!!.rbMinuteTypeAssigned.isChecked = true
                    selectedMinuteList = minute
                }

                else -> {
                    minute = "*"
                    binding!!.etMinute.setText(minute)
                    binding!!.rbMinuteTypeAll.isChecked = true
                }
            }
        } catch (e: Exception) {
            minute = "*"
            binding!!.etMinute.setText(minute)
            binding!!.rbMinuteTypeAll.isChecked = true
            XToastUtils.error("Cron表达式无效：" + e.message, 30000)
        }
    }

    //初始化输入提示--时
    @SuppressLint("SetTextI18n")
    private fun initHourInputHelper() {
        binding!!.etHour.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                switchInputHelper(binding!!.layoutHourType)
            } else {
                afterHourChanged()
            }
        }
        binding!!.etHour.setText(hour)
        afterHourChanged()

        //时类型
        binding!!.rgHourType.setOnCheckedChangeListener { _: RadioGroup?, checkedId: Int ->
            when (checkedId) {
                R.id.rb_hour_type_cyclic -> {
                    var hourCyclicFrom = binding!!.etHourCyclicFrom.text.toString().trim()
                    if (hourCyclicFrom.isEmpty()) {
                        hourCyclicFrom = "00"
                        binding!!.etHourCyclicFrom.setText(hourCyclicFrom)
                    }
                    var hourCyclicTo = binding!!.etHourCyclicTo.text.toString().trim()
                    if (hourCyclicTo.isEmpty()) {
                        hourCyclicTo = "23"
                        binding!!.etHourCyclicTo.setText(hourCyclicTo)
                    }
                    hour = "$hourCyclicFrom-$hourCyclicTo"
                }

                R.id.rb_hour_type_interval -> {
                    var hourIntervalStart = binding!!.etHourIntervalStart.text.toString().trim()
                    if (hourIntervalStart.isEmpty()) {
                        hourIntervalStart = "0"
                        binding!!.etHourIntervalStart.setText(hourIntervalStart)
                    }
                    var hourInterval = binding!!.etHourInterval.text.toString().trim()
                    if (hourInterval.isEmpty()) {
                        hourInterval = "2"
                        binding!!.etHourInterval.setText(hourInterval)
                    }
                    hour = "$hourIntervalStart/$hourInterval"
                }

                R.id.rb_hour_type_assigned -> {
                    if (selectedHourList.isEmpty()) {
                        selectedHourList = "00"
                        binding!!.flowlayoutMultiSelectHour.setSelectedItems("00")
                    }
                    hour = selectedHourList
                }

                else -> {
                    hour = "*"
                }
            }
            binding!!.etHour.setText(hour)
        }

        //初始化输入提示--时--周期
        val hourCyclicWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                val hourCyclicFrom = binding!!.etHourCyclicFrom.text.toString().trim()
                val hourCyclicTo = binding!!.etHourCyclicTo.text.toString().trim()
                if (hourCyclicFrom.isNotEmpty() && hourCyclicTo.isNotEmpty()) {
                    hour = "$hourCyclicFrom-$hourCyclicTo"
                    binding!!.etHour.setText(hour)
                    binding!!.rbHourTypeCyclic.isChecked = true
                }
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
        }
        binding!!.etHourCyclicFrom.addTextChangedListener(hourCyclicWatcher)
        binding!!.etHourCyclicTo.addTextChangedListener(hourCyclicWatcher)

        //初始化输入提示--时--间隔
        val hourIntervalWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                val hourIntervalStart = binding!!.etHourIntervalStart.text.toString().trim()
                val hourInterval = binding!!.etHourInterval.text.toString().trim()
                if (hourIntervalStart.isNotEmpty() && hourInterval.isNotEmpty()) {
                    hour = "$hourIntervalStart/$hourInterval"
                    binding!!.etHour.setText(hour)
                    binding!!.rbHourTypeInterval.isChecked = true
                }
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
        }
        binding!!.etHourIntervalStart.addTextChangedListener(hourIntervalWatcher)
        binding!!.etHourInterval.addTextChangedListener(hourIntervalWatcher)

        //初始化输入提示--时--指定
        binding!!.flowlayoutMultiSelectHour.setTagCheckedMode(FlowTagLayout.FLOW_TAG_CHECKED_MULTI)
        binding!!.flowlayoutMultiSelectHour.setItems(hoursList)
        binding!!.flowlayoutMultiSelectHour.setOnTagSelectListener { parent, position, selectedList ->
            selectedHourList = getSelectedItems(parent, selectedList, 1)
            Log.d(TAG, "position:$position, selectedHoursList:$selectedHourList")
            if (selectedHourList.isEmpty()) {
                binding!!.rbHourTypeAll.isChecked = true
                hour = "*"
            } else {
                binding!!.rbHourTypeAssigned.isChecked = true
                hour = selectedHourList
            }
            binding!!.etHour.setText(hour)
        }
    }

    private fun afterHourChanged() {
        hour = binding!!.etHour.text.toString().trim()
        try {
            //判断cronExpression是否有效
            expression = "$second $minute $hour $day $month $week $year"
            Log.d(TAG, "afterHourChanged expression:$expression")
            CronExpression.validateExpression(expression)

            when {
                hour == "*" -> {
                    binding!!.rbHourTypeAll.isChecked = true
                }

                hour.contains("/") -> {
                    val hoursArray = hour.split("/")
                    binding!!.etHourIntervalStart.setText(hoursArray.getOrNull(0) ?: "0")
                    binding!!.etHourInterval.setText(hoursArray.getOrNull(1) ?: "1")
                    binding!!.rbHourTypeInterval.isChecked = true
                }

                hour.contains(",") -> {
                    val hoursList = restoreMergedItems(hour, "%02d")
                    Log.d(TAG, "hoursList:$hoursList")
                    binding!!.flowlayoutMultiSelectHour.setSelectedItems(hoursList)
                    binding!!.rbHourTypeAssigned.isChecked = true
                    selectedHourList = hoursList.joinToString(",")
                }

                hour.contains("-") -> {
                    val hoursArray = hour.split("-")
                    binding!!.etHourCyclicFrom.setText(hoursArray.getOrNull(0) ?: "00")
                    binding!!.etHourCyclicTo.setText(hoursArray.getOrNull(1) ?: "23")
                    binding!!.rbHourTypeCyclic.isChecked = true
                }

                regexNum.matches(hour) && hoursList.indexOf(String.format("%02d", hour.toInt())) != -1 -> {
                    binding!!.flowlayoutMultiSelectHour.setSelectedItems(String.format("%02d", hour.toInt()))
                    binding!!.rbHourTypeAssigned.isChecked = true
                    selectedHourList = hour
                }

                else -> {
                    hour = "*"
                    binding!!.etHour.setText(hour)
                    binding!!.rbHourTypeAll.isChecked = true
                }
            }
        } catch (e: Exception) {
            hour = "*"
            binding!!.etHour.setText(hour)
            binding!!.rbHourTypeAll.isChecked = true
            XToastUtils.error("Cron表达式无效：" + e.message, 30000)
        }
    }

    //初始化输入提示--日
    @SuppressLint("SetTextI18n")
    private fun initDayInputHelper() {
        binding!!.etDay.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                switchInputHelper(binding!!.layoutDayType)
            } else {
                afterDayChanged()
            }
        }
        binding!!.etDay.setText(day)
        afterDayChanged()

        //日类型
        binding!!.rgDayType.setOnCheckedChangeListener { _: RadioGroup?, checkedId: Int ->
            when (checkedId) {
                R.id.rb_day_type_cyclic -> {
                    var dayCyclicFrom = binding!!.etDayCyclicFrom.text.toString().trim()
                    if (dayCyclicFrom.isEmpty()) {
                        dayCyclicFrom = "1"
                        binding!!.etDayCyclicFrom.setText(dayCyclicFrom)
                    }
                    var dayCyclicTo = binding!!.etDayCyclicTo.text.toString().trim()
                    if (dayCyclicTo.isEmpty()) {
                        dayCyclicTo = "31"
                        binding!!.etDayCyclicTo.setText(dayCyclicTo)
                    }
                    day = "$dayCyclicFrom-$dayCyclicTo"
                }

                R.id.rb_day_type_interval -> {
                    var dayIntervalStart = binding!!.etDayIntervalStart.text.toString().trim()
                    if (dayIntervalStart.isEmpty()) {
                        dayIntervalStart = "1"
                        binding!!.etDayIntervalStart.setText(dayIntervalStart)
                    }
                    var dayInterval = binding!!.etDayInterval.text.toString().trim()
                    if (dayInterval.isEmpty()) {
                        dayInterval = "2"
                        binding!!.etDayInterval.setText(dayInterval)
                    }
                    day = "$dayIntervalStart/$dayInterval"
                }

                R.id.rb_day_type_assigned -> {
                    if (selectedDayList.isEmpty()) {
                        selectedDayList = "1"
                        binding!!.flowlayoutMultiSelectDay.setSelectedItems("1")
                    }
                    day = selectedDayList
                }

                R.id.rb_day_type_last_day_of_month -> {
                    day = "L"
                }

                R.id.rb_day_type_last_day_of_month_recent_day -> {
                    day = "LW"
                }

                R.id.rb_day_type_recent_day -> {
                    var recentDay = binding!!.etRecentDay.text.toString().trim()
                    if (recentDay.isEmpty()) {
                        recentDay = "1"
                        binding!!.etRecentDay.setText(recentDay)
                    }
                    day = recentDay + "W"
                }

                R.id.rb_day_type_not_assigned -> {
                    day = "?"
                }

                else -> {
                    day = "*"
                }
            }
            binding!!.etDay.setText(day)
        }

        //初始化输入提示--日--周期
        val dayCyclicWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                val dayCyclicFrom = binding!!.etDayCyclicFrom.text.toString().trim()
                val dayCyclicTo = binding!!.etDayCyclicTo.text.toString().trim()
                if (dayCyclicFrom.isNotEmpty() && dayCyclicTo.isNotEmpty()) {
                    day = "$dayCyclicFrom-$dayCyclicTo"
                    binding!!.etDay.setText(day)
                    binding!!.rbDayTypeCyclic.isChecked = true
                }
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
        }
        binding!!.etDayCyclicFrom.addTextChangedListener(dayCyclicWatcher)
        binding!!.etDayCyclicTo.addTextChangedListener(dayCyclicWatcher)

        //初始化输入提示--日--间隔
        val dayIntervalWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                val dayIntervalStart = binding!!.etDayIntervalStart.text.toString().trim()
                val dayInterval = binding!!.etDayInterval.text.toString().trim()
                if (dayIntervalStart.isNotEmpty() && dayInterval.isNotEmpty()) {
                    day = "$dayIntervalStart/$dayInterval"
                    binding!!.etDay.setText(day)
                    binding!!.rbDayTypeInterval.isChecked = true
                }
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
        }
        binding!!.etDayIntervalStart.addTextChangedListener(dayIntervalWatcher)
        binding!!.etDayInterval.addTextChangedListener(dayIntervalWatcher)

        //初始化输入提示--日--指定
        binding!!.flowlayoutMultiSelectDay.setTagCheckedMode(FlowTagLayout.FLOW_TAG_CHECKED_MULTI)
        binding!!.flowlayoutMultiSelectDay.setItems(dayList)
        binding!!.flowlayoutMultiSelectDay.setOnTagSelectListener { parent, position, selectedList ->
            selectedDayList = getSelectedItems(parent, selectedList)
            Log.d(TAG, "position:$position, selectedDayList:$selectedDayList")
            if (selectedDayList.isEmpty()) {
                binding!!.rbDayTypeAll.isChecked = true
                day = "*"
            } else {
                binding!!.rbDayTypeAssigned.isChecked = true
                day = selectedDayList
            }
            binding!!.etDay.setText(day)
        }
    }

    private fun afterDayChanged() {
        //周和日不能同时设置
        day = binding!!.etDay.text.toString().trim()
        if (day != "?" && week != "?") {
            week = "?"
            binding!!.etWeek.setText(week)
        }

        try {
            //判断cronExpression是否有效
            expression = "$second $minute $hour $day $month $week $year"
            Log.d(TAG, "afterDayChanged expression:$expression")
            CronExpression.validateExpression(expression)

            when {
                day == "*" -> {
                    binding!!.rbDayTypeAll.isChecked = true
                }

                day == "?" -> {
                    binding!!.rbDayTypeNotAssigned.isChecked = true
                }

                day == "L" -> {
                    binding!!.rbDayTypeLastDayOfMonth.isChecked = true
                }

                day == "LW" -> {
                    binding!!.rbDayTypeLastDayOfMonthRecentDay.isChecked = true
                    return
                }

                day.endsWith("W") -> {
                    binding!!.rbDayTypeRecentDay.isChecked = true
                    binding!!.etRecentDay.setText(day.removeSuffix("W"))
                    return
                }

                day.contains("/") -> {
                    val dayArray = day.split("/")
                    binding!!.etDayIntervalStart.setText(dayArray.getOrNull(0) ?: "0")
                    binding!!.etDayInterval.setText(dayArray.getOrNull(1) ?: "1")
                    binding!!.rbDayTypeInterval.isChecked = true
                }

                day.contains(",") -> {
                    val dayList = restoreMergedItems(day, "%d")
                    Log.d(TAG, "dayList:$dayList")
                    binding!!.flowlayoutMultiSelectDay.setSelectedItems(dayList)
                    binding!!.rbDayTypeAssigned.isChecked = true
                    selectedDayList = dayList.joinToString(",")
                }

                day.contains("-") -> {
                    val dayArray = day.split("-")
                    binding!!.etDayCyclicFrom.setText(dayArray.getOrNull(0) ?: "1")
                    binding!!.etDayCyclicTo.setText(dayArray.getOrNull(1) ?: "31")
                    binding!!.rbDayTypeCyclic.isChecked = true
                }

                regexNum.matches(day) && dayList.indexOf(String.format("%d", day.toInt())) != -1 -> {
                    binding!!.flowlayoutMultiSelectDay.setSelectedItems(String.format("%d", day.toInt()))
                    binding!!.rbDayTypeAssigned.isChecked = true
                    selectedDayList = day
                }

                else -> {
                    day = "*"
                    binding!!.etDay.setText(day)
                    binding!!.rbDayTypeAll.isChecked = true
                }
            }
        } catch (e: Exception) {
            day = "*"
            binding!!.etDay.setText(day)
            binding!!.rbDayTypeAll.isChecked = true
            XToastUtils.error("Cron表达式无效：" + e.message, 30000)
        }
    }

    //初始化输入提示--月
    @SuppressLint("SetTextI18n")
    private fun initMonthInputHelper() {
        binding!!.etMonth.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                switchInputHelper(binding!!.layoutMonthType)
            } else {
                afterMonthChanged()
            }
        }
        binding!!.etMonth.setText(month)
        afterMonthChanged()

        //月类型
        binding!!.rgMonthType.setOnCheckedChangeListener { _: RadioGroup?, checkedId: Int ->
            when (checkedId) {
                R.id.rb_month_type_cyclic -> {
                    var monthCyclicFrom = binding!!.etMonthCyclicFrom.text.toString().trim()
                    if (monthCyclicFrom.isEmpty()) {
                        monthCyclicFrom = "1"
                        binding!!.etMonthCyclicFrom.setText(monthCyclicFrom)
                    }
                    var monthCyclicTo = binding!!.etMonthCyclicTo.text.toString().trim()
                    if (monthCyclicTo.isEmpty()) {
                        monthCyclicTo = "12"
                        binding!!.etMonthCyclicTo.setText(monthCyclicTo)
                    }
                    month = "$monthCyclicFrom-$monthCyclicTo"
                }

                R.id.rb_month_type_interval -> {
                    var monthIntervalStart = binding!!.etMonthIntervalStart.text.toString().trim()
                    if (monthIntervalStart.isEmpty()) {
                        monthIntervalStart = "1"
                        binding!!.etMonthIntervalStart.setText(monthIntervalStart)
                    }
                    var monthInterval = binding!!.etMonthInterval.text.toString().trim()
                    if (monthInterval.isEmpty()) {
                        monthInterval = "2"
                        binding!!.etMonthInterval.setText(monthInterval)
                    }
                    month = "$monthIntervalStart/$monthInterval"
                }

                R.id.rb_month_type_assigned -> {
                    if (selectedMonthList.isEmpty()) {
                        selectedMonthList = "1"
                        binding!!.flowlayoutMultiSelectMonth.setSelectedItems("1")
                    }
                    month = selectedMonthList
                }

                else -> {
                    month = "*"
                }
            }
            binding!!.etMonth.setText(month)
        }

        //初始化输入提示--月--周期
        val monthCyclicWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                val monthCyclicFrom = binding!!.etMonthCyclicFrom.text.toString().trim()
                val monthCyclicTo = binding!!.etMonthCyclicTo.text.toString().trim()
                if (monthCyclicFrom.isNotEmpty() && monthCyclicTo.isNotEmpty()) {
                    month = "$monthCyclicFrom-$monthCyclicTo"
                    binding!!.etMonth.setText(month)
                    binding!!.rbMonthTypeCyclic.isChecked = true
                }
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
        }
        binding!!.etMonthCyclicFrom.addTextChangedListener(monthCyclicWatcher)
        binding!!.etMonthCyclicTo.addTextChangedListener(monthCyclicWatcher)

        //初始化输入提示--月--间隔
        val monthIntervalWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                val monthIntervalStart = binding!!.etMonthIntervalStart.text.toString().trim()
                val monthInterval = binding!!.etMonthInterval.text.toString().trim()
                if (monthIntervalStart.isNotEmpty() && monthInterval.isNotEmpty()) {
                    month = "$monthIntervalStart/$monthInterval"
                    binding!!.etMonth.setText(month)
                    binding!!.rbMonthTypeInterval.isChecked = true
                }
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
        }
        binding!!.etMonthIntervalStart.addTextChangedListener(monthIntervalWatcher)
        binding!!.etMonthInterval.addTextChangedListener(monthIntervalWatcher)

        //初始化输入提示--月--指定
        binding!!.flowlayoutMultiSelectMonth.setTagCheckedMode(FlowTagLayout.FLOW_TAG_CHECKED_MULTI)
        binding!!.flowlayoutMultiSelectMonth.setItems(monthList)
        binding!!.flowlayoutMultiSelectMonth.setOnTagSelectListener { parent, position, selectedList ->
            selectedMonthList = getSelectedItems(parent, selectedList)
            Log.d(TAG, "position:$position, selectedMonthList:$selectedMonthList")
            if (selectedMonthList.isEmpty()) {
                binding!!.rbMonthTypeAll.isChecked = true
                month = "*"
            } else {
                binding!!.rbMonthTypeAssigned.isChecked = true
                month = selectedMonthList
            }
            binding!!.etMonth.setText(month)
        }
    }

    private fun afterMonthChanged() {
        month = binding!!.etMonth.text.toString().trim()
        try {
            //判断cronExpression是否有效
            expression = "$second $minute $hour $day $month $week $year"
            Log.d(TAG, "afterMonthChanged expression:$expression")
            CronExpression.validateExpression(expression)

            when {
                month == "*" -> {
                    binding!!.rbMonthTypeAll.isChecked = true
                }

                month.contains("/") -> {
                    val monthArray = month.split("/")
                    binding!!.etMonthIntervalStart.setText(monthArray.getOrNull(0) ?: "0")
                    binding!!.etMonthInterval.setText(monthArray.getOrNull(1) ?: "1")
                    binding!!.rbMonthTypeInterval.isChecked = true
                }

                month.contains(",") -> {
                    val monthList = restoreMergedItems(month, "%d")
                    Log.d(TAG, "monthList:$monthList")
                    binding!!.flowlayoutMultiSelectMonth.setSelectedItems(monthList)
                    binding!!.rbMonthTypeAssigned.isChecked = true
                    selectedMonthList = monthList.joinToString(",")
                }

                month.contains("-") -> {
                    val monthArray = month.split("-")
                    binding!!.etMonthCyclicFrom.setText(monthArray.getOrNull(0) ?: "1")
                    binding!!.etMonthCyclicTo.setText(monthArray.getOrNull(1) ?: "31")
                    binding!!.rbMonthTypeCyclic.isChecked = true
                }

                monthList.indexOf(month) != -1 -> {
                    binding!!.flowlayoutMultiSelectMonth.setSelectedItems(month)
                    binding!!.rbMonthTypeAssigned.isChecked = true
                    selectedMonthList = month
                }

                else -> {
                    month = "*"
                    binding!!.etMonth.setText(month)
                    binding!!.rbMonthTypeAll.isChecked = true
                }
            }
        } catch (e: Exception) {
            month = "*"
            binding!!.etMonth.setText(month)
            binding!!.rbMonthTypeAll.isChecked = true
            XToastUtils.error("Cron表达式无效：" + e.message, 30000)
        }
    }

    //初始化输入提示--周
    private fun initWeekInputHelper() {
        binding!!.etWeek.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                switchInputHelper(binding!!.layoutWeekType)
            } else {
                afterWeekChanged()
            }
        }
        binding!!.etWeek.setText(week)
        afterWeekChanged()

        //周类型
        binding!!.rgWeekType.setOnCheckedChangeListener { _: RadioGroup?, checkedId: Int ->
            when (checkedId) {
                R.id.rb_week_type_cyclic -> {
                    var weekCyclicFrom = binding!!.etWeekCyclicFrom.text.toString().trim()
                    if (weekCyclicFrom.isEmpty()) {
                        weekCyclicFrom = "1"
                        binding!!.etWeekCyclicFrom.setText(weekCyclicFrom)
                    }
                    var weekCyclicTo = binding!!.etWeekCyclicTo.text.toString().trim()
                    if (weekCyclicTo.isEmpty()) {
                        weekCyclicTo = "7"
                        binding!!.etWeekCyclicTo.setText(weekCyclicTo)
                    }
                    week = "$weekCyclicFrom-$weekCyclicTo"
                }

                R.id.rb_week_type_weeks_of_week -> {
                    var whichWeekOfMonth = binding!!.etWhichWeekOfMonth.text.toString().trim()
                    if (whichWeekOfMonth.isEmpty()) {
                        whichWeekOfMonth = "1"
                        binding!!.etWhichWeekOfMonth.setText(whichWeekOfMonth)
                    }
                    var whichDayOfWeek = binding!!.etWhichDayOfWeek.text.toString().trim()
                    if (whichDayOfWeek.isEmpty()) {
                        whichDayOfWeek = "1"
                        binding!!.etWhichDayOfWeek.setText(whichDayOfWeek)
                    }
                    week = "$whichWeekOfMonth#$whichDayOfWeek"
                }

                R.id.rb_week_type_assigned -> {
                    if (selectedWeekList.isEmpty()) {
                        selectedWeekList = "1"
                        binding!!.flowlayoutMultiSelectWeek.setSelectedItems("1")
                    }
                    week = selectedWeekList
                }

                R.id.rb_week_type_last_week_of_month -> {
                    var lastWeekOfMonth = binding!!.etLastWeekOfMonth.text.toString().trim()
                    if (lastWeekOfMonth.isEmpty()) {
                        lastWeekOfMonth = "1"
                        binding!!.etLastWeekOfMonth.setText(lastWeekOfMonth)
                    }
                    week = lastWeekOfMonth + "L"
                }

                R.id.rb_week_type_all -> {
                    week = "*"
                }

                else -> {
                    week = "?"
                }
            }
            binding!!.etWeek.setText(week)
        }

        //初始化输入提示--周--周期
        val weekCyclicWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                val weekCyclicFrom = binding!!.etWeekCyclicFrom.text.toString().trim()
                val weekCyclicTo = binding!!.etWeekCyclicTo.text.toString().trim()
                if (weekCyclicFrom.isNotEmpty() && weekCyclicTo.isNotEmpty()) {
                    week = "$weekCyclicFrom-$weekCyclicTo"
                    binding!!.etWeek.setText(week)
                    binding!!.rbWeekTypeCyclic.isChecked = true
                }
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
        }
        binding!!.etWeekCyclicFrom.addTextChangedListener(weekCyclicWatcher)
        binding!!.etWeekCyclicTo.addTextChangedListener(weekCyclicWatcher)

        //初始化输入提示--周--间隔
        val weeksOfWeekWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                val whichWeekOfMonth = binding!!.etWhichWeekOfMonth.text.toString().trim()
                val whichDayOfWeek = binding!!.etWhichDayOfWeek.text.toString().trim()
                if (whichWeekOfMonth.isNotEmpty() && whichDayOfWeek.isNotEmpty()) {
                    week = "$whichWeekOfMonth#$whichDayOfWeek"
                    binding!!.etWeek.setText(week)
                    binding!!.rbWeekTypeWeeksOfWeek.isChecked = true
                }
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
        }
        binding!!.etWhichWeekOfMonth.addTextChangedListener(weeksOfWeekWatcher)
        binding!!.etWhichDayOfWeek.addTextChangedListener(weeksOfWeekWatcher)

        //初始化输入提示--周--指定
        binding!!.flowlayoutMultiSelectWeek.setTagCheckedMode(FlowTagLayout.FLOW_TAG_CHECKED_MULTI)
        binding!!.flowlayoutMultiSelectWeek.setItems(weekList)
        binding!!.flowlayoutMultiSelectWeek.setOnTagSelectListener { parent, position, selectedList ->
            selectedWeekList = getSelectedItems(parent, selectedList)
            Log.d(TAG, "position:$position, selectedWeekList:$selectedWeekList")
            if (selectedWeekList.isEmpty()) {
                binding!!.rbWeekTypeAll.isChecked = true
                week = "?"
            } else {
                binding!!.rbWeekTypeAssigned.isChecked = true
                week = selectedWeekList
            }
            binding!!.etWeek.setText(week)
        }

        //初始化输入提示--周--本月最后
        binding!!.etLastWeekOfMonth.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                val lastWeekOfMonth = binding!!.etLastWeekOfMonth.text.toString().trim()
                if (lastWeekOfMonth.isNotEmpty()) {
                    week = lastWeekOfMonth + "L"
                    binding!!.etWeek.setText(week)
                    binding!!.rbWeekTypeLastWeekOfMonth.isChecked = true
                } else {
                    week = "*"
                    binding!!.rbWeekTypeAll.isChecked = true
                }
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
        })
    }

    private fun afterWeekChanged() {
        //周和日不能同时设置
        week = binding!!.etWeek.text.toString().trim()
        if (day != "?" && week != "?") {
            day = "?"
            binding!!.etDay.setText(day)
        }

        try {
            //判断cronExpression是否有效
            expression = "$second $minute $hour $day $month $week $year"
            Log.d(TAG, "afterWeekChanged expression:$expression")
            CronExpression.validateExpression(expression)

            when {
                week == "*" -> {
                    binding!!.rbWeekTypeAll.isChecked = true
                }

                week == "?" -> {
                    binding!!.rbWeekTypeNotAssigned.isChecked = true
                }

                week.contains(",") -> {
                    val weekList = restoreMergedItems(week, "%d")
                    Log.d(TAG, "weekList:$weekList")
                    binding!!.flowlayoutMultiSelectWeek.setSelectedItems(weekList)
                    binding!!.rbWeekTypeAssigned.isChecked = true
                    selectedWeekList = weekList.joinToString(",")
                }

                week.contains("-") -> {
                    val weekArray = week.split("-")
                    binding!!.etWeekCyclicFrom.setText(weekArray.getOrNull(0) ?: "1")
                    binding!!.etWeekCyclicTo.setText(weekArray.getOrNull(1) ?: "31")
                    binding!!.rbWeekTypeCyclic.isChecked = true
                }

                week.contains("#") -> {
                    val weekArray = week.split("#")
                    binding!!.etWhichWeekOfMonth.setText(weekArray.getOrNull(0) ?: "1")
                    binding!!.etWhichDayOfWeek.setText(weekArray.getOrNull(1) ?: "1")
                    binding!!.rbWeekTypeWeeksOfWeek.isChecked = true
                }

                weekList.indexOf(week) != -1 -> {
                    binding!!.flowlayoutMultiSelectWeek.setSelectedItems(week)
                    binding!!.rbWeekTypeAssigned.isChecked = true
                    selectedWeekList = week
                }

                week.endsWith("L") -> {
                    binding!!.rbWeekTypeLastWeekOfMonth.isChecked = true
                    binding!!.etLastWeekOfMonth.setText(week.removeSuffix("L"))
                }

                else -> {
                    week = "*"
                    binding!!.etWeek.setText(week)
                    binding!!.rbWeekTypeAll.isChecked = true
                }
            }
        } catch (e: Exception) {
            week = "*"
            binding!!.etWeek.setText(week)
            binding!!.rbWeekTypeAll.isChecked = true
            XToastUtils.error("Cron表达式无效：" + e.message, 30000)
        }
    }

    //初始化输入提示--年
    @SuppressLint("SetTextI18n")
    private fun initYearInputHelper() {
        binding!!.etYear.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                switchInputHelper(binding!!.layoutYearType)
            } else {
                afterYearChanged()
            }
        }
        binding!!.etYear.setText(year)
        afterYearChanged()

        //年类型
        binding!!.rgYearType.setOnCheckedChangeListener { _: RadioGroup?, checkedId: Int ->
            when (checkedId) {
                R.id.rb_year_type_cyclic -> {
                    var yearCyclicFrom = binding!!.etYearCyclicFrom.text.toString().trim()
                    if (yearCyclicFrom.isEmpty()) {
                        yearCyclicFrom = "2023"
                        binding!!.etYearCyclicFrom.setText(yearCyclicFrom)
                    }
                    var yearCyclicTo = binding!!.etYearCyclicTo.text.toString().trim()
                    if (yearCyclicTo.isEmpty()) {
                        yearCyclicTo = "2058"
                        binding!!.etYearCyclicTo.setText(yearCyclicTo)
                    }
                    year = "$yearCyclicFrom-$yearCyclicTo"
                }

                R.id.rb_year_type_interval -> {
                    var yearIntervalStart = binding!!.etYearIntervalStart.text.toString().trim()
                    if (yearIntervalStart.isEmpty()) {
                        yearIntervalStart = "2023"
                        binding!!.etYearIntervalStart.setText(yearIntervalStart)
                    }
                    var yearInterval = binding!!.etYearInterval.text.toString().trim()
                    if (yearInterval.isEmpty()) {
                        yearInterval = "2"
                        binding!!.etYearInterval.setText(yearInterval)
                    }
                    year = "$yearIntervalStart/$yearInterval"
                }

                R.id.rb_year_type_assigned -> {
                    if (selectedYearList.isEmpty()) {
                        selectedYearList = "1"
                        binding!!.flowlayoutMultiSelectYear.setSelectedItems("1")
                    }
                    year = selectedYearList
                }

                else -> {
                    year = "*"
                }
            }
            binding!!.etYear.setText(year)
        }

        //初始化输入提示--年--周期
        val yearCyclicWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                val yearCyclicFrom = binding!!.etYearCyclicFrom.text.toString().trim()
                val yearCyclicTo = binding!!.etYearCyclicTo.text.toString().trim()
                if (yearCyclicFrom.isNotEmpty() && yearCyclicTo.isNotEmpty()) {
                    year = "$yearCyclicFrom-$yearCyclicTo"
                    binding!!.etYear.setText(year)
                    binding!!.rbYearTypeCyclic.isChecked = true
                }
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
        }
        binding!!.etYearCyclicFrom.addTextChangedListener(yearCyclicWatcher)
        binding!!.etYearCyclicTo.addTextChangedListener(yearCyclicWatcher)

        //初始化输入提示--年--间隔
        val yearIntervalWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                val yearIntervalStart = binding!!.etYearIntervalStart.text.toString().trim()
                val yearInterval = binding!!.etYearInterval.text.toString().trim()
                if (yearIntervalStart.isNotEmpty() && yearInterval.isNotEmpty()) {
                    year = "$yearIntervalStart/$yearInterval"
                    binding!!.etYear.setText(year)
                    binding!!.rbYearTypeInterval.isChecked = true
                }
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
        }
        binding!!.etYearIntervalStart.addTextChangedListener(yearIntervalWatcher)
        binding!!.etYearInterval.addTextChangedListener(yearIntervalWatcher)

        //初始化输入提示--年--指定
        binding!!.flowlayoutMultiSelectYear.setTagCheckedMode(FlowTagLayout.FLOW_TAG_CHECKED_MULTI)
        binding!!.flowlayoutMultiSelectYear.setItems(yearList)
        binding!!.flowlayoutMultiSelectYear.setOnTagSelectListener { parent, position, selectedList ->
            selectedYearList = getSelectedItems(parent, selectedList)
            Log.d(TAG, "position:$position, selectedYearList:$selectedYearList")
            if (selectedYearList.isEmpty()) {
                binding!!.rbYearTypeAll.isChecked = true
                year = "*"
            } else {
                binding!!.rbYearTypeAssigned.isChecked = true
                year = selectedYearList
            }
            binding!!.etYear.setText(year)
        }
    }

    private fun afterYearChanged() {
        year = binding!!.etYear.text.toString().trim()
        try {
            //判断cronExpression是否有效
            expression = "$second $minute $hour $day $month $week $year"
            Log.d(TAG, "afterYearChanged expression:$expression")
            CronExpression.validateExpression(expression)

            when {
                year == "*" -> {
                    binding!!.rbYearTypeAll.isChecked = true
                }

                year == "?" -> {
                    binding!!.rbYearTypeNotAssigned.isChecked = true
                }

                year.contains("/") -> {
                    val yearArray = year.split("/")
                    binding!!.etYearIntervalStart.setText(yearArray.getOrNull(0) ?: "2023")
                    binding!!.etYearInterval.setText(yearArray.getOrNull(1) ?: "2")
                    binding!!.rbYearTypeInterval.isChecked = true
                }

                year.contains(",") -> {
                    val yearList = restoreMergedItems(year, "%d")
                    Log.d(TAG, "yearList:$yearList")
                    binding!!.flowlayoutMultiSelectYear.setSelectedItems(yearList)
                    binding!!.rbYearTypeAssigned.isChecked = true
                    selectedYearList = yearList.joinToString(",")
                }

                year.contains("-") -> {
                    val yearArray = year.split("-")
                    binding!!.etYearCyclicFrom.setText(yearArray.getOrNull(0) ?: "1970")
                    binding!!.etYearCyclicTo.setText(yearArray.getOrNull(1) ?: "2099")
                    binding!!.rbYearTypeCyclic.isChecked = true
                }

                yearList.indexOf(year) != -1 -> {
                    binding!!.flowlayoutMultiSelectYear.setSelectedItems(year)
                    binding!!.rbYearTypeAssigned.isChecked = true
                    selectedYearList = year
                }

                else -> {
                    year = "*"
                    binding!!.etYear.setText(year)
                    binding!!.rbYearTypeAll.isChecked = true
                }
            }
        } catch (e: Exception) {
            year = "*"
            binding!!.etYear.setText(year)
            binding!!.rbYearTypeAll.isChecked = true
            XToastUtils.error("Cron表达式无效：" + e.message, 30000)
        }
    }

    //获取选中的项目
    private fun getSelectedItems(parent: FlowTagLayout, selectedList: List<Int>, dataType: Int = 0): String {
        if (selectedList.isEmpty()) return ""

        val selectedNumList = mutableListOf<String>()
        for (index in selectedList) {
            selectedNumList.add(parent.adapter.getItem(index).toString())
        }
        val mergedList = when (dataType) {
            2 -> mergeContinuousEnum(selectedNumList)
            1 -> mergeContinuousItems(selectedNumList, "%02d")
            else -> mergeContinuousItems(selectedNumList)
        }
        return mergedList.joinToString(",")
    }

    //合并连续的枚举值
    private fun mergeContinuousEnum(input: List<String>): List<String> {
        if (input.isEmpty()) return emptyList()

        val result = mutableListOf<String>()
        val enumValues = if (input.firstOrNull() in monthList) monthList else weekList

        var start = enumValues.indexOf(input[0])
        var end = enumValues.indexOf(input[0])

        for (i in 1 until input.size) {
            val currentIndex = enumValues.indexOf(input[i])
            if (currentIndex == end + 1) {
                end = currentIndex
            } else {
                if (start == end) {
                    result.add(enumValues[start])
                } else {
                    result.add("${enumValues[start]}-${enumValues[end]}")
                }
                start = currentIndex
                end = currentIndex
            }
        }

        if (start == end) {
            result.add(enumValues[start])
        } else {
            result.add("${enumValues[start]}-${enumValues[end]}")
        }

        return result
    }

    //合并连续的数字
    private fun mergeContinuousItems(input: List<String>, stringFormat: String = "%d"): List<String> {
        if (input.isEmpty()) return emptyList()

        val items = input.map { it.toInt() }.sorted()

        val result = mutableListOf<String>()
        var start = items[0]
        var end = items[0]

        for (i in 1 until items.size) {
            if (items[i] == end + 1) {
                end = items[i]
            } else {
                if (start == end) {
                    result.add(String.format(stringFormat, start))
                } else {
                    result.add(String.format(stringFormat, start) + "-" + String.format(stringFormat, end))
                }
                start = items[i]
                end = items[i]
            }
        }

        if (start == end) {
            result.add(String.format(stringFormat, start))
        } else {
            result.add(String.format(stringFormat, start) + "-" + String.format(stringFormat, end))
        }

        return result
    }

    //还原被合并的连续数字
    private fun restoreMergedItems(mergedString: String, stringFormat: String = "%d"): List<String> {
        if (mergedString.isEmpty()) return emptyList()

        val items = mutableListOf<String>()
        val ranges = mergedString.split(",")

        for (range in ranges) {
            val rangeParts = range.split("-")
            if (rangeParts.size == 1) {
                items.add(rangeParts[0])
            } else if (rangeParts.size == 2) {
                val start = rangeParts[0].toInt()
                val end = rangeParts[1].toInt()
                for (i in start..end) {
                    items.add(String.format(stringFormat, i))
                }
            }
        }

        return items
    }

    //检查设置
    @SuppressLint("SetTextI18n")
    private fun checkSetting(): CronSetting {
        second = binding!!.etSecond.text.toString().trim()
        minute = binding!!.etMinute.text.toString().trim()
        hour = binding!!.etHour.text.toString().trim()
        day = binding!!.etDay.text.toString().trim()
        month = binding!!.etMonth.text.toString().trim()
        week = binding!!.etWeek.text.toString().trim()
        year = binding!!.etYear.text.toString().trim()

        expression = "$second $minute $hour $day $month $week $year"
        description = ""
        Log.d(TAG, "checkSetting, expression:$expression")

        //判断cronExpression是否有效
        CronExpression.validateExpression(expression)

        //TODO：低版本Android解析Cron表达式会报错，暂时不处理
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            //生成cron表达式描述
            val options = Options()
            options.isTwentyFourHourTime = true
            //TODO：支持多语言
            val locale = Locale.getDefault()
            //Chinese, Japanese, Korean and other East Asian languages have no spaces between words
            options.isNeedSpaceBetweenWords = locale == Locale("zh") || locale == Locale("ja") || locale == Locale("ko")
            description = CronExpressionDescriptor.getDescription(expression, options, locale)
        } else {
            description = expression
        }

        return CronSetting(description, expression)
    }
}