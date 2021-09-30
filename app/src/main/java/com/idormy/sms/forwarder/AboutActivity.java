package com.idormy.sms.forwarder;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.idormy.sms.forwarder.receiver.RebootBroadcastReceiver;
import com.idormy.sms.forwarder.utils.CacheUtil;
import com.idormy.sms.forwarder.utils.Define;
import com.idormy.sms.forwarder.utils.aUtil;
import com.xuexiang.xupdate.easy.EasyUpdate;
import com.xuexiang.xupdate.proxy.impl.DefaultUpdateChecker;


@SuppressWarnings("SpellCheckingInspection")
public class AboutActivity extends AppCompatActivity {
    private final String TAG = "com.idormy.sms.forwarder.AboutActivity";
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
            version_now.setText(aUtil.getVersionName(AboutActivity.this));
        } catch (Exception e) {
            e.printStackTrace();
        }

        check_version_now.setOnClickListener(v -> {
            try {
                String updateUrl = "https://xupdate.bms.ink/update/checkVersion?appKey=com.idormy.sms.forwarder&versionCode=";
                updateUrl += aUtil.getVersionCode(AboutActivity.this);

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


}
