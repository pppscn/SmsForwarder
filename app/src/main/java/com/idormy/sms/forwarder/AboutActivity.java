package com.idormy.sms.forwarder;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.idormy.sms.forwarder.receiver.RebootBroadcastReceiver;
import com.idormy.sms.forwarder.utils.CacheUtil;
import com.idormy.sms.forwarder.utils.CommonUtil;
import com.idormy.sms.forwarder.utils.Define;
import com.xuexiang.xupdate.easy.EasyUpdate;
import com.xuexiang.xupdate.proxy.impl.DefaultUpdateChecker;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class AboutActivity extends AppCompatActivity {
    private final String TAG = "AboutActivity";
    private Context context;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);

        context = AboutActivity.this;

        setContentView(R.layout.activity_about);
        Log.d(TAG, "onCreate: " + RebootBroadcastReceiver.class.getName());

        @SuppressLint("UseSwitchCompatOrMaterialCode") Switch check_with_reboot = findViewById(R.id.switch_with_reboot);
        checkWithReboot(check_with_reboot);

        @SuppressLint("UseSwitchCompatOrMaterialCode") Switch switch_help_tip = findViewById(R.id.switch_help_tip);
        SwitchHelpTip(switch_help_tip);

        final TextView version_now = findViewById(R.id.version_now);
        Button check_version_now = findViewById(R.id.check_version_now);
        try {
            version_now.setText(CommonUtil.getVersionName(AboutActivity.this));
        } catch (Exception e) {
            e.printStackTrace();
        }

        check_version_now.setOnClickListener(v -> {
            try {
                String updateUrl = "https://xupdate.bms.ink/update/checkVersion?appKey=com.idormy.sms.forwarder&versionCode=";
                updateUrl += CommonUtil.getVersionCode(AboutActivity.this);

                EasyUpdate.create(AboutActivity.this, updateUrl)
                        .updateChecker(new DefaultUpdateChecker() {
                            @Override
                            public void onBeforeCheck() {
                                super.onBeforeCheck();
                                Toast.makeText(AboutActivity.this, R.string.checking, Toast.LENGTH_LONG).show();
                            }

                            @Override
                            public void noNewVersion(Throwable throwable) {
                                super.noNewVersion(throwable);
                                // 没有最新版本的处理
                                Toast.makeText(AboutActivity.this, R.string.up_to_date, Toast.LENGTH_LONG).show();
                            }
                        })
                        .update();
            } catch (Exception e) {
                e.printStackTrace();
            }

        });

        final TextView cache_size = findViewById(R.id.cache_size);
        try {
            cache_size.setText(CacheUtil.getTotalCacheSize(AboutActivity.this));
        } catch (Exception e) {
            e.printStackTrace();
        }
        Button clear_all_cache = findViewById(R.id.clear_all_cache);
        clear_all_cache.setOnClickListener(v -> {
            CacheUtil.clearAllCache(AboutActivity.this);
            try {
                cache_size.setText(CacheUtil.getTotalCacheSize(AboutActivity.this));
            } catch (Exception e) {
                e.printStackTrace();
            }
            Toast.makeText(AboutActivity.this, R.string.cache_purged, Toast.LENGTH_LONG).show();
        });

        Button join_qq_group1 = findViewById(R.id.join_qq_group1);
        join_qq_group1.setOnClickListener(v -> {
            String key = "Mj5m39bqy6eodOImrFLI19Tdeqvv-9zf";
            joinQQGroup(key);
        });

        Button join_qq_group2 = findViewById(R.id.join_qq_group2);
        join_qq_group2.setOnClickListener(v -> {
            String key = "jPXy4YaUzA7Uo0yPPbZXdkb66NS1smU_";
            joinQQGroup(key);
        });

    }

    //检查重启广播接受器状态并设置
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

    //页面帮助提示
    private void SwitchHelpTip(@SuppressLint("UseSwitchCompatOrMaterialCode") Switch switchHelpTip) {
        switchHelpTip.setChecked(MyApplication.showHelpTip);

        switchHelpTip.setOnCheckedChangeListener((buttonView, isChecked) -> {
            MyApplication.showHelpTip = isChecked;
            SharedPreferences sp = context.getSharedPreferences(Define.SP_CONFIG, Context.MODE_PRIVATE);
            sp.edit().putBoolean(Define.SP_CONFIG_SWITCH_HELP_TIP, isChecked).apply();
            Log.d(TAG, "onCheckedChanged:" + isChecked);
        });
    }

    //发起添加群流程
    public void joinQQGroup(String key) {
        Intent intent = new Intent();
        intent.setData(Uri.parse("mqqopensdkapi://bizAgent/qm/qr?url=http%3A%2F%2Fqm.qq.com%2Fcgi-bin%2Fqm%2Fqr%3Ffrom%3Dapp%26p%3Dandroid%26jump_from%3Dwebapi%26k%3D" + key));
        // 此Flag可根据具体产品需要自定义，如设置，则在加群界面按返回，返回手Q主界面，不设置，按返回会返回到呼起产品界面
        //intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            startActivity(intent);
        } catch (Exception e) {
            // 未安装手Q或安装的版本不支持
            Toast.makeText(AboutActivity.this, R.string.unknown_qq_version, Toast.LENGTH_LONG).show();
        }
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
}
