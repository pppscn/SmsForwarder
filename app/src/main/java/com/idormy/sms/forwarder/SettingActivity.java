package com.idormy.sms.forwarder;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;

import androidx.appcompat.app.AppCompatActivity;

import com.idormy.sms.forwarder.utils.SettingUtil;


public class SettingActivity extends AppCompatActivity {
    private String TAG = "SettingActivity";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "oncreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        Switch switch_add_extra = (Switch) findViewById(R.id.switch_add_extra);
        switchAddExtra(switch_add_extra);

        EditText et_add_extra_device_mark = (EditText) findViewById(R.id.et_add_extra_device_mark);
        editAddExtraDeviceMark(et_add_extra_device_mark);

        EditText et_add_extra_sim1 = (EditText) findViewById(R.id.et_add_extra_sim1);
        editAddExtraSim1(et_add_extra_sim1);

        EditText et_add_extra_sim2 = (EditText) findViewById(R.id.et_add_extra_sim2);
        editAddExtraSim2(et_add_extra_sim2);

        Switch switch_sms_template = (Switch) findViewById(R.id.switch_sms_template);
        switchSmsTemplate(switch_sms_template);

        EditText textSmsTemplate = (EditText) findViewById(R.id.text_sms_template);
        editSmsTemplate(textSmsTemplate);
    }

    //设置转发附加信息
    private void switchAddExtra(Switch switch_add_extra) {
        switch_add_extra.setChecked(SettingUtil.getSwitchAddExtra());

        switch_add_extra.setOnCheckedChangeListener(new Switch.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SettingUtil.switchAddExtra(isChecked);
                Log.d(TAG, "onCheckedChanged:" + isChecked);
            }
        });
    }

    //设置转发附加信息devicemark
    private void editAddExtraDeviceMark(final EditText et_add_extra_device_mark) {
        et_add_extra_device_mark.setText(SettingUtil.getAddExtraDeviceMark());

        et_add_extra_device_mark.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                SettingUtil.setAddExtraDeviceMark(et_add_extra_device_mark.getText().toString());
            }
        });
    }

    //设置转发附加信息devicemark
    private void editAddExtraSim1(final EditText et_add_extra_sim1) {
        et_add_extra_sim1.setText(SettingUtil.getAddExtraSim1());

        et_add_extra_sim1.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                SettingUtil.setAddExtraSim1(et_add_extra_sim1.getText().toString());
            }
        });
    }

    //设置转发附加信息devicemark
    private void editAddExtraSim2(final EditText et_add_extra_sim2) {
        et_add_extra_sim2.setText(SettingUtil.getAddExtraSim2());

        et_add_extra_sim2.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                SettingUtil.setAddExtraSim2(et_add_extra_sim2.getText().toString());
            }
        });
    }

    //设置转发时启用自定义模版
    private void switchSmsTemplate(Switch switch_sms_template) {
        boolean isOn = SettingUtil.getSwitchSmsTemplate();
        switch_sms_template.setChecked(isOn);

        final LinearLayout layout_sms_template = (LinearLayout) findViewById(R.id.layout_sms_template);
        layout_sms_template.setVisibility(isOn ? View.VISIBLE : View.GONE);
        final EditText textSmsTemplate = (EditText) findViewById(R.id.text_sms_template);

        switch_sms_template.setOnCheckedChangeListener(new Switch.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Log.d(TAG, "onCheckedChanged:" + isChecked);
                layout_sms_template.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                SettingUtil.switchSmsTemplate(isChecked);
                if (!isChecked) {
                    textSmsTemplate.setText("{{来源号码}}\n{{短信内容}}\n{{卡槽信息}}\n{{接收时间}}\n{{设备名称}}");
                }
            }
        });
    }

    //设置转发附加信息devicemark
    private void editSmsTemplate(final EditText textSmsTemplate) {
        textSmsTemplate.setText(SettingUtil.getSmsTemplate());

        textSmsTemplate.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                SettingUtil.setSmsTemplate(textSmsTemplate.getText().toString());
            }
        });
    }

    //插入标签
    public void toInsertLabel(View v) {
        EditText textSmsTemplate = (EditText) findViewById(R.id.text_sms_template);
        textSmsTemplate.setFocusable(true);
        textSmsTemplate.requestFocus();
        switch (v.getId()) {
            case R.id.bt_insert_sender:
                textSmsTemplate.append("{{来源号码}}");
                return;
            /*case R.id.bt_insert_receiver:
                textSmsTemplate.append("{{接收号码}}");
                return;*/
            case R.id.bt_insert_content:
                textSmsTemplate.append("{{短信内容}}");
                return;
            case R.id.bt_insert_extra:
                textSmsTemplate.append("{{卡槽信息}}");
                return;
            case R.id.bt_insert_time:
                textSmsTemplate.append("{{接收时间}}");
                return;
            case R.id.bt_insert_device_name:
                textSmsTemplate.append("{{设备名称}}");
                return;
            default:
                return;
        }
    }

    //恢复初始化配置
    public void initSetting(View v) {
        Switch switch_add_extra = (Switch) findViewById(R.id.switch_add_extra);
        switch_add_extra.setChecked(false);
        switchAddExtra(switch_add_extra);

        EditText et_add_extra_device_mark = (EditText) findViewById(R.id.et_add_extra_device_mark);
        et_add_extra_device_mark.setText("");
        editAddExtraDeviceMark(et_add_extra_device_mark);

        EditText et_add_extra_sim1 = (EditText) findViewById(R.id.et_add_extra_sim1);
        et_add_extra_sim1.setText("");
        editAddExtraSim1(et_add_extra_sim1);

        EditText et_add_extra_sim2 = (EditText) findViewById(R.id.et_add_extra_sim2);
        et_add_extra_sim2.setText("");
        editAddExtraSim2(et_add_extra_sim2);

        Switch switch_sms_template = (Switch) findViewById(R.id.switch_sms_template);
        switch_sms_template.setChecked(false);
        switchSmsTemplate(switch_sms_template);

        EditText textSmsTemplate = (EditText) findViewById(R.id.text_sms_template);
        textSmsTemplate.setText("{{来源号码}}\n{{短信内容}}\n{{卡槽信息}}\n{{接收时间}}\n{{设备名称}}");
        editSmsTemplate(textSmsTemplate);

    }

}
