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
import com.idormy.sms.forwarder.adapter.spinner.RuleAdapterItem
import com.idormy.sms.forwarder.adapter.spinner.RuleSpinnerAdapter
import com.idormy.sms.forwarder.core.BaseFragment
import com.idormy.sms.forwarder.database.AppDatabase
import com.idormy.sms.forwarder.database.entity.Rule
import com.idormy.sms.forwarder.databinding.FragmentTasksActionRuleBinding
import com.idormy.sms.forwarder.entity.action.RuleSetting
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

@Page(name = "Rule")
@Suppress("PrivatePropertyName")
class RuleFragment : BaseFragment<FragmentTasksActionRuleBinding?>(), View.OnClickListener {

    private val TAG: String = RuleFragment::class.java.simpleName
    private var titleBar: TitleBar? = null
    private var mCountDownHelper: CountDownButtonHelper? = null

    //当前转发规则
    private var ruleId = 0L
    private var ruleListSelected: MutableList<Rule> = mutableListOf()
    private var ruleItemMap = HashMap<Long, LinearLayout>(2)

    //发送通道列表
    private var ruleListAll: MutableList<Rule> = mutableListOf()
    private val ruleSpinnerList = ArrayList<RuleAdapterItem>()
    private lateinit var ruleSpinnerAdapter: RuleSpinnerAdapter<*>

    @JvmField
    @AutoWired(name = KEY_EVENT_DATA_ACTION)
    var eventData: String? = null

    override fun initArgs() {
        XRouter.getInstance().inject(this)
    }

    override fun viewBindingInflate(
        inflater: LayoutInflater,
        container: ViewGroup,
    ): FragmentTasksActionRuleBinding {
        return FragmentTasksActionRuleBinding.inflate(inflater, container, false)
    }

    override fun initTitle(): TitleBar? {
        titleBar = super.initTitle()!!.setImmersive(false).setTitle(R.string.task_rule)
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
            val settingVo = Gson().fromJson(eventData, RuleSetting::class.java)
            binding!!.rgStatus.check(if (settingVo.status == "enable") R.id.rb_status_enable else R.id.rb_status_disable)
            Log.d(TAG, settingVo.ruleList.toString())
            settingVo.ruleList.forEach {
                ruleId = it.id
                ruleListSelected.add(it)
            }
            Log.d(TAG, "initViews settingVo:$settingVo")
        }

        //初始化发送通道下拉框
        initRuleSpinner()
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
    private fun initRuleSpinner() {
        AppDatabase.getInstance(requireContext()).ruleDao().getAll().subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(object : SingleObserver<List<Rule>> {
            override fun onSubscribe(d: Disposable) {}

            override fun onError(e: Throwable) {
                e.printStackTrace()
                Log.e(TAG, "initRuleSpinner error: ${e.message}")
            }

            override fun onSuccess(ruleList: List<Rule>) {
                if (ruleList.isEmpty()) {
                    XToastUtils.error(R.string.add_rule_first)
                    return
                }

                ruleListAll = ruleList as MutableList<Rule>
                for (rule in ruleList) {
                    val name = if (rule.name.length > 20) rule.name.substring(0, 19) else rule.name
                    ruleSpinnerList.add(RuleAdapterItem(name, getDrawable(rule.imageId), rule.id, rule.status))
                }
                ruleSpinnerAdapter = RuleSpinnerAdapter(ruleSpinnerList)
                    .setIsFilterKey(true).setFilterColor("#EF5362").setBackgroundSelector(R.drawable.selector_custom_spinner_bg)
                binding!!.spRule.setAdapter(ruleSpinnerAdapter)

                if (ruleListSelected.isNotEmpty()) {
                    for (rule in ruleListSelected) {
                        for (ruleItem in ruleSpinnerList) {
                            if (rule.id == ruleItem.id) {
                                addRuleItemLinearLayout(ruleItemMap, binding!!.layoutRules, ruleItem)
                            }
                        }
                    }
                }
            }
        })
        binding!!.spRule.setOnItemClickListener { _: AdapterView<*>, _: View, position: Int, _: Long ->
            try {
                val rule = ruleSpinnerAdapter.getItemSource(position) as RuleAdapterItem
                ruleId = rule.id!!
                if (ruleId > 0L) {
                    ruleListSelected.forEach {
                        if (ruleId == it.id) {
                            XToastUtils.warning(getString(R.string.rule_contains_tips))
                            return@setOnItemClickListener
                        }
                    }
                    ruleListAll.forEach {
                        if (ruleId == it.id) {
                            ruleListSelected.add(it)
                            addRuleItemLinearLayout(ruleItemMap, binding!!.layoutRules, rule)
                        }
                    }

                    if (STATUS_OFF == rule.status) {
                        XToastUtils.warning(getString(R.string.rule_disabled_tips))
                    }
                }
            } catch (e: Exception) {
                XToastUtils.error(e.message.toString())
            }
        }
    }

    /**
     * 动态增删Rule
     *
     * @param ruleItemMap          管理item的map，用于删除指定header
     * @param layoutRules          需要挂载item的LinearLayout
     * @param rule                 RuleAdapterItem
     */
    @SuppressLint("SetTextI18n")
    private fun addRuleItemLinearLayout(
        ruleItemMap: MutableMap<Long, LinearLayout>, layoutRules: LinearLayout, rule: RuleAdapterItem
    ) {
        val layoutRuleItem = View.inflate(requireContext(), R.layout.item_add_rule, null) as LinearLayout
        val ivRemoveRule = layoutRuleItem.findViewById<ImageView>(R.id.iv_remove_rule)
        val ivRuleImage = layoutRuleItem.findViewById<ImageView>(R.id.iv_rule_image)
        val ivRuleStatus = layoutRuleItem.findViewById<ImageView>(R.id.iv_rule_status)
        val tvRuleName = layoutRuleItem.findViewById<TextView>(R.id.tv_rule_name)

        ivRuleImage.setImageDrawable(rule.icon)
        ivRuleStatus.setImageDrawable(getDrawable(if (STATUS_OFF == rule.status) R.drawable.ic_stop else R.drawable.ic_start))
        val ruleItemId = rule.id as Long
        tvRuleName.text = "ID-$ruleItemId：${rule.title}"

        ivRemoveRule.tag = ruleItemId
        ivRemoveRule.setOnClickListener { view2: View ->
            val tagId = view2.tag as Long
            layoutRules.removeView(ruleItemMap[tagId])
            ruleItemMap.remove(tagId)
            //ruleListSelected.removeIf { it.id == tagId }
            for (it in ruleListSelected) {
                if (it.id == tagId) {
                    ruleListSelected -= it
                    break
                }
            }
            Log.d(TAG, ruleListSelected.count().toString())
            Log.d(TAG, ruleListSelected.toString())
            if (ruleListSelected.isEmpty()) ruleId = 0L
        }
        layoutRules.addView(layoutRuleItem)
        ruleItemMap[ruleItemId] = layoutRuleItem
    }

    //检查设置
    @SuppressLint("SetTextI18n")
    private fun checkSetting(): RuleSetting {
        val description = StringBuilder()
        val status: String
        if (binding!!.rgStatus.checkedRadioButtonId == R.id.rb_status_enable) {
            status = "enable"
            description.append(getString(R.string.enable))
        } else {
            status = "disable"
            description.append(getString(R.string.disable))
        }
        description.append(getString(R.string.menu_rules))

        if (ruleListSelected.isNotEmpty()) {
            description.append(", ").append(getString(R.string.specified_rule)).append(": ")
            description.append(ruleListSelected.joinToString("/") { it.id.toString() })
        }

        return RuleSetting(description.toString(), status, ruleListSelected)
    }
}