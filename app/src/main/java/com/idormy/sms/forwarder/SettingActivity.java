package com.idormy.sms.forwarder;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.idormy.sms.forwarder.utils.CommonUtil;
import com.idormy.sms.forwarder.utils.DbHelper;
import com.idormy.sms.forwarder.utils.KeepAliveUtils;
import com.idormy.sms.forwarder.utils.SettingUtil;

import java.util.List;

public class SettingActivity extends AppCompatActivity {
    private final String TAG = "SettingActivity";
    private TextView textView;

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

        EditText et_battery_level_alarm_min = findViewById(R.id.et_battery_level_alarm_min);
        editBatteryLevelAlarmMin(et_battery_level_alarm_min);
        EditText et_battery_level_alarm_max = findViewById(R.id.et_battery_level_alarm_max);
        editBatteryLevelAlarmMax(et_battery_level_alarm_max);

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

        @SuppressLint("UseSwitchCompatOrMaterialCode") Switch switch_enable_sms = findViewById(R.id.switch_enable_sms);
        switchEnableSms(switch_enable_sms);

        @SuppressLint("UseSwitchCompatOrMaterialCode") Switch switch_enable_phone = findViewById(R.id.switch_enable_phone);
        switchEnablePhone(switch_enable_phone);

        @SuppressLint("UseSwitchCompatOrMaterialCode") Switch switch_enable_app_notify = findViewById(R.id.switch_enable_app_notify);
        @SuppressLint("UseSwitchCompatOrMaterialCode") Switch switch_cancel_app_notify = findViewById(R.id.switch_cancel_app_notify);
        switchEnableAppNotify(switch_enable_app_notify, switch_cancel_app_notify);

        @SuppressLint("UseSwitchCompatOrMaterialCode") Switch switch_exclude_from_recents = findViewById(R.id.switch_exclude_from_recents);
        switchExcludeFromRecents(switch_exclude_from_recents);

        @SuppressLint("UseSwitchCompatOrMaterialCode") Switch switch_battery_receiver = findViewById(R.id.switch_battery_receiver);
        switchBatteryReceiver(switch_battery_receiver);

        EditText textSmsTemplate = findViewById(R.id.text_sms_template);
        editSmsTemplate(textSmsTemplate);
    }

    //设置转发短信
    private void switchEnableSms(@SuppressLint("UseSwitchCompatOrMaterialCode") Switch switch_enable_sms) {
        switch_enable_sms.setChecked(SettingUtil.getSwitchEnableSms());

        switch_enable_sms.setOnCheckedChangeListener((buttonView, isChecked) -> {
            //TODO:校验使用短信转发必备的权限
            SettingUtil.switchEnableSms(isChecked);
            Log.d(TAG, "switchEnableSms:" + isChecked);
        });
    }

    //设置转发来电
    private void switchEnablePhone(@SuppressLint("UseSwitchCompatOrMaterialCode") Switch switch_enable_phone) {
        switch_enable_phone.setChecked(SettingUtil.getSwitchEnablePhone());

        switch_enable_phone.setOnCheckedChangeListener((buttonView, isChecked) -> {
            //TODO:校验使用来电转发必备的权限
            SettingUtil.switchEnablePhone(isChecked);
            Log.d(TAG, "switchEnablePhone:" + isChecked);
        });
    }

    //监听电池状态变化
    private void switchBatteryReceiver(@SuppressLint("UseSwitchCompatOrMaterialCode") Switch switch_battery_receiver) {
        switch_battery_receiver.setChecked(SettingUtil.getSwitchEnableBatteryReceiver());

        switch_battery_receiver.setOnCheckedChangeListener((buttonView, isChecked) -> {
            //TODO:校验使用来电转发必备的权限
            SettingUtil.switchEnableBatteryReceiver(isChecked);
            Log.d(TAG, "switchEnablePhone:" + isChecked);
        });
    }

    //设置转发APP通知
    private void switchEnableAppNotify(@SuppressLint("UseSwitchCompatOrMaterialCode") Switch switch_enable_app_notify, @SuppressLint("UseSwitchCompatOrMaterialCode") Switch switch_cancel_app_notify) {
        final LinearLayout layout_cancel_app_notify = findViewById(R.id.layout_cancel_app_notify);
        boolean isEnable = SettingUtil.getSwitchEnableAppNotify();
        switch_enable_app_notify.setChecked(isEnable);
        layout_cancel_app_notify.setVisibility(isEnable ? View.VISIBLE : View.GONE);

        switch_enable_app_notify.setOnCheckedChangeListener((buttonView, isChecked) -> {
            layout_cancel_app_notify.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            //TODO:校验使用APP通知转发必备的权限
            if (isChecked) {
                if (!CommonUtil.isNotificationListenerServiceEnabled(this)) {
                    CommonUtil.openNotificationAccess(this);
                    Toast.makeText(this, "请先授予《短信转发器》通知使用权，否则无法转发APP通知，开启失败!", Toast.LENGTH_LONG).show();
                    return;
                } else {
                    Toast.makeText(this, "通知服务已开启", Toast.LENGTH_LONG).show();
                    CommonUtil.toggleNotificationListenerService(this);
                }
            }
            SettingUtil.switchEnableAppNotify(isChecked);
            Log.d(TAG, "switchEnableAppNotify:" + isChecked);
        });

        switch_cancel_app_notify.setChecked(SettingUtil.getSwitchCancelAppNotify());
        switch_cancel_app_notify.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SettingUtil.switchCancelAppNotify(isChecked);
            Log.d(TAG, "switchCancelAppNotify:" + isChecked);
        });
    }

    //不在最近任务列表中显示
    @SuppressLint("ObsoleteSdkInt")
    private void switchExcludeFromRecents(@SuppressLint("UseSwitchCompatOrMaterialCode") Switch switch_exclude_from_recents) {
        switch_exclude_from_recents.setChecked(SettingUtil.getExcludeFromRecents());

        switch_exclude_from_recents.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SettingUtil.switchExcludeFromRecents(isChecked);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                ActivityManager am = (ActivityManager) this.getSystemService(Context.ACTIVITY_SERVICE);
                if (am != null) {
                    List<ActivityManager.AppTask> appTasks = am.getAppTasks();
                    if (appTasks != null && !appTasks.isEmpty()) {
                        appTasks.get(0).setExcludeFromRecents(isChecked);
                    }
                }
            }
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
                SettingUtil.setAddExtraDeviceMark(et_add_extra_device_mark.getText().toString().trim());
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
                SettingUtil.setAddExtraSim1(et_add_extra_sim1.getText().toString().trim());
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
                SettingUtil.setAddExtraSim2(et_add_extra_sim2.getText().toString().trim());
            }
        });
    }

    //设置低电量报警值下限
    private void editBatteryLevelAlarmMin(final EditText et_battery_level_alarm) {
        et_battery_level_alarm.setText(String.valueOf(SettingUtil.getBatteryLevelAlarmMin()));

        et_battery_level_alarm.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String batteryLevel = et_battery_level_alarm.getText().toString().trim();
                if (!batteryLevel.isEmpty()) {
                    SettingUtil.setBatteryLevelAlarmMin(Integer.parseInt(batteryLevel));
                } else {
                    SettingUtil.setBatteryLevelAlarmMin(0);
                }
            }
        });
    }

    //设置低电量报警值上限
    private void editBatteryLevelAlarmMax(final EditText et_battery_level_alarm) {
        et_battery_level_alarm.setText(String.valueOf(SettingUtil.getBatteryLevelAlarmMax()));

        et_battery_level_alarm.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String batteryLevel = et_battery_level_alarm.getText().toString().trim();
                if (!batteryLevel.isEmpty()) {
                    SettingUtil.setBatteryLevelAlarmMax(Integer.parseInt(batteryLevel));
                } else {
                    SettingUtil.setBatteryLevelAlarmMax(0);
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
                String delayTime = et_retry_delay_time.getText().toString().trim();
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
                SettingUtil.setSmsTemplate(textSmsTemplate.getText().toString().trim());
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
                insertOrReplaceText2Cursor(textSmsTemplate, "{{来源号码}}");
                return;
            case R.id.bt_insert_content:
                insertOrReplaceText2Cursor(textSmsTemplate, "{{短信内容}}");
                return;
            case R.id.bt_insert_extra:
                insertOrReplaceText2Cursor(textSmsTemplate, "{{卡槽信息}}");
                return;
            case R.id.bt_insert_time:
                insertOrReplaceText2Cursor(textSmsTemplate, "{{接收时间}}");
                return;
            case R.id.bt_insert_device_name:
                insertOrReplaceText2Cursor(textSmsTemplate, "{{设备名称}}");
                return;
            default:
        }
    }

    private void insertOrReplaceText2Cursor(EditText editText, String str) {
        int start = Math.max(editText.getSelectionStart(), 0);
        int end = Math.max(editText.getSelectionEnd(), 0);
        editText.getText().replace(Math.min(start, end), Math.max(start, end), str, 0, str.length());
    }

    //恢复初始化配置
    public void initSetting(View view) {

        AlertDialog.Builder builder = new AlertDialog.Builder(SettingActivity.this);
        builder.setTitle(R.string.init_setting);
        builder.setMessage(R.string.init_setting_tips);

        //添加AlertDialog.Builder对象的setPositiveButton()方法
        builder.setPositiveButton(R.string.confirm, (dialog, which) -> {
            //初始化配置
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor editor = preferences.edit();
            editor.clear();
            editor.apply();

            //初始化数据库
            DbHelper dbHelper = new DbHelper(this);
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            dbHelper.delCreateTable(db);
            dbHelper.onCreate(db);

            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        });

        //添加AlertDialog.Builder对象的setNegativeButton()方法
        builder.setNegativeButton(R.string.cancel, (dialog, which) -> {

        });

        builder.create().show();
    }

    //电池优化设置
    @RequiresApi(api = Build.VERSION_CODES.M)
    public void batterySetting(View view) {
        if (KeepAliveUtils.isIgnoreBatteryOptimization(this)) {
            Toast.makeText(this, R.string.isIgnored, Toast.LENGTH_SHORT).show();
        } else {
            KeepAliveUtils.ignoreBatteryOptimization(this);
        }
    }

    /**
     * 请求通知使用权限
     *
     * @param view 控件
     */
    public void requestNotificationPermission(View view) {
        if (!CommonUtil.isNotificationListenerServiceEnabled(this)) {
            CommonUtil.openNotificationAccess(this);
        } else {
            Toast.makeText(this, R.string.notification_listener_service_enabled, Toast.LENGTH_SHORT).show();
            CommonUtil.toggleNotificationListenerService(this);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CommonUtil.NOTIFICATION_REQUEST_CODE) {
            if (CommonUtil.isNotificationListenerServiceEnabled(this)) {
                Toast.makeText(this, R.string.notification_listener_service_enabled, Toast.LENGTH_SHORT).show();
                CommonUtil.toggleNotificationListenerService(this);
                SettingUtil.switchEnableAppNotify(true);
            } else {
                Toast.makeText(this, R.string.notification_listener_service_disabled, Toast.LENGTH_SHORT).show();
                SettingUtil.switchEnableAppNotify(false);
            }

            @SuppressLint("UseSwitchCompatOrMaterialCode") Switch switch_enable_app_notify = findViewById(R.id.switch_enable_app_notify);
            switch_enable_app_notify.setChecked(SettingUtil.getSwitchEnableAppNotify());
        }
    }

}
