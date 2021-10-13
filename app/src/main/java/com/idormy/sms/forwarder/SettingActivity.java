package com.idormy.sms.forwarder;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.idormy.sms.forwarder.utils.KeepAliveUtils;
import com.idormy.sms.forwarder.utils.SettingUtil;


public class SettingActivity extends AppCompatActivity {
    private final String TAG = "SettingActivity";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        EditText et_add_extra_device_mark = findViewById(R.id.et_add_extra_device_mark);
        editAddExtraDeviceMark(et_add_extra_device_mark);

        EditText et_add_extra_sim1 = findViewById(R.id.et_add_extra_sim1);
        editAddExtraSim1(et_add_extra_sim1);

        EditText et_add_extra_sim2 = findViewById(R.id.et_add_extra_sim2);
        editAddExtraSim2(et_add_extra_sim2);

        EditText et_battery_level_alarm = findViewById(R.id.et_battery_level_alarm);
        editBatteryLevelAlarm(et_battery_level_alarm);

        EditText et_retry_delay_time1 = findViewById(R.id.et_retry_delay_time1);
        editRetryDelayTime(et_retry_delay_time1, 1);
        EditText et_retry_delay_time2 = findViewById(R.id.et_retry_delay_time2);
        editRetryDelayTime(et_retry_delay_time2, 2);
        EditText et_retry_delay_time3 = findViewById(R.id.et_retry_delay_time3);
        editRetryDelayTime(et_retry_delay_time3, 3);
        EditText et_retry_delay_time4 = findViewById(R.id.et_retry_delay_time4);
        editRetryDelayTime(et_retry_delay_time4, 4);
        EditText et_retry_delay_time5 = findViewById(R.id.et_retry_delay_time5);
        editRetryDelayTime(et_retry_delay_time5, 5);

        @SuppressLint("UseSwitchCompatOrMaterialCode") Switch switch_sms_template = findViewById(R.id.switch_sms_template);
        switchSmsTemplate(switch_sms_template);

        @SuppressLint("UseSwitchCompatOrMaterialCode") Switch switch_enable_phone = findViewById(R.id.switch_enable_phone);
        switchEnablePhone(switch_enable_phone);

        EditText textSmsTemplate = findViewById(R.id.text_sms_template);
        editSmsTemplate(textSmsTemplate);
    }

    //设置转发来电
    private void switchEnablePhone(@SuppressLint("UseSwitchCompatOrMaterialCode") Switch switch_enable_phone) {
        switch_enable_phone.setChecked(SettingUtil.getSwitchEnablePhone());

        switch_enable_phone.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SettingUtil.switchEnablePhone(isChecked);
            Log.d(TAG, "onCheckedChanged:" + isChecked);
        });
    }

    //设置设备名称
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

    //设置SIM1备注
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

    //设置SIM2备注
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

    //设置低电量报警值
    private void editBatteryLevelAlarm(final EditText et_battery_level_alarm) {
        et_battery_level_alarm.setText(String.valueOf(SettingUtil.getBatteryLevelAlarm()));

        et_battery_level_alarm.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String batteryLevel = et_battery_level_alarm.getText().toString();
                if (!batteryLevel.isEmpty()) {
                    SettingUtil.setBatteryLevelAlarm(Integer.parseInt(batteryLevel));
                } else {
                    SettingUtil.setBatteryLevelAlarm(0);
                }
            }
        });
    }


    //接口请求失败重试
    private void editRetryDelayTime(final EditText et_retry_delay_time, final int index) {
        et_retry_delay_time.setText(String.valueOf(SettingUtil.getRetryDelayTime(index)));

        et_retry_delay_time.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String delayTime = et_retry_delay_time.getText().toString();
                if (!delayTime.isEmpty()) {
                    SettingUtil.setRetryDelayTime(index, Integer.parseInt(delayTime));
                } else {
                    SettingUtil.setRetryDelayTime(index, 0);
                }
            }
        });
    }

    //设置转发时启用自定义模版
    private void switchSmsTemplate(@SuppressLint("UseSwitchCompatOrMaterialCode") Switch switch_sms_template) {
        boolean isOn = SettingUtil.getSwitchSmsTemplate();
        switch_sms_template.setChecked(isOn);

        final LinearLayout layout_sms_template = findViewById(R.id.layout_sms_template);
        layout_sms_template.setVisibility(isOn ? View.VISIBLE : View.GONE);
        final EditText textSmsTemplate = findViewById(R.id.text_sms_template);

        switch_sms_template.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Log.d(TAG, "onCheckedChanged:" + isChecked);
            layout_sms_template.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            SettingUtil.switchSmsTemplate(isChecked);
            if (!isChecked) {
                textSmsTemplate.setText("{{来源号码}}\n{{短信内容}}\n{{卡槽信息}}\n{{接收时间}}\n{{设备名称}}");
            }
        });
    }

    //设置转发信息模版
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
    @SuppressLint("NonConstantResourceId")
    public void toInsertLabel(View v) {
        EditText textSmsTemplate = findViewById(R.id.text_sms_template);
        textSmsTemplate.setFocusable(true);
        textSmsTemplate.requestFocus();
        switch (v.getId()) {
            case R.id.bt_insert_sender:
                textSmsTemplate.append("{{来源号码}}");
                return;
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
        }
    }

    //恢复初始化配置
    public void initSetting(View view) {

        EditText et_add_extra_device_mark = findViewById(R.id.et_add_extra_device_mark);
        et_add_extra_device_mark.setText("");
        editAddExtraDeviceMark(et_add_extra_device_mark);

        EditText et_add_extra_sim1 = findViewById(R.id.et_add_extra_sim1);
        et_add_extra_sim1.setText("");
        editAddExtraSim1(et_add_extra_sim1);

        EditText et_add_extra_sim2 = findViewById(R.id.et_add_extra_sim2);
        et_add_extra_sim2.setText("");
        editAddExtraSim2(et_add_extra_sim2);

        @SuppressLint("UseSwitchCompatOrMaterialCode") Switch switch_sms_template = findViewById(R.id.switch_sms_template);
        switch_sms_template.setChecked(false);
        switchSmsTemplate(switch_sms_template);

        EditText textSmsTemplate = findViewById(R.id.text_sms_template);
        textSmsTemplate.setText("{{来源号码}}\n{{短信内容}}\n{{卡槽信息}}\n{{接收时间}}\n{{设备名称}}");
        editSmsTemplate(textSmsTemplate);

    }

    public void batterySetting(View view) {
        if (KeepAliveUtils.isIgnoreBatteryOptimization(this)) {
            Toast.makeText(this, R.string.isIgnored, Toast.LENGTH_SHORT).show();
        } else {
            KeepAliveUtils.ignoreBatteryOptimization(this);
        }
    }
}
