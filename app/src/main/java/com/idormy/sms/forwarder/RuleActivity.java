package com.idormy.sms.forwarder;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.idormy.sms.forwarder.adapter.RuleAdapter;
import com.idormy.sms.forwarder.model.RuleModel;
import com.idormy.sms.forwarder.model.SenderModel;
import com.idormy.sms.forwarder.utils.RuleUtil;
import com.idormy.sms.forwarder.utils.SenderUtil;
import com.umeng.analytics.MobclickAgent;

import java.util.ArrayList;
import java.util.List;

public class RuleActivity extends AppCompatActivity {

    private String TAG = "RuleActivity";
    // 用于存储数据
    private List<RuleModel> ruleModels = new ArrayList<>();
    private RuleAdapter adapter;
    private Long selectSenderId = 0l;
    private String selectSenderName = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "oncreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sender);
        RuleUtil.init(RuleActivity.this);
        SenderUtil.init(RuleActivity.this);

        // 先拿到数据并放在适配器上
        initRules(); //初始化数据
        adapter = new RuleAdapter(RuleActivity.this, R.layout.rule_item, ruleModels);

        // 将适配器上的数据传递给listView
        ListView listView = findViewById(R.id.list_view_sender);
        listView.setAdapter(adapter);

        // 为ListView注册一个监听器，当用户点击了ListView中的任何一个子项时，就会回调onItemClick()方法
        // 在这个方法中可以通过position参数判断出用户点击的是那一个子项
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                RuleModel ruleModel = ruleModels.get(position);
                Log.d(TAG, "onItemClick: " + ruleModel);
                setRule(ruleModel);
            }
        });


    }

    // 初始化数据
    private void initRules() {
        ruleModels = RuleUtil.getRule(null, null);
    }

    public void addSender(View view) {
        setRule(null);
    }


    private void setRule(final RuleModel ruleModel) {
        final AlertDialog.Builder alertDialog71 = new AlertDialog.Builder(RuleActivity.this);
        final View view1 = View.inflate(RuleActivity.this, R.layout.activity_alter_dialog_setview_rule, null);

        final RadioGroup radioGroupRuleFiled = (RadioGroup) view1.findViewById(R.id.radioGroupRuleFiled);
        if (ruleModel != null) radioGroupRuleFiled.check(ruleModel.getRuleFiledCheckId());

        final RadioGroup radioGroupRuleCheck = (RadioGroup) view1.findViewById(R.id.radioGroupRuleCheck);
        if (ruleModel != null) radioGroupRuleCheck.check(ruleModel.getRuleCheckCheckId());

        final TextView ruleSenderTv = (TextView) view1.findViewById(R.id.ruleSenderTv);
        if (ruleModel != null && ruleModel.getSenderId() != null) {
            List<SenderModel> getSeners = SenderUtil.getSender(ruleModel.getSenderId(), null);
            if (!getSeners.isEmpty()) {
                ruleSenderTv.setText(getSeners.get(0).getName());
                ruleSenderTv.setTag(getSeners.get(0).getId());
            }
        }
        final Button btSetRuleSender = (Button) view1.findViewById(R.id.btSetRuleSender);
        btSetRuleSender.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(RuleActivity.this, "selectSender", Toast.LENGTH_LONG).show();
                selectSender(ruleSenderTv);
            }
        });

        final EditText editTextRuleValue = view1.findViewById(R.id.editTextRuleValue);
        if (ruleModel != null)
            editTextRuleValue.setText(ruleModel.getValue());

        Button buttonruleok = view1.findViewById(R.id.buttonruleok);
        Button buttonruledel = view1.findViewById(R.id.buttonruledel);
        alertDialog71
                .setTitle(R.string.setrule)
                .setView(view1)
                .create();
        final AlertDialog show = alertDialog71.show();
        buttonruleok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Object senderId = ruleSenderTv.getTag();
                if (ruleModel == null) {
                    RuleModel newRuleModel = new RuleModel();
                    newRuleModel.setFiled(RuleModel.getRuleFiledFromCheckId(radioGroupRuleFiled.getCheckedRadioButtonId()));
                    newRuleModel.setCheck(RuleModel.getRuleCheckFromCheckId(radioGroupRuleCheck.getCheckedRadioButtonId()));
                    newRuleModel.setValue(editTextRuleValue.getText().toString());
                    if (senderId != null) {
                        newRuleModel.setSenderId(Long.valueOf(senderId.toString()));
                    }
                    RuleUtil.addRule(newRuleModel);
                    initRules();
                    adapter.add(ruleModels);
                } else {
                    ruleModel.setFiled(RuleModel.getRuleFiledFromCheckId(radioGroupRuleFiled.getCheckedRadioButtonId()));
                    ruleModel.setCheck(RuleModel.getRuleCheckFromCheckId(radioGroupRuleCheck.getCheckedRadioButtonId()));
                    ruleModel.setValue(editTextRuleValue.getText().toString());
                    if (senderId != null) {
                        ruleModel.setSenderId(Long.valueOf(senderId.toString()));
                    }
                    RuleUtil.updateRule(ruleModel);
                    initRules();
                    adapter.update(ruleModels);
                }

                show.dismiss();


            }
        });
        buttonruledel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ruleModel != null) {
                    RuleUtil.delRule(ruleModel.getId());
                    initRules();
                    adapter.del(ruleModels);
                }
                show.dismiss();
            }
        });
    }

    public void selectSender(final TextView showTv) {
        final List<SenderModel> senderModels = SenderUtil.getSender(null, null);
        if (senderModels.isEmpty()) {
            Toast.makeText(RuleActivity.this, "请先去设置发送方页面添加", Toast.LENGTH_SHORT).show();
            return;
        }
        final CharSequence[] senderNames = new CharSequence[senderModels.size()];
        for (int i = 0; i < senderModels.size(); i++) {
            senderNames[i] = senderModels.get(i).getName();
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(RuleActivity.this);
        builder.setTitle("选择发送方");
        builder.setItems(senderNames, new DialogInterface.OnClickListener() {//添加列表
            @Override
            public void onClick(DialogInterface dialogInterface, int which) {
                Toast.makeText(RuleActivity.this, senderNames[which], Toast.LENGTH_LONG).show();
                showTv.setText(senderNames[which]);
                showTv.setTag(senderModels.get(which).getId());
            }
        });
        builder.show();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
    }


    @Override
    protected void onResume() {
        super.onResume();
        MobclickAgent.onResume(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        MobclickAgent.onPause(this);
    }

}
