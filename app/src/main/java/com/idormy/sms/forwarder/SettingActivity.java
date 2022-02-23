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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.hjq.permissions.OnPermissionCallback;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;
import com.hjq.toast.ToastUtils;
import com.idormy.sms.forwarder.receiver.RebootBroadcastReceiver;
import com.idormy.sms.forwarder.sender.HttpServer;
import com.idormy.sms.forwarder.sender.SenderUtil;
import com.idormy.sms.forwarder.sender.SmsHubApiTask;
import com.idormy.sms.forwarder.service.MusicService;
import com.idormy.sms.forwarder.utils.CommonUtil;
import com.idormy.sms.forwarder.utils.DbHelper;
import com.idormy.sms.forwarder.utils.Define;
import com.idormy.sms.forwarder.utils.HttpUtil;
import com.idormy.sms.forwarder.utils.KeepAliveUtils;
import com.idormy.sms.forwarder.utils.LogUtil;
import com.idormy.sms.forwarder.utils.OnePixelManager;
import com.idormy.sms.forwarder.utils.RuleUtil;
import com.idormy.sms.forwarder.utils.SettingUtil;
import com.idormy.sms.forwarder.view.ClearEditText;
import com.idormy.sms.forwarder.view.StepBar;

import java.lang.reflect.Method;
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
        ScrollView scrollView = findViewById(R.id.scrollView);
        CommonUtil.calcMarginBottom(this, null, null, scrollView);

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
        editBatteryLevelAlarm(findViewById(R.id.et_battery_level_alarm_min), findViewById(R.id.et_battery_level_alarm_max), findViewById(R.id.cb_battery_level_alarm_once));

        //开机启动
        checkWithReboot(findViewById(R.id.switch_with_reboot), findViewById(R.id.tv_auto_startup));
        //电池优化设置
        batterySetting(findViewById(R.id.layout_battery_setting), findViewById(R.id.switch_battery_setting));
        //不在最近任务列表中显示
        switchExcludeFromRecents(findViewById(R.id.switch_exclude_from_recents));
        //后台播放无声音乐
        switchPlaySilenceMusic(findViewById(R.id.switch_play_silence_music));
        //1像素透明Activity保活
        switchOnePixelActivity(findViewById(R.id.switch_one_pixel_activity));
        //接口请求失败重试时间间隔
        editRetryDelayTime(findViewById(R.id.et_retry_times), findViewById(R.id.et_delay_time));

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
        StepBar stepBar = findViewById(R.id.stepBar);
        stepBar.setHighlight();
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
            Log.d(TAG, "switchEnableSms:" + isChecked);
            if (isChecked) {
                //检查权限是否获取
                PackageManager pm = getPackageManager();
                CommonUtil.CheckPermission(pm, this);
                XXPermissions.with(this)
                        // 接收短信
                        .permission(Permission.RECEIVE_SMS)
                        // 发送短信
                        //.permission(Permission.SEND_SMS)
                        // 读取短信
                        .permission(Permission.READ_SMS)
                        .request(new OnPermissionCallback() {

                            @Override
                            public void onGranted(List<String> permissions, boolean all) {
                                if (all) {
                                    ToastUtils.show(R.string.toast_granted_all);
                                } else {
                                    ToastUtils.show(R.string.toast_granted_part);
                                }
                                SettingUtil.switchEnableSms(true);
                            }

                            @Override
                            public void onDenied(List<String> permissions, boolean never) {
                                if (never) {
                                    ToastUtils.show(R.string.toast_denied_never);
                                    // 如果是被永久拒绝就跳转到应用权限系统设置页面
                                    XXPermissions.startPermissionActivity(SettingActivity.this, permissions);
                                } else {
                                    ToastUtils.show(R.string.toast_denied);
                                }
                                SettingUtil.switchEnableSms(false);
                            }
                        });
            } else {
                SettingUtil.switchEnableSms(false);
            }
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
                ToastUtils.show(R.string.enable_phone_fw_tips);
                SettingUtil.switchEnablePhone(false);
                return;
            }

            Log.d(TAG, "switchEnablePhone:" + isChecked);
            if (isChecked) {
                //检查权限是否获取
                PackageManager pm = getPackageManager();
                CommonUtil.CheckPermission(pm, this);
                XXPermissions.with(this)
                        // 读取电话状态
                        .permission(Permission.READ_PHONE_STATE)
                        // 读取手机号码
                        .permission(Permission.READ_PHONE_NUMBERS)
                        // 读取通话记录
                        .permission(Permission.READ_CALL_LOG)
                        // 读取联系人
                        .permission(Permission.READ_CONTACTS)
                        .request(new OnPermissionCallback() {

                            @Override
                            public void onGranted(List<String> permissions, boolean all) {
                                if (all) {
                                    ToastUtils.show(R.string.toast_granted_all);
                                } else {
                                    ToastUtils.show(R.string.toast_granted_part);
                                }
                                SettingUtil.switchEnablePhone(true);
                            }

                            @Override
                            public void onDenied(List<String> permissions, boolean never) {
                                if (never) {
                                    ToastUtils.show(R.string.toast_denied_never);
                                    // 如果是被永久拒绝就跳转到应用权限系统设置页面
                                    XXPermissions.startPermissionActivity(SettingActivity.this, permissions);
                                } else {
                                    ToastUtils.show(R.string.toast_denied);
                                }
                                SettingUtil.switchEnablePhone(false);
                            }
                        });
            } else {
                SettingUtil.switchEnablePhone(false);
            }
        });

        check_box_call_type_1.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SettingUtil.switchCallType1(isChecked);
            if (!isChecked && !SettingUtil.getSwitchCallType1() && !SettingUtil.getSwitchCallType2() && !SettingUtil.getSwitchCallType3()) {
                ToastUtils.show(R.string.enable_phone_fw_tips);
                SettingUtil.switchEnablePhone(false);
            }
        });

        check_box_call_type_2.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SettingUtil.switchCallType2(isChecked);
            if (!isChecked && !SettingUtil.getSwitchCallType1() && !SettingUtil.getSwitchCallType2() && !SettingUtil.getSwitchCallType3()) {
                ToastUtils.show(R.string.enable_phone_fw_tips);
                SettingUtil.switchEnablePhone(false);
            }
        });

        check_box_call_type_3.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SettingUtil.switchCallType3(isChecked);
            if (!isChecked && !SettingUtil.getSwitchCallType1() && !SettingUtil.getSwitchCallType2() && !SettingUtil.getSwitchCallType3()) {
                ToastUtils.show(R.string.enable_phone_fw_tips);
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
                    ToastUtils.delayedShow(R.string.tips_notification_listener, 3000);
                    return;
                } else {
                    ToastUtils.delayedShow(R.string.notification_service_is_on, 3000);
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
            ToastUtils.show(R.string.notification_listener_service_enabled);
            CommonUtil.toggleNotificationListenerService(this);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CommonUtil.NOTIFICATION_REQUEST_CODE) {
            if (CommonUtil.isNotificationListenerServiceEnabled(this)) {
                ToastUtils.show(R.string.notification_listener_service_enabled);
                CommonUtil.toggleNotificationListenerService(this);
                SettingUtil.switchEnableAppNotify(true);
            } else {
                ToastUtils.show(R.string.notification_listener_service_disabled);
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
                HttpUtil.Toast(TAG, getString(R.string.invalid_webserver));
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

    //设置低电量报警
    private void editBatteryLevelAlarm(final EditText et_battery_level_alarm_min, final EditText et_battery_level_alarm_max, CheckBox cb_battery_level_alarm_once) {
        et_battery_level_alarm_min.setText(String.valueOf(SettingUtil.getBatteryLevelAlarmMin()));
        et_battery_level_alarm_min.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String batteryLevel = et_battery_level_alarm_min.getText().toString().trim();
                if (!batteryLevel.isEmpty()) {
                    SettingUtil.setBatteryLevelAlarmMin(Integer.parseInt(batteryLevel));
                } else {
                    SettingUtil.setBatteryLevelAlarmMin(0);
                }
            }
        });

        et_battery_level_alarm_max.setText(String.valueOf(SettingUtil.getBatteryLevelAlarmMax()));
        et_battery_level_alarm_max.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String batteryLevel = et_battery_level_alarm_max.getText().toString().trim();
                if (!batteryLevel.isEmpty()) {
                    SettingUtil.setBatteryLevelAlarmMax(Integer.parseInt(batteryLevel));
                } else {
                    SettingUtil.setBatteryLevelAlarmMax(0);
                }
            }
        });

        cb_battery_level_alarm_once.setChecked(SettingUtil.getBatteryLevelAlarmOnce());
        cb_battery_level_alarm_once.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SettingUtil.switchBatteryLevelAlarmOnce(isChecked);
            if (isChecked && 0 == SettingUtil.getBatteryLevelAlarmMin() && 0 == SettingUtil.getBatteryLevelAlarmMax()) {
                ToastUtils.show(R.string.tips_battery_level_alarm_once);
                SettingUtil.switchEnablePhone(false);
            }
        });
    }

    //开机启动
    private void checkWithReboot(@SuppressLint("UseSwitchCompatOrMaterialCode") Switch withrebootSwitch, TextView tvAutoStartup) {
        tvAutoStartup.setText(getAutoStartTips());

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
        Log.e("Util", "******************The current phone model is:" + Build.MANUFACTURER);

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
            ToastUtils.show(R.string.tips_compatible_solution);
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
    public void batterySetting(LinearLayout layout_battery_setting, Switch switch_battery_setting) {
        //安卓6.0以下没有忽略电池优化
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M) {
            layout_battery_setting.setVisibility(View.GONE);
            return;
        }

        isIgnoreBatteryOptimization = KeepAliveUtils.isIgnoreBatteryOptimization(this);
        switch_battery_setting.setChecked(isIgnoreBatteryOptimization);

        switch_battery_setting.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked && !isIgnoreBatteryOptimization) {
                KeepAliveUtils.ignoreBatteryOptimization(this);
            } else if (isChecked) {
                ToastUtils.show(R.string.isIgnored);
                switch_battery_setting.setChecked(isIgnoreBatteryOptimization);
            } else {
                ToastUtils.show(R.string.isIgnored2);
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

    //后台播放无声音乐
    @SuppressLint("ObsoleteSdkInt,UseSwitchCompatOrMaterialCode")
    private void switchPlaySilenceMusic(Switch switch_play_silence_music) {
        switch_play_silence_music.setChecked(SettingUtil.getPlaySilenceMusic());

        switch_play_silence_music.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SettingUtil.switchPlaySilenceMusic(isChecked);

            if (isChecked) {
                startService(new Intent(context, MusicService.class));
            } else {
                stopService(new Intent(context, MusicService.class));
            }
            Log.d(TAG, "onCheckedChanged:" + isChecked);
        });
    }

    //1像素透明Activity保活
    @SuppressLint("ObsoleteSdkInt,UseSwitchCompatOrMaterialCode")
    private void switchOnePixelActivity(Switch switch_one_pixel_activity) {
        switch_one_pixel_activity.setChecked(SettingUtil.getOnePixelActivity());

        switch_one_pixel_activity.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SettingUtil.switchOnePixelActivity(isChecked);

            OnePixelManager onePixelManager = new OnePixelManager();
            if (isChecked) {
                onePixelManager.registerOnePixelReceiver(this);//注册广播接收者
            } else {
                onePixelManager.unregisterOnePixelReceiver(this);
            }
            Log.d(TAG, "onCheckedChanged:" + isChecked);
        });
    }

    //接口请求失败重试时间间隔
    private void editRetryDelayTime(final EditText et_retry_times, final EditText et_delay_time) {
        et_retry_times.setText(String.valueOf(SettingUtil.getRetryTimes()));
        et_retry_times.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String retryTimes = et_retry_times.getText().toString().trim();
                if (!retryTimes.isEmpty()) {
                    SettingUtil.setRetryTimes(Integer.parseInt(retryTimes));
                } else {
                    SettingUtil.setRetryTimes(0);
                }
            }
        });

        et_delay_time.setText(String.valueOf(SettingUtil.getDelayTime()));
        et_delay_time.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String delayTime = et_delay_time.getText().toString().trim();
                if (!delayTime.isEmpty()) {
                    SettingUtil.setDelayTime(Integer.parseInt(delayTime));
                } else {
                    SettingUtil.setDelayTime(1);
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
    @SuppressLint({"UseSwitchCompatOrMaterialCode", "SetTextI18n"})
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
                textSmsTemplate.setText(getString(R.string.tag_from) + "\n" +
                        getString(R.string.tag_sms) + "\n" +
                        getString(R.string.tag_card_slot) + "\n" +
                        getString(R.string.tag_receive_time) + "\n" +
                        getString(R.string.tag_device_name));
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
                CommonUtil.insertOrReplaceText2Cursor(textSmsTemplate, getString(R.string.tag_from));
                return;
            case R.id.bt_insert_content:
                CommonUtil.insertOrReplaceText2Cursor(textSmsTemplate, getString(R.string.tag_sms));
                return;
            case R.id.bt_insert_extra:
                CommonUtil.insertOrReplaceText2Cursor(textSmsTemplate, getString(R.string.tag_card_slot));
                return;
            case R.id.bt_insert_time:
                CommonUtil.insertOrReplaceText2Cursor(textSmsTemplate, getString(R.string.tag_receive_time));
                return;
            case R.id.bt_insert_device_name:
                CommonUtil.insertOrReplaceText2Cursor(textSmsTemplate, getString(R.string.tag_device_name));
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

            StepBar stepBar = findViewById(R.id.stepBar);
            stepBar.setHighlight();
            CommonUtil.calcMarginBottom(this, null, null, findViewById(R.id.scrollView));
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

    //启用menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    //menu点击事件
    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {
            case R.id.to_app_list:
                intent = new Intent(this, AppListActivity.class);
                break;
            case R.id.to_clone:
                intent = new Intent(this, CloneActivity.class);
                break;
            case R.id.to_about:
                intent = new Intent(this, AboutActivity.class);
                break;
            case R.id.to_help:
                Uri uri = Uri.parse("https://gitee.com/pp/SmsForwarder/wikis/pages");
                intent = new Intent(Intent.ACTION_VIEW, uri);
                break;
            default:
                return super.onOptionsItemSelected(item);
        }

        startActivity(intent);
        return true;
    }

    //设置menu图标显示
    @Override
    public boolean onMenuOpened(int featureId, Menu menu) {
        Log.d(TAG, "onMenuOpened, featureId=" + featureId);
        if (menu != null) {
            if (menu.getClass().getSimpleName().equals("MenuBuilder")) {
                try {
                    Method m = menu.getClass().getDeclaredMethod("setOptionalIconsVisible", Boolean.TYPE);
                    m.setAccessible(true);
                    m.invoke(menu, true);
                } catch (NoSuchMethodException e) {
                    Log.e(TAG, "onMenuOpened", e);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return super.onMenuOpened(featureId, menu);
    }

    /**
     * 获取当前手机品牌
     *
     * @return 手机品牌
     */
    public static String getAutoStartTips() {
        String brand = Build.BRAND.toLowerCase();
        String tips;

        switch (brand) {
            case "huawei":
                tips = "华为手机：应用启动管理 -> 关闭应用开关 -> 打开允许自启动";
                break;
            case "honor":
                tips = "荣耀手机：应用启动管理 -> 关闭应用开关 -> 打开允许自启动";
                break;
            case "xiaomi":
                tips = "小米手机：授权管理 -> 自启动管理 -> 允许应用自启动";
                break;
            case "oppo":
                tips = "OPPO手机：权限隐私 -> 自启动管理 -> 允许应用自启动";
                break;
            case "vivo":
                tips = "vivo手机：权限管理 -> 自启动 -> 允许应用自启动";
                break;
            case "meizu":
                tips = "魅族手机：权限管理 -> 后台管理 -> 点击应用 -> 允许后台运行";
                break;
            case "samsung":
                tips = "三星手机：自动运行应用程序 -> 打开应用开关 -> 电池管理 -> 未监视的应用程序 -> 添加应用";
                break;
            case "letv":
                tips = "乐视手机：自启动管理 -> 允许应用自启动";
                break;
            case "smartisan":
                tips = "锤子手机：权限管理 -> 自启动权限管理 -> 点击应用 -> 允许被系统启动";
                break;
            default:
                tips = "未知手机品牌：需要自主查看设置操作";
                break;
        }

        return tips;
    }
}
