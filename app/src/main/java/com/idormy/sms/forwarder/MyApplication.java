package com.idormy.sms.forwarder;

import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import com.idormy.sms.forwarder.receiver.SimStateReceiver;
import com.idormy.sms.forwarder.sender.SendHistory;
import com.idormy.sms.forwarder.service.BatteryService;
import com.idormy.sms.forwarder.service.FrontService;
import com.idormy.sms.forwarder.utils.Define;
import com.idormy.sms.forwarder.utils.PhoneUtils;
import com.idormy.sms.forwarder.utils.SettingUtil;
import com.idormy.sms.forwarder.utils.SharedPreferencesHelper;
import com.smailnet.emailkit.EmailKit;
import com.umeng.commonsdk.UMConfigure;

import java.util.ArrayList;
import java.util.List;

public class MyApplication extends Application {
    private static final String TAG = "MyApplication";
    //SIM卡信息
    public static List<PhoneUtils.SimInfo> SimInfoList = new ArrayList<>();
    //是否关闭页面提示
    public static boolean showHelpTip = true;
    SharedPreferencesHelper sharedPreferencesHelper;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");
        super.onCreate();

        try {
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

            //SIM卡插拔状态广播监听
            IntentFilter simStateFilter = new IntentFilter(SimStateReceiver.ACTION_SIM_STATE_CHANGED);
            registerReceiver(new SimStateReceiver(), simStateFilter);

            //友盟统计
            sharedPreferencesHelper = new SharedPreferencesHelper(this, "umeng");
            //设置LOG开关，默认为false
            //UMConfigure.setLogEnabled(true);
            //友盟预初始化
            UMConfigure.preInit(getApplicationContext(), "60254fc7425ec25f10f4293e", "Umeng");

            //判断是否同意隐私协议，uminit为1时为已经同意，直接初始化umsdk
            if (sharedPreferencesHelper.getSharedPreference("uminit", "").equals("1")) {
                //友盟正式初始化
                UmInitConfig umInitConfig = new UmInitConfig();
                umInitConfig.UMinit(getApplicationContext());
            }
        } catch (Exception e) {
            Log.e(TAG, "onCreate:", e);
        }
    }
}
