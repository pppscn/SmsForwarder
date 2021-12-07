package com.idormy.sms.forwarder;

import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import com.idormy.sms.forwarder.sender.SendHistory;
import com.idormy.sms.forwarder.service.BatteryService;
import com.idormy.sms.forwarder.service.FrontService;
import com.idormy.sms.forwarder.utils.CommonUtil;
import com.idormy.sms.forwarder.utils.Define;
import com.idormy.sms.forwarder.utils.PhoneUtils;
import com.idormy.sms.forwarder.utils.SettingUtil;
import com.smailnet.emailkit.EmailKit;
import com.umeng.analytics.MobclickAgent;
import com.umeng.commonsdk.UMConfigure;

import java.util.ArrayList;
import java.util.List;

public class MyApplication extends Application {
    private static final String TAG = "MyApplication";
    //SIM卡信息
    public static List<PhoneUtils.SimInfo> SimInfoList = new ArrayList<>();
    //是否关闭页面提示
    public static boolean showHelpTip = true;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");
        super.onCreate();

        try {
            //初始化组件化基础库, 所有友盟业务SDK都必须调用此初始化接口。
            //建议在宿主App的Application.onCreate函数中调用基础组件库初始化函数。
            UMConfigure.init(this, "60254fc7425ec25f10f4293e", CommonUtil.getChannelName(this), UMConfigure.DEVICE_TYPE_PHONE, "");
            // 选用LEGACY_AUTO页面采集模式
            MobclickAgent.setPageCollectionMode(MobclickAgent.PageMode.LEGACY_MANUAL);
            //pro close log
            UMConfigure.setLogEnabled(true);

            //前台服务
            Intent intent = new Intent(this, FrontService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent);
            } else {
                startService(intent);
            }

            SendHistory.init(this);
            SettingUtil.init(this);

            EmailKit.initialize(this);

            SharedPreferences sp = MyApplication.this.getSharedPreferences(Define.SP_CONFIG, Context.MODE_PRIVATE);
            showHelpTip = sp.getBoolean(Define.SP_CONFIG_SWITCH_HELP_TIP, true);

            if (SettingUtil.getExcludeFromRecents()) {
                ActivityManager am = (ActivityManager) this.getSystemService(Context.ACTIVITY_SERVICE);
                if (am != null) {
                    List<ActivityManager.AppTask> appTasks = am.getAppTasks();
                    if (appTasks != null && !appTasks.isEmpty()) {
                        appTasks.get(0).setExcludeFromRecents(true);
                    }
                }
            }

            //电池状态监听
            Intent batteryServiceIntent = new Intent(this, BatteryService.class);
            startService(batteryServiceIntent);
        } catch (Exception e) {
            Log.e(TAG, "onCreate:", e);
        }
    }
}
