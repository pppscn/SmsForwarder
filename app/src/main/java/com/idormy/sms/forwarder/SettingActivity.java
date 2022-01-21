package com.idormy.sms.forwarder;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.idormy.sms.forwarder.receiver.RebootBroadcastReceiver;
import com.idormy.sms.forwarder.sender.HttpServer;
import com.idormy.sms.forwarder.sender.SenderUtil;
import com.idormy.sms.forwarder.sender.SmsHubApiTask;
import com.idormy.sms.forwarder.utils.CommonUtil;
import com.idormy.sms.forwarder.utils.DbHelper;
import com.idormy.sms.forwarder.utils.Define;
import com.idormy.sms.forwarder.utils.HttpUtil;
import com.idormy.sms.forwarder.utils.KeepAliveUtils;
import com.idormy.sms.forwarder.utils.LogUtil;
import com.idormy.sms.forwarder.utils.RuleUtil;
import com.idormy.sms.forwarder.utils.SettingUtil;
import com.idormy.sms.forwarder.view.ClearEditText;
import com.idormy.sms.forwarder.view.StepBar;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SettingActivity extends AppCompatActivity {
    private final String TAG = "SettingActivity";
    private Context context;
    private boolean isIgnoreBatteryOptimization;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        context = SettingActivity.this;
        setContentView(R.layout.activity_setting);

        LogUtil.init(this);
        RuleUtil.init(this);
        SenderUtil.init(this);
    }

    @SuppressLint("NewApi")
    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");

        //是否关闭页面提示
        TextView help_tip = findViewById(R.id.help_tip);
        ScrollView scrollView = findViewById(R.id.scrollView);
        CommonUtil.calcMarginBottom(this, help_tip, null, null, scrollView);

        //转发短信广播
        switchEnableSms(findViewById(R.id.switch_enable_sms));
        //转发通话记录
        switchEnablePhone(findViewById(R.id.switch_enable_phone), findViewById(R.id.cbCallType1), findViewById(R.id.cbCallType2), findViewById(R.id.cbCallType3));
        //转发应用通知 & 自动关闭通知
        switchEnableAppNotify(findViewById(R.id.switch_enable_app_notify), findViewById(R.id.checkbox_cancel_app_notify));

        //HttpServer
        switchEnableHttpServer(findViewById(R.id.switch_enable_http_server));
        //SmsHubApiTask
        editSmsHubConfig(findViewById(R.id.switch_enable_sms_hub), findViewById(R.id.editText_text_sms_hub_url));

        //监听电池状态变化
        switchBatteryReceiver(findViewById(R.id.switch_battery_receiver));
        //电量预警
        editBatteryLevelAlarmMin(findViewById(R.id.et_battery_level_alarm_min));
        editBatteryLevelAlarmMax(findViewById(R.id.et_battery_level_alarm_max));

        //开机启动
        checkWithReboot(findViewById(R.id.switch_with_reboot));
        //电池优化设置
        batterySetting(findViewById(R.id.switch_battery_setting));
        //不在最近任务列表中显示
        switchExcludeFromRecents(findViewById(R.id.switch_exclude_from_recents));

        //是否开启失败重试
        switchRetryDelay(findViewById(R.id.switch_retry_delay));
        //接口请求失败重试时间间隔
        editRetryDelayTime(findViewById(R.id.et_retry_delay_time1), 1);
        editRetryDelayTime(findViewById(R.id.et_retry_delay_time2), 2);
        editRetryDelayTime(findViewById(R.id.et_retry_delay_time3), 3);
        editRetryDelayTime(findViewById(R.id.et_retry_delay_time4), 4);
        editRetryDelayTime(findViewById(R.id.et_retry_delay_time5), 5);

        //设备备注
        editAddExtraDeviceMark(findViewById(R.id.et_add_extra_device_mark));
        //SIM1备注
        editAddExtraSim1(findViewById(R.id.et_add_extra_sim1));
        //SIM2备注
        editAddExtraSim2(findViewById(R.id.et_add_extra_sim2));
        //启用自定义模版
        switchSmsTemplate(findViewById(R.id.switch_sms_template));
        //自定义模板
        editSmsTemplate(findViewById(R.id.text_sms_template));

        //帮助提示
        SwitchHelpTip(findViewById(R.id.switch_help_tip));

        //步骤完成状态校验
        boolean checkStep1 = SettingUtil.getSwitchEnableSms() || SettingUtil.getSwitchEnablePhone() || SettingUtil.getSwitchEnableAppNotify();
        boolean checkStep2 = SenderUtil.countSender("1", null) > 0;
        boolean checkStep3 = RuleUtil.countRule("1", null, null) > 0;
        boolean checkStep4 = LogUtil.countLog("2", null, null) > 0;
        StepBar stepBar = findViewById(R.id.stepBar);
        stepBar.setHighlight(checkStep1, checkStep2, checkStep3, checkStep4);
    }

    @Override
    protected void onPause() {
        overridePendingTransition(0, 0);
        super.onPause();
    }

    //设置转发短信
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private void switchEnableSms(Switch switch_enable_sms) {
        switch_enable_sms.setChecked(SettingUtil.getSwitchEnableSms());

        switch_enable_sms.setOnCheckedChangeListener((buttonView, isChecked) -> {
            //TODO:校验使用短信转发必备的权限
            SettingUtil.switchEnableSms(isChecked);
            Log.d(TAG, "switchEnableSms:" + isChecked);
        });
    }

    //转发通话记录
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private void switchEnablePhone(Switch switch_enable_phone, CheckBox check_box_call_type_1, CheckBox check_box_call_type_2, CheckBox check_box_call_type_3) {
        switch_enable_phone.setChecked(SettingUtil.getSwitchEnablePhone());
        check_box_call_type_1.setChecked(SettingUtil.getSwitchCallType1());
        check_box_call_type_2.setChecked(SettingUtil.getSwitchCallType2());
        check_box_call_type_3.setChecked(SettingUtil.getSwitchCallType3());

        switch_enable_phone.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked && !SettingUtil.getSwitchCallType1() && !SettingUtil.getSwitchCallType2() && !SettingUtil.getSwitchCallType3()) {
                Toast.makeText(context, "必选选择一个通话类型，才能开启通话记录转发！", Toast.LENGTH_SHORT).show();
                SettingUtil.switchEnablePhone(false);
                return;
            }

            //TODO:校验使用来电转发必备的权限
            SettingUtil.switchEnablePhone(isChecked);
            Log.d(TAG, "switchEnablePhone:" + isChecked);
        });

        check_box_call_type_1.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SettingUtil.switchCallType1(isChecked);
            if (!isChecked && !SettingUtil.getSwitchCallType1() && !SettingUtil.getSwitchCallType2() && !SettingUtil.getSwitchCallType3()) {
                Toast.makeText(context, "必选选择一个通话类型，才能开启通话记录转发！", Toast.LENGTH_SHORT).show();
                SettingUtil.switchEnablePhone(false);
            }
        });

        check_box_call_type_2.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SettingUtil.switchCallType2(isChecked);
            if (!isChecked && !SettingUtil.getSwitchCallType1() && !SettingUtil.getSwitchCallType2() && !SettingUtil.getSwitchCallType3()) {
                Toast.makeText(context, "必选选择一个通话类型，才能开启通话记录转发！", Toast.LENGTH_SHORT).show();
                SettingUtil.switchEnablePhone(false);
            }
        });

        check_box_call_type_3.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SettingUtil.switchCallType3(isChecked);
            if (!isChecked && !SettingUtil.getSwitchCallType1() && !SettingUtil.getSwitchCallType2() && !SettingUtil.getSwitchCallType3()) {
                Toast.makeText(context, "必选选择一个通话类型，才能开启通话记录转发！", Toast.LENGTH_SHORT).show();
                SettingUtil.switchEnablePhone(false);
            }
        });
    }

    //转发应用通知 & 自动关闭通知
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private void switchEnableAppNotify(Switch switch_enable_app_notify, CheckBox checkbox_cancel_app_notify) {
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

        checkbox_cancel_app_notify.setChecked(SettingUtil.getSwitchCancelAppNotify());
        checkbox_cancel_app_notify.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SettingUtil.switchCancelAppNotify(isChecked);
            Log.d(TAG, "switchCancelAppNotify:" + isChecked);
        });
    }

    //请求通知使用权限
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

    //SmsHubApiTask
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private void editSmsHubConfig(Switch switch_enable_send_sms, ClearEditText editText_text_send_sms) {
        switch_enable_send_sms.setChecked(SettingUtil.getSwitchEnableSmsHubApi());
        switch_enable_send_sms.setOnCheckedChangeListener((buttonView, isChecked) -> {
            String webServer = editText_text_send_sms.getText().trim();
            if (isChecked && !CommonUtil.checkUrl(webServer, false)) {
                HttpUtil.Toast(TAG, "url为空或不正确无法启用");
                switch_enable_send_sms.setChecked(false);
                return;
            }
            SettingUtil.switchEnableSmsHubApi(isChecked);
            Log.d(TAG, "switchEnableSendApi:" + isChecked);
            SmsHubApiTask.updateTimer();
        });

        editText_text_send_sms.setText(SettingUtil.getSmsHubApiUrl());
        editText_text_send_sms.setOnEditInputListener(content -> SettingUtil.smsHubApiUrl(content.trim()));
    }

    //HttpServer
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private void switchEnableHttpServer(Switch switch_enable_http_server) {
        switch_enable_http_server.setChecked(SettingUtil.getSwitchEnableHttpServer());

        switch_enable_http_server.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SettingUtil.switchEnableHttpServer(isChecked);
            Log.d(TAG, "switchEnableHttpServer:" + isChecked);

            HttpUtil.init(this);
            HttpServer.init(this);
            HttpServer.update();
        });
    }

    //监听电池状态变化
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private void switchBatteryReceiver(Switch switch_battery_receiver) {
        switch_battery_receiver.setChecked(SettingUtil.getSwitchEnableBatteryReceiver());

        switch_battery_receiver.setOnCheckedChangeListener((buttonView, isChecked) -> {
            //TODO:校验使用来电转发必备的权限
            SettingUtil.switchEnableBatteryReceiver(isChecked);
            Log.d(TAG, "switchEnablePhone:" + isChecked);
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

    //开机启动
    private void checkWithReboot(@SuppressLint("UseSwitchCompatOrMaterialCode") Switch withrebootSwitch) {
        //获取组件
        final ComponentName cm = new ComponentName(this.getPackageName(), RebootBroadcastReceiver.class.getName());

        final PackageManager pm = getPackageManager();
        int state = pm.getComponentEnabledSetting(cm);
        withrebootSwitch.setChecked(state != PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                && state != PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER);
        withrebootSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            int newState = isChecked ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                    : PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
            pm.setComponentEnabledSetting(cm, newState, PackageManager.DONT_KILL_APP);
            Log.d(TAG, "onCheckedChanged:" + isChecked);

            if (isChecked) startToAutoStartSetting(this);
        });
    }

    //Intent跳转到[自启动]页面全网最全适配机型解决方案
    private static final HashMap<String, List<String>> hashMap = new HashMap<>() {
        {
            put("Xiaomi", Arrays.asList(
                    "com.miui.securitycenter/com.miui.permcenter.autostart.AutoStartManagementActivity",//MIUI10_9.8.1(9.0)
                    "com.miui.securitycenter"
            ));

            put("samsung", Arrays.asList(
                    "com.samsung.android.sm_cn/com.samsung.android.sm.ui.ram.AutoRunActivity",
                    "com.samsung.android.sm_cn/com.samsung.android.sm.ui.appmanagement.AppManagementActivity",
                    "com.samsung.android.sm_cn/com.samsung.android.sm.ui.cstyleboard.SmartManagerDashBoardActivity",
                    "com.samsung.android.sm_cn/.ui.ram.RamActivity",
                    "com.samsung.android.sm_cn/.app.dashboard.SmartManagerDashBoardActivity",

                    "com.samsung.android.sm/com.samsung.android.sm.ui.ram.AutoRunActivity",
                    "com.samsung.android.sm/com.samsung.android.sm.ui.appmanagement.AppManagementActivity",
                    "com.samsung.android.sm/com.samsung.android.sm.ui.cstyleboard.SmartManagerDashBoardActivity",
                    "com.samsung.android.sm/.ui.ram.RamActivity",
                    "com.samsung.android.sm/.app.dashboard.SmartManagerDashBoardActivity",

                    "com.samsung.android.lool/com.samsung.android.sm.ui.battery.BatteryActivity",
                    "com.samsung.android.sm_cn",
                    "com.samsung.android.sm"
            ));

            put("HUAWEI", Arrays.asList(
                    "com.huawei.systemmanager/.startupmgr.ui.StartupNormalAppListActivity",//EMUI9.1.0(方舟,9.0)
                    "com.huawei.systemmanager/.appcontrol.activity.StartupAppControlActivity",
                    "com.huawei.systemmanager/.optimize.process.ProtectActivity",
                    "com.huawei.systemmanager/.optimize.bootstart.BootStartActivity",
                    "com.huawei.systemmanager"//最后一行可以写包名, 这样如果签名的类路径在某些新版本的ROM中没找到 就直接跳转到对应的安全中心/手机管家 首页.
            ));

            put("vivo", Arrays.asList(
                    "com.iqoo.secure/.ui.phoneoptimize.BgStartUpManager",
                    "com.iqoo.secure/.safeguard.PurviewTabActivity",
                    "com.vivo.permissionmanager/.activity.BgStartUpManagerActivity",
                    //"com.iqoo.secure/.ui.phoneoptimize.AddWhiteListActivity", //这是白名单, 不是自启动
                    "com.iqoo.secure",
                    "com.vivo.permissionmanager"
            ));

            put("Meizu", Arrays.asList(
                    "com.meizu.safe/.permission.SmartBGActivity",//Flyme7.3.0(7.1.2)
                    "com.meizu.safe/.permission.PermissionMainActivity",//网上的
                    "com.meizu.safe"
            ));

            put("OPPO", Arrays.asList(
                    "com.coloros.safecenter/.startupapp.StartupAppListActivity",
                    "com.coloros.safecenter/.permission.startup.StartupAppListActivity",
                    "com.oppo.safe/.permission.startup.StartupAppListActivity",
                    "com.coloros.oppoguardelf/com.coloros.powermanager.fuelgaue.PowerUsageModelActivity",
                    "com.coloros.safecenter/com.coloros.privacypermissionsentry.PermissionTopActivity",
                    "com.coloros.safecenter",
                    "com.oppo.safe",
                    "com.coloros.oppoguardelf"
            ));

            put("oneplus", Arrays.asList(
                    "com.oneplus.security/.chainlaunch.view.ChainLaunchAppListActivity",
                    "com.oneplus.security"
            ));

            put("letv", Arrays.asList(
                    "com.letv.android.letvsafe/.AutobootManageActivity",
                    "com.letv.android.letvsafe/.BackgroundAppManageActivity",//应用保护
                    "com.letv.android.letvsafe"
            ));

            put("zte", Arrays.asList(
                    "com.zte.heartyservice/.autorun.AppAutoRunManager",
                    "com.zte.heartyservice"
            ));

            //金立
            put("F", Arrays.asList(
                    "com.gionee.softmanager/.MainActivity",
                    "com.gionee.softmanager"
            ));

            //以下为未确定(厂商名也不确定)
            put("smartisanos", Arrays.asList(
                    "com.smartisanos.security/.invokeHistory.InvokeHistoryActivity",
                    "com.smartisanos.security"
            ));

            //360
            put("360", Arrays.asList(
                    "com.yulong.android.coolsafe/.ui.activity.autorun.AutoRunListActivity",
                    "com.yulong.android.coolsafe"
            ));

            //360
            put("ulong", Arrays.asList(
                    "com.yulong.android.coolsafe/.ui.activity.autorun.AutoRunListActivity",
                    "com.yulong.android.coolsafe"
            ));

            //酷派
            put("coolpad"/*厂商名称不确定是否正确*/, Arrays.asList(
                    "com.yulong.android.security/com.yulong.android.seccenter.tabbarmain",
                    "com.yulong.android.security"
            ));

            //联想
            put("lenovo"/*厂商名称不确定是否正确*/, Arrays.asList(
                    "com.lenovo.security/.purebackground.PureBackgroundActivity",
                    "com.lenovo.security"
            ));

            put("htc"/*厂商名称不确定是否正确*/, Arrays.asList(
                    "com.htc.pitroad/.landingpage.activity.LandingPageActivity",
                    "com.htc.pitroad"
            ));

            //华硕
            put("asus"/*厂商名称不确定是否正确*/, Arrays.asList(
                    "com.asus.mobilemanager/.MainActivity",
                    "com.asus.mobilemanager"
            ));

        }
    };

    //跳转自启动页面
    public static void startToAutoStartSetting(Context context) {
        Log.e("Util", "******************当前手机型号为：" + Build.MANUFACTURER);

        Set<Map.Entry<String, List<String>>> entries = hashMap.entrySet();
        boolean has = false;
        for (Map.Entry<String, List<String>> entry : entries) {
            String manufacturer = entry.getKey();
            List<String> actCompatList = entry.getValue();
            if (Build.MANUFACTURER.equalsIgnoreCase(manufacturer)) {
                for (String act : actCompatList) {
                    try {
                        Intent intent;
                        if (act.contains("/")) {
                            intent = new Intent();
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            ComponentName componentName = ComponentName.unflattenFromString(act);
                            intent.setComponent(componentName);
                        } else {
                            //找不到? 网上的做法都是跳转到设置... 这基本上是没意义的 基本上自启动这个功能是第三方厂商自己写的安全管家类app
                            //所以我是直接跳转到对应的安全管家/安全中心
                            intent = context.getPackageManager().getLaunchIntentForPackage(act);
                        }
                        context.startActivity(intent);
                        has = true;
                        break;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        if (!has) {
            Toast.makeText(context, "兼容方案", Toast.LENGTH_SHORT).show();
            try {
                Intent intent = new Intent();
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setAction("android.settings.APPLICATION_DETAILS_SETTINGS");
                intent.setData(Uri.fromParts("package", context.getPackageName(), null));
                context.startActivity(intent);
            } catch (Exception e) {
                e.printStackTrace();
                Intent intent = new Intent(Settings.ACTION_SETTINGS);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            }
        }
    }

    //电池优化设置
    @RequiresApi(api = Build.VERSION_CODES.M)
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    public void batterySetting(Switch switch_battery_setting) {
        isIgnoreBatteryOptimization = KeepAliveUtils.isIgnoreBatteryOptimization(this);
        switch_battery_setting.setChecked(isIgnoreBatteryOptimization);

        switch_battery_setting.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked && !isIgnoreBatteryOptimization) {
                KeepAliveUtils.ignoreBatteryOptimization(this);
            } else if (isChecked) {
                Toast.makeText(this, R.string.isIgnored, Toast.LENGTH_SHORT).show();
                switch_battery_setting.setChecked(isIgnoreBatteryOptimization);
            } else {
                Toast.makeText(this, R.string.isIgnored2, Toast.LENGTH_SHORT).show();
                switch_battery_setting.setChecked(isIgnoreBatteryOptimization);
            }
        });
    }

    //不在最近任务列表中显示
    @SuppressLint("ObsoleteSdkInt,UseSwitchCompatOrMaterialCode")
    private void switchExcludeFromRecents(Switch switch_exclude_from_recents) {
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

    //是否开启失败重试
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private void switchRetryDelay(Switch switch_retry_delay) {
        switch_retry_delay.setChecked(SettingUtil.getSwitchRetryDelay());

        switch_retry_delay.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked
                    && SettingUtil.getRetryDelayTime(1) == 0
                    && SettingUtil.getRetryDelayTime(2) == 0
                    && SettingUtil.getRetryDelayTime(3) == 0
                    && SettingUtil.getRetryDelayTime(4) == 0
                    && SettingUtil.getRetryDelayTime(5) == 0) {
                Toast.makeText(context, "所有间隔时间都为0，无法启用", Toast.LENGTH_SHORT).show();
                SettingUtil.switchRetryDelay(false);
                return;
            }
            SettingUtil.switchRetryDelay(isChecked);
            Log.d(TAG, "switchRetryDelay:" + isChecked);
        });
    }

    //接口请求失败重试时间间隔
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

                if (SettingUtil.getRetryDelayTime(1) == 0
                        && SettingUtil.getRetryDelayTime(2) == 0
                        && SettingUtil.getRetryDelayTime(3) == 0
                        && SettingUtil.getRetryDelayTime(4) == 0
                        && SettingUtil.getRetryDelayTime(5) == 0) {
                    Toast.makeText(context, "所有间隔时间都为0，自动禁用失败重试", Toast.LENGTH_SHORT).show();
                    SettingUtil.switchRetryDelay(false);
                }
            }
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

    //设置转发时启用自定义模版
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private void switchSmsTemplate(Switch switch_sms_template) {
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
                CommonUtil.insertOrReplaceText2Cursor(textSmsTemplate, "{{来源号码}}");
                return;
            case R.id.bt_insert_content:
                CommonUtil.insertOrReplaceText2Cursor(textSmsTemplate, "{{短信内容}}");
                return;
            case R.id.bt_insert_extra:
                CommonUtil.insertOrReplaceText2Cursor(textSmsTemplate, "{{卡槽信息}}");
                return;
            case R.id.bt_insert_time:
                CommonUtil.insertOrReplaceText2Cursor(textSmsTemplate, "{{接收时间}}");
                return;
            case R.id.bt_insert_device_name:
                CommonUtil.insertOrReplaceText2Cursor(textSmsTemplate, "{{设备名称}}");
                return;
            default:
        }
    }

    //页面帮助提示
    private void SwitchHelpTip(@SuppressLint("UseSwitchCompatOrMaterialCode") Switch switchHelpTip) {
        switchHelpTip.setChecked(MyApplication.showHelpTip);

        switchHelpTip.setOnCheckedChangeListener((buttonView, isChecked) -> {
            MyApplication.showHelpTip = isChecked;
            SharedPreferences sp = context.getSharedPreferences(Define.SP_CONFIG, Context.MODE_PRIVATE);
            sp.edit().putBoolean(Define.SP_CONFIG_SWITCH_HELP_TIP, isChecked).apply();
            Log.d(TAG, "onCheckedChanged:" + isChecked);

            TextView help_tip = findViewById(R.id.help_tip);
            ScrollView scrollView = findViewById(R.id.scrollView);
            CommonUtil.calcMarginBottom(this, help_tip, null, null, scrollView);
        });
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

}
