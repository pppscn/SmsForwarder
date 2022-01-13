package com.idormy.sms.forwarder;

import static com.idormy.sms.forwarder.SenderActivity.NOTIFY;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.idormy.sms.forwarder.adapter.RuleAdapter;
import com.idormy.sms.forwarder.model.RuleModel;
import com.idormy.sms.forwarder.model.SenderModel;
import com.idormy.sms.forwarder.model.vo.SmsVo;
import com.idormy.sms.forwarder.sender.SendUtil;
import com.idormy.sms.forwarder.sender.SenderUtil;
import com.idormy.sms.forwarder.utils.RuleUtil;
import com.idormy.sms.forwarder.utils.SettingUtil;
import com.umeng.analytics.MobclickAgent;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@SuppressWarnings("deprecation")
public class RuleActivity extends AppCompatActivity {

    private final String TAG = "RuleActivity";
    // 用于存储数据
    private List<RuleModel> ruleModels = new ArrayList<>();
    private RuleAdapter adapter;
    private String currentType = "sms";
    private ListView listView;

    //消息处理者,创建一个Handler的子类对象,目的是重写Handler的处理消息的方法(handleMessage())
    @SuppressLint("HandlerLeak")
    private final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == NOTIFY) {
                Toast.makeText(RuleActivity.this, msg.getData().getString("DATA"), Toast.LENGTH_LONG).show();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rule);
        RuleUtil.init(RuleActivity.this);
        SenderUtil.init(RuleActivity.this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");

        //是否关闭页面提示
        TextView help_tip = findViewById(R.id.help_tip);
        help_tip.setVisibility(MyApplication.showHelpTip ? View.VISIBLE : View.GONE);

        // 先拿到数据并放在适配器上
        initRules(); //初始化数据
        adapter = new RuleAdapter(RuleActivity.this, R.layout.item_rule, ruleModels);

        // 将适配器上的数据传递给listView
        listView = findViewById(R.id.list_view_rule);
        listView.setAdapter(adapter);

        // 为ListView注册一个监听器，当用户点击了ListView中的任何一个子项时，就会回调onItemClick()方法
        // 在这个方法中可以通过position参数判断出用户点击的是那一个子项
        listView.setOnItemClickListener((parent, view, position, id) -> {
            RuleModel ruleModel = ruleModels.get(position);
            Log.d(TAG, "onItemClick: " + ruleModel);
            setRule(ruleModel, false);
        });

        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            //定义AlertDialog.Builder对象，当长按列表项的时候弹出确认删除对话框
            AlertDialog.Builder builder = new AlertDialog.Builder(RuleActivity.this);
            builder.setTitle(R.string.delete_rule_title);
            builder.setMessage(R.string.delete_rule_tips);

            //添加AlertDialog.Builder对象的setPositiveButton()方法
            builder.setPositiveButton(R.string.confirm, (dialog, which) -> {
                RuleUtil.delRule(ruleModels.get(position).getId());
                initRules();
                adapter.del(ruleModels);
                Toast.makeText(getBaseContext(), R.string.delete_rule_toast, Toast.LENGTH_SHORT).show();
            });

            //添加AlertDialog.Builder对象的setNegativeButton()方法
            builder.setNeutralButton(R.string.clone, (dialog, which) -> {
                RuleModel ruleModel = ruleModels.get(position);
                //TODO:直接复制
                //RuleUtil.addRule(ruleModel);
                //initRules();
                //adapter.add(ruleModels);
                //TODO:只复制到编辑对话框
                setRule(ruleModel, true);
            });

            //添加AlertDialog.Builder对象的setNegativeButton()方法
            builder.setNegativeButton(R.string.cancel, (dialog, which) -> {

            });

            builder.create().show();
            return true;
        });

        //切换日志类别
        int typeCheckId = getTypeCheckId(currentType);
        final RadioGroup radioGroupTypeCheck = findViewById(R.id.radioGroupTypeCheck);
        radioGroupTypeCheck.check(typeCheckId);
        radioGroupTypeCheck.setOnCheckedChangeListener((group, checkedId) -> {
            RadioButton rb = findViewById(checkedId);
            currentType = (String) rb.getTag();
            initRules(); //初始化数据
            adapter = new RuleAdapter(RuleActivity.this, R.layout.item_rule, ruleModels);
            listView.setAdapter(adapter);
        });
    }

    private int getTypeCheckId(String currentType) {
        switch (currentType) {
            case "call":
                return R.id.btnTypeCall;
            case "app":
                return R.id.btnTypeApp;
            default:
                return R.id.btnTypeSms;
        }
    }

    private int getDialogView(String currentType) {
        switch (currentType) {
            case "call":
                return R.layout.alert_dialog_setview_rule_call;
            case "app":
                return R.layout.alert_dialog_setview_rule_app;
            default:
                return R.layout.alert_dialog_setview_rule;
        }
    }

    private int getDialogTitle(String currentType) {
        switch (currentType) {
            case "call":
                return R.string.setrule_call;
            case "app":
                return R.string.setrule_app;
            default:
                return R.string.setrule;
        }
    }

    // 初始化数据
    private void initRules() {
        ruleModels = RuleUtil.getRule(null, null, currentType);
    }

    public void addRule(View view) {
        currentType = (String) view.getTag();
        int typeCheckId = getTypeCheckId(currentType);
        final RadioGroup radioGroupTypeCheck = findViewById(R.id.radioGroupTypeCheck);
        radioGroupTypeCheck.check(typeCheckId);

        setRule(null, false);
    }

    private void setRule(final RuleModel ruleModel, final boolean isClone) {
        final AlertDialog.Builder alertDialog71 = new AlertDialog.Builder(RuleActivity.this);
        final View view1 = View.inflate(RuleActivity.this, getDialogView(currentType), null);

        final RadioGroup radioGroupRuleFiled = view1.findViewById(R.id.radioGroupRuleFiled);
        if (ruleModel != null) radioGroupRuleFiled.check(ruleModel.getRuleFiledCheckId());

        final RadioGroup radioGroupRuleCheck = view1.findViewById(R.id.radioGroupRuleCheck);
        final RadioGroup radioGroupRuleCheck2 = view1.findViewById(R.id.radioGroupRuleCheck2);
        if (ruleModel != null) {
            int ruleCheckCheckId = ruleModel.getRuleCheckCheckId();
            if (ruleCheckCheckId == R.id.btnIs || ruleCheckCheckId == R.id.btnNotContain || ruleCheckCheckId == R.id.btnContain) {
                radioGroupRuleCheck.check(ruleCheckCheckId);
            } else {
                radioGroupRuleCheck2.check(ruleCheckCheckId);
            }
        } else {
            radioGroupRuleCheck.check(R.id.btnIs);
        }

        final RadioGroup radioGroupSimSlot = view1.findViewById(R.id.radioGroupSimSlot);
        if (ruleModel != null) radioGroupSimSlot.check(ruleModel.getRuleSimSlotCheckId());

        final TextView tv_mu_rule_tips = view1.findViewById(R.id.tv_mu_rule_tips);
        final TextView ruleSenderTv = view1.findViewById(R.id.ruleSenderTv);
        if (ruleModel != null && ruleModel.getSenderId() != null) {
            List<SenderModel> getSenders = SenderUtil.getSender(ruleModel.getSenderId(), null);
            if (!getSenders.isEmpty()) {
                ruleSenderTv.setText(getSenders.get(0).getName());
                ruleSenderTv.setTag(getSenders.get(0).getId());
            }
        }
        final Button btSetRuleSender = view1.findViewById(R.id.btSetRuleSender);
        btSetRuleSender.setOnClickListener(view -> {
            //Toast.makeText(RuleActivity.this, "selectSender", Toast.LENGTH_LONG).show();
            selectSender(ruleSenderTv);
        });

        final EditText editTextRuleValue = view1.findViewById(R.id.editTextRuleValue);
        if (ruleModel != null)
            editTextRuleValue.setText(ruleModel.getValue());

        //当更新选择的字段的时候，更新之下各个选项的状态
        final LinearLayout matchTypeLayout = view1.findViewById(R.id.matchTypeLayout);
        final LinearLayout matchValueLayout = view1.findViewById(R.id.matchValueLayout);
        refreshSelectRadioGroupRuleFiled(radioGroupRuleFiled, radioGroupRuleCheck, radioGroupRuleCheck2, editTextRuleValue, tv_mu_rule_tips, matchTypeLayout, matchValueLayout);

        //自定义模板
        @SuppressLint("UseSwitchCompatOrMaterialCode") Switch switchSmsTemplate = view1.findViewById(R.id.switch_sms_template);
        EditText textSmsTemplate = view1.findViewById(R.id.text_sms_template);
        if (ruleModel != null) {
            switchSmsTemplate.setChecked(ruleModel.getSwitchSmsTemplate());
            textSmsTemplate.setText(ruleModel.getSmsTemplate());
        }

        //正则替换
        @SuppressLint("UseSwitchCompatOrMaterialCode") Switch switchRegexReplace = view1.findViewById(R.id.switch_regex_replace);
        EditText textRegexReplace = view1.findViewById(R.id.text_regex_replace);
        if (ruleModel != null) {
            switchRegexReplace.setChecked(ruleModel.getSwitchRegexReplace());
            textRegexReplace.setText(ruleModel.getRegexReplace());
        }

        Button buttonRuleOk = view1.findViewById(R.id.buttonRuleOk);
        Button buttonRuleDel = view1.findViewById(R.id.buttonRuleDel);
        Button buttonRuleTest = view1.findViewById(R.id.buttonRuleTest);
        alertDialog71
                .setTitle(getDialogTitle(currentType))
                .setView(view1)
                .create();
        final AlertDialog show = alertDialog71.show();
        buttonRuleOk.setOnClickListener(view -> {
            Object senderId = ruleSenderTv.getTag();
            if (senderId == null) {
                Toast.makeText(RuleActivity.this, R.string.new_sender_first, Toast.LENGTH_LONG).show();
                return;
            }

            //检查正则替换填写是否正确
            String regexReplace = textRegexReplace.getText().toString().trim();
            int lineNum = checkRegexReplace(regexReplace);
            if (lineNum > 0) {
                Toast.makeText(getBaseContext(), String.format(RuleActivity.this.getString(R.string.regex_check_tips), lineNum), Toast.LENGTH_SHORT).show();
                return;
            }

            int radioGroupRuleCheckId = Math.max(radioGroupRuleCheck.getCheckedRadioButtonId(), radioGroupRuleCheck2.getCheckedRadioButtonId());
            Log.d(TAG, radioGroupRuleCheck.getCheckedRadioButtonId() + "  " + radioGroupRuleCheck2.getCheckedRadioButtonId() + " " + radioGroupRuleCheckId);
            if (isClone || ruleModel == null) {
                RuleModel newRuleModel = new RuleModel();
                newRuleModel.setType(currentType);
                newRuleModel.setFiled(RuleModel.getRuleFiledFromCheckId(radioGroupRuleFiled.getCheckedRadioButtonId()));
                newRuleModel.setCheck(RuleModel.getRuleCheckFromCheckId(radioGroupRuleCheckId));
                newRuleModel.setSimSlot(RuleModel.getRuleSimSlotFromCheckId(radioGroupSimSlot.getCheckedRadioButtonId()));
                newRuleModel.setValue(editTextRuleValue.getText().toString().trim());
                newRuleModel.setSwitchSmsTemplate(switchSmsTemplate.isChecked());
                newRuleModel.setSmsTemplate(textSmsTemplate.getText().toString().trim());
                newRuleModel.setSwitchRegexReplace(switchRegexReplace.isChecked());
                newRuleModel.setRegexReplace(regexReplace);
                newRuleModel.setSenderId(Long.valueOf(senderId.toString()));
                RuleUtil.addRule(newRuleModel);
                initRules();
                adapter.add(ruleModels);
            } else {
                ruleModel.setFiled(RuleModel.getRuleFiledFromCheckId(radioGroupRuleFiled.getCheckedRadioButtonId()));
                ruleModel.setCheck(RuleModel.getRuleCheckFromCheckId(radioGroupRuleCheckId));
                ruleModel.setSimSlot(RuleModel.getRuleSimSlotFromCheckId(radioGroupSimSlot.getCheckedRadioButtonId()));
                ruleModel.setValue(editTextRuleValue.getText().toString().trim());
                ruleModel.setSwitchSmsTemplate(switchSmsTemplate.isChecked());
                ruleModel.setSmsTemplate(textSmsTemplate.getText().toString().trim());
                ruleModel.setSwitchRegexReplace(switchRegexReplace.isChecked());
                ruleModel.setRegexReplace(regexReplace);
                ruleModel.setSenderId(Long.valueOf(senderId.toString()));
                RuleUtil.updateRule(ruleModel);
                initRules();
                adapter.update(ruleModels);
            }

            show.dismiss();
        });

        buttonRuleDel.setOnClickListener(view -> {
            if (ruleModel != null) {
                RuleUtil.delRule(ruleModel.getId());
                initRules();
                adapter.del(ruleModels);
            }
            show.dismiss();
        });

        buttonRuleTest.setOnClickListener(view -> {
            Object senderId = ruleSenderTv.getTag();
            if (senderId == null) {
                Toast.makeText(RuleActivity.this, R.string.new_sender_first, Toast.LENGTH_LONG).show();
                return;
            }

            //检查正则替换填写是否正确
            String regexReplace = textRegexReplace.getText().toString().trim();
            int lineNum = checkRegexReplace(regexReplace);
            if (lineNum > 0) {
                Toast.makeText(getBaseContext(), String.format(RuleActivity.this.getString(R.string.regex_check_tips), lineNum), Toast.LENGTH_SHORT).show();
                return;
            }

            int radioGroupRuleCheckId = Math.max(radioGroupRuleCheck.getCheckedRadioButtonId(), radioGroupRuleCheck2.getCheckedRadioButtonId());
            if (ruleModel == null) {
                RuleModel newRuleModel = new RuleModel();
                newRuleModel.setFiled(RuleModel.getRuleFiledFromCheckId(radioGroupRuleFiled.getCheckedRadioButtonId()));
                newRuleModel.setCheck(RuleModel.getRuleCheckFromCheckId(radioGroupRuleCheckId));
                newRuleModel.setSimSlot(RuleModel.getRuleSimSlotFromCheckId(radioGroupSimSlot.getCheckedRadioButtonId()));
                newRuleModel.setValue(editTextRuleValue.getText().toString().trim());
                newRuleModel.setSenderId(Long.valueOf(senderId.toString()));
                newRuleModel.setSwitchSmsTemplate(switchSmsTemplate.isChecked());
                newRuleModel.setSmsTemplate(textSmsTemplate.getText().toString().trim());
                newRuleModel.setSwitchRegexReplace(switchRegexReplace.isChecked());
                newRuleModel.setRegexReplace(regexReplace);

                testRule(newRuleModel, Long.valueOf(senderId.toString()));
            } else {
                ruleModel.setFiled(RuleModel.getRuleFiledFromCheckId(radioGroupRuleFiled.getCheckedRadioButtonId()));
                ruleModel.setCheck(RuleModel.getRuleCheckFromCheckId(radioGroupRuleCheckId));
                ruleModel.setSimSlot(RuleModel.getRuleSimSlotFromCheckId(radioGroupSimSlot.getCheckedRadioButtonId()));
                ruleModel.setValue(editTextRuleValue.getText().toString().trim());
                ruleModel.setSenderId(Long.valueOf(senderId.toString()));
                ruleModel.setSwitchSmsTemplate(switchSmsTemplate.isChecked());
                ruleModel.setSmsTemplate(textSmsTemplate.getText().toString().trim());
                ruleModel.setSwitchRegexReplace(switchRegexReplace.isChecked());
                ruleModel.setRegexReplace(regexReplace);

                testRule(ruleModel, Long.valueOf(senderId.toString()));
            }
        });

        //自定义模板
        final LinearLayout layout_sms_template = view1.findViewById(R.id.layout_sms_template);
        if (ruleModel != null) {
            layout_sms_template.setVisibility(ruleModel.getSwitchSmsTemplate() ? View.VISIBLE : View.GONE);
        }
        switchSmsTemplate.setOnCheckedChangeListener((buttonView, isChecked) -> {
            layout_sms_template.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            if (!isChecked) {
                textSmsTemplate.setText("");
            }
        });

        Button buttonInsertSender = view1.findViewById(R.id.bt_insert_sender);
        buttonInsertSender.setOnClickListener(view -> {
            textSmsTemplate.setFocusable(true);
            textSmsTemplate.requestFocus();
            insertOrReplaceText2Cursor(textSmsTemplate, "{{来源号码}}");
        });

        Button buttonInsertContent = view1.findViewById(R.id.bt_insert_content);
        buttonInsertContent.setOnClickListener(view -> {
            textSmsTemplate.setFocusable(true);
            textSmsTemplate.requestFocus();
            insertOrReplaceText2Cursor(textSmsTemplate, "{{短信内容}}");
        });

        Button buttonInsertSenderApp = view1.findViewById(R.id.bt_insert_sender_app);
        buttonInsertSenderApp.setOnClickListener(view -> {
            textSmsTemplate.setFocusable(true);
            textSmsTemplate.requestFocus();
            insertOrReplaceText2Cursor(textSmsTemplate, "{{APP包名}}");
        });

        Button buttonInsertContentApp = view1.findViewById(R.id.bt_insert_content_app);
        buttonInsertContentApp.setOnClickListener(view -> {
            textSmsTemplate.setFocusable(true);
            textSmsTemplate.requestFocus();
            insertOrReplaceText2Cursor(textSmsTemplate, "{{通知内容}}");
        });

        Button buttonInsertExtra = view1.findViewById(R.id.bt_insert_extra);
        buttonInsertExtra.setOnClickListener(view -> {
            textSmsTemplate.setFocusable(true);
            textSmsTemplate.requestFocus();
            insertOrReplaceText2Cursor(textSmsTemplate, "{{卡槽信息}}");
        });

        Button buttonInsertTime = view1.findViewById(R.id.bt_insert_time);
        buttonInsertTime.setOnClickListener(view -> {
            textSmsTemplate.setFocusable(true);
            textSmsTemplate.requestFocus();
            insertOrReplaceText2Cursor(textSmsTemplate, "{{接收时间}}");
        });

        Button buttonInsertDeviceName = view1.findViewById(R.id.bt_insert_device_name);
        buttonInsertDeviceName.setOnClickListener(view -> {
            textSmsTemplate.setFocusable(true);
            textSmsTemplate.requestFocus();
            insertOrReplaceText2Cursor(textSmsTemplate, "{{设备名称}}");
        });

        //正则替换
        final LinearLayout layout_regex_replace = view1.findViewById(R.id.layout_regex_replace);
        if (ruleModel != null) {
            layout_regex_replace.setVisibility(ruleModel.getSwitchRegexReplace() ? View.VISIBLE : View.GONE);
        }
        switchRegexReplace.setOnCheckedChangeListener((buttonView, isChecked) -> {
            layout_regex_replace.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            if (!isChecked) {
                textRegexReplace.setText("");
            }
        });

    }

    private void insertOrReplaceText2Cursor(EditText editText, String str) {
        int start = Math.max(editText.getSelectionStart(), 0);
        int end = Math.max(editText.getSelectionEnd(), 0);
        editText.getText().replace(Math.min(start, end), Math.max(start, end), str, 0, str.length());
    }

    //当更新选择的字段的时候，更新之下各个选项的状态
    // 如果设置了转发全部，禁用选择模式和匹配值输入
    // 如果设置了多重规则，选择模式置为是
    private void refreshSelectRadioGroupRuleFiled(RadioGroup radioGroupRuleFiled, final RadioGroup radioGroupRuleCheck, final RadioGroup radioGroupRuleCheck2, final EditText editTextRuleValue, final TextView tv_mu_rule_tips, final LinearLayout matchTypeLayout, final LinearLayout matchValueLayout) {
        refreshSelectRadioGroupRuleFiledAction(radioGroupRuleFiled.getCheckedRadioButtonId(), radioGroupRuleCheck, radioGroupRuleCheck2, editTextRuleValue, tv_mu_rule_tips, matchTypeLayout, matchValueLayout);

        radioGroupRuleCheck.setOnCheckedChangeListener((group, checkedId) -> {
            Log.d(TAG, String.valueOf(group));
            Log.d(TAG, String.valueOf(checkedId));
            if (group != null && checkedId > 0) {
                if (group == radioGroupRuleCheck) {
                    radioGroupRuleCheck2.clearCheck();
                } else if (group == radioGroupRuleCheck2) {
                    radioGroupRuleCheck.clearCheck();
                }
                group.check(checkedId);
            }
        });
        radioGroupRuleCheck2.setOnCheckedChangeListener((group, checkedId) -> {
            Log.d(TAG, String.valueOf(group));
            Log.d(TAG, String.valueOf(checkedId));
            if (group != null && checkedId > 0) {
                if (group == radioGroupRuleCheck) {
                    radioGroupRuleCheck2.clearCheck();
                } else if (group == radioGroupRuleCheck2) {
                    radioGroupRuleCheck.clearCheck();
                }
                group.check(checkedId);
            }
        });
        radioGroupRuleFiled.setOnCheckedChangeListener((group, checkedId) -> {
            Log.d(TAG, String.valueOf(group));
            Log.d(TAG, String.valueOf(checkedId));
            if (group == radioGroupRuleCheck) {
                radioGroupRuleCheck2.clearCheck();
            } else if (group == radioGroupRuleCheck2) {
                radioGroupRuleCheck.clearCheck();
            }
            refreshSelectRadioGroupRuleFiledAction(checkedId, radioGroupRuleCheck, radioGroupRuleCheck2, editTextRuleValue, tv_mu_rule_tips, matchTypeLayout, matchValueLayout);
        });
    }

    @SuppressLint("NonConstantResourceId")
    private void refreshSelectRadioGroupRuleFiledAction(int checkedRuleFiledId, final RadioGroup radioGroupRuleCheck, final RadioGroup radioGroupRuleCheck2, final EditText editTextRuleValue, final TextView tv_mu_rule_tips, final LinearLayout matchTypeLayout, final LinearLayout matchValueLayout) {
        tv_mu_rule_tips.setVisibility(View.GONE);
        matchTypeLayout.setVisibility(View.VISIBLE);
        matchValueLayout.setVisibility(View.VISIBLE);

        switch (checkedRuleFiledId) {
            case R.id.btnTranspondAll:
                for (int i = 0; i < radioGroupRuleCheck.getChildCount(); i++) {
                    radioGroupRuleCheck.getChildAt(i).setEnabled(false);
                }
                for (int i = 0; i < radioGroupRuleCheck2.getChildCount(); i++) {
                    radioGroupRuleCheck2.getChildAt(i).setEnabled(false);
                }
                editTextRuleValue.setEnabled(false);
                matchTypeLayout.setVisibility(View.GONE);
                matchValueLayout.setVisibility(View.GONE);
                break;
            case R.id.btnMultiMatch:
                for (int i = 0; i < radioGroupRuleCheck.getChildCount(); i++) {
                    radioGroupRuleCheck.getChildAt(i).setEnabled(false);
                }
                for (int i = 0; i < radioGroupRuleCheck2.getChildCount(); i++) {
                    radioGroupRuleCheck2.getChildAt(i).setEnabled(false);
                }
                editTextRuleValue.setEnabled(true);
                matchTypeLayout.setVisibility(View.GONE);
                tv_mu_rule_tips.setVisibility(MyApplication.showHelpTip ? View.VISIBLE : View.GONE);
                break;
            default:
                for (int i = 0; i < radioGroupRuleCheck.getChildCount(); i++) {
                    radioGroupRuleCheck.getChildAt(i).setEnabled(true);
                }
                for (int i = 0; i < radioGroupRuleCheck2.getChildCount(); i++) {
                    radioGroupRuleCheck2.getChildAt(i).setEnabled(true);
                }
                editTextRuleValue.setEnabled(true);
                break;
        }
    }

    public void selectSender(final TextView showTv) {
        final List<SenderModel> senderModels = SenderUtil.getSender(null, null);
        if (senderModels.isEmpty()) {
            Toast.makeText(RuleActivity.this, R.string.add_sender_first, Toast.LENGTH_SHORT).show();
            return;
        }
        final CharSequence[] senderNames = new CharSequence[senderModels.size()];
        for (int i = 0; i < senderModels.size(); i++) {
            senderNames[i] = senderModels.get(i).getName();
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(RuleActivity.this);
        builder.setTitle(R.string.select_sender);
        //添加列表
        builder.setItems(senderNames, (dialogInterface, which) -> {
            Toast.makeText(RuleActivity.this, senderNames[which], Toast.LENGTH_LONG).show();
            showTv.setText(senderNames[which]);
            showTv.setTag(senderModels.get(which).getId());
        });
        builder.show();
    }

    public void testRule(final RuleModel ruleModel, final Long senderId) {
        final View view = View.inflate(RuleActivity.this, R.layout.alert_dialog_setview_rule_test, null);
        final TextView textTestSimSlot = view.findViewById(R.id.textTestSimSlot);
        final TextView textTestPhone = view.findViewById(R.id.textTestPhone);
        final TextView textTestContent = view.findViewById(R.id.textTestContent);
        final RadioGroup radioGroupTestSimSlot = view.findViewById(R.id.radioGroupTestSimSlot);
        final EditText editTextTestPhone = view.findViewById(R.id.editTextTestPhone);
        final EditText editTextTestMsgContent = view.findViewById(R.id.editTextTestMsgContent);

        if ("app".equals(currentType)) {
            textTestSimSlot.setVisibility(View.GONE);
            radioGroupTestSimSlot.setVisibility(View.GONE);
            textTestPhone.setText(R.string.test_package_name);
            textTestContent.setText(R.string.test_inform_content);
        } else if ("call".equals(currentType)) {
            textTestContent.setVisibility(View.GONE);
            editTextTestMsgContent.setVisibility(View.GONE);
        }

        Button buttonRuleTest = view.findViewById(R.id.buttonRuleTest);
        AlertDialog.Builder ad1 = new AlertDialog.Builder(RuleActivity.this);
        ad1.setTitle(R.string.rule_tester);
        ad1.setIcon(android.R.drawable.ic_dialog_email);
        ad1.setView(view);
        buttonRuleTest.setOnClickListener(v -> {

            Log.i("editTextTestPhone", editTextTestPhone.getText().toString().trim());
            Log.i("editTextTestMsgContent", editTextTestMsgContent.getText().toString().trim());

            try {
                String simSlot = RuleModel.getRuleSimSlotFromCheckId(radioGroupTestSimSlot.getCheckedRadioButtonId());
                String simInfo;
                if (simSlot.equals("SIM2")) {
                    simInfo = simSlot + "_" + SettingUtil.getAddExtraSim2();
                } else {
                    simInfo = simSlot + "_" + SettingUtil.getAddExtraSim1();
                }
                SmsVo testSmsVo = new SmsVo(editTextTestPhone.getText().toString().trim(), editTextTestMsgContent.getText().toString().trim(), new Date(), simInfo);
                SendUtil.sendMsgByRuleModelSenderId(handler, ruleModel, testSmsVo, senderId);
            } catch (Exception e) {
                Toast.makeText(RuleActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
        ad1.show();// 显示对话框
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        MobclickAgent.onPageStart(TAG);
        MobclickAgent.onResume(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        MobclickAgent.onPageEnd(TAG);
        MobclickAgent.onPause(this);
    }

    private int checkRegexReplace(String regexReplace) {
        if (regexReplace == null || regexReplace.isEmpty()) return 0;

        int lineNum = 1;
        String[] lineArray = regexReplace.split("\\n");
        for (String line : lineArray) {
            int position = line.indexOf("===");
            if (position < 1) return lineNum;
            lineNum++;
        }

        return 0;
    }
}
