package com.idormy.sms.forwarder;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.idormy.sms.forwarder.BroadCastReceiver.RebootBroadcastReceiver;
import com.idormy.sms.forwarder.utils.CacheUtil;
import com.idormy.sms.forwarder.utils.Define;
import com.idormy.sms.forwarder.utils.aUtil;
import com.xuexiang.xupdate.easy.EasyUpdate;
import com.xuexiang.xupdate.proxy.impl.DefaultUpdateChecker;


public class SettingActivity extends AppCompatActivity {
    private String TAG = "SettingActivity";
    private Context context;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "oncreate");
        super.onCreate(savedInstanceState);

        context = SettingActivity.this;

        setContentView(R.layout.activity_setting);
        Log.d(TAG, "onCreate: " + RebootBroadcastReceiver.class.getName());

        Switch check_with_reboot = (Switch) findViewById(R.id.switch_with_reboot);
        checkWithReboot(check_with_reboot);

        Switch switch_help_tip = (Switch) findViewById(R.id.switch_help_tip);
        SwitchHelpTip(switch_help_tip);

        final TextView version_now = (TextView) findViewById(R.id.version_now);
        Button check_version_now = (Button) findViewById(R.id.check_version_now);
        try {
            version_now.setText(aUtil.getVersionName(SettingActivity.this));
        } catch (Exception e) {
            e.printStackTrace();
        }

        check_version_now.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //checkNewVersion();
                try {
                    String updateUrl = "https://xupdate.bms.ink/update/checkVersion?appKey=com.idormy.sms.forwarder&versionCode=";
                    updateUrl += aUtil.getVersionCode(SettingActivity.this);

                    EasyUpdate.create(SettingActivity.this, updateUrl)
                            .updateChecker(new DefaultUpdateChecker() {
                                @Override
                                public void onBeforeCheck() {
                                    super.onBeforeCheck();
                                    Toast.makeText(SettingActivity.this, "查询中...", Toast.LENGTH_LONG).show();
                                }

                                @Override
                                public void onAfterCheck() {
                                    super.onAfterCheck();
                                }

                                @Override
                                public void noNewVersion(Throwable throwable) {
                                    super.noNewVersion(throwable);
                                    // 没有最新版本的处理
                                    Toast.makeText(SettingActivity.this, "已是最新版本！", Toast.LENGTH_LONG).show();
                                }
                            })
                            .update();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        final TextView cache_size = (TextView) findViewById(R.id.cache_size);
        try {
            cache_size.setText(CacheUtil.getTotalCacheSize(SettingActivity.this));
        } catch (Exception e) {
            e.printStackTrace();
        }
        Button clear_all_cache = (Button) findViewById(R.id.clear_all_cache);
        clear_all_cache.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CacheUtil.clearAllCache(SettingActivity.this);
                try {
                    cache_size.setText(CacheUtil.getTotalCacheSize(SettingActivity.this));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Toast.makeText(SettingActivity.this, "缓存清理完成", Toast.LENGTH_LONG).show();
            }
        });

        Button join_qq_group = (Button) findViewById(R.id.join_qq_group);
        join_qq_group.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String key = "HvroJRfvK7GGfnQgaIQ4Rh1un9O83N7M";
                joinQQGroup(key);
            }
        });

    }

    //检查重启广播接受器状态并设置
    private void checkWithReboot(Switch withrebootSwitch) {
        //获取组件
        final ComponentName cm = new ComponentName(this.getPackageName(), RebootBroadcastReceiver.class.getName());

        final PackageManager pm = getPackageManager();
        int state = pm.getComponentEnabledSetting(cm);
        if (state != PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                && state != PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER) {
            withrebootSwitch.setChecked(true);
        } else {
            withrebootSwitch.setChecked(false);
        }
        withrebootSwitch.setOnCheckedChangeListener(new Switch.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                int newState = (Boolean) isChecked ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                        : PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
                pm.setComponentEnabledSetting(cm, newState, PackageManager.DONT_KILL_APP);
                Log.d(TAG, "onCheckedChanged:" + isChecked);
            }
        });
    }

    //页面帮助提示
    private void SwitchHelpTip(Switch switchHelpTip) {
        switchHelpTip.setChecked(MyApplication.showHelpTip);

        switchHelpTip.setOnCheckedChangeListener(new Switch.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                MyApplication.showHelpTip = isChecked;
                SharedPreferences sp = context.getSharedPreferences(Define.SP_CONFIG, Context.MODE_PRIVATE);
                sp.edit().putBoolean(Define.SP_CONFIG_SWITCH_HELP_TIP, isChecked).apply();
                Log.d(TAG, "onCheckedChanged:" + isChecked);
            }
        });
    }

    //发起添加群流程
    public boolean joinQQGroup(String key) {
        Intent intent = new Intent();
        intent.setData(Uri.parse("mqqopensdkapi://bizAgent/qm/qr?url=http%3A%2F%2Fqm.qq.com%2Fcgi-bin%2Fqm%2Fqr%3Ffrom%3Dapp%26p%3Dandroid%26jump_from%3Dwebapi%26k%3D" + key));
        // 此Flag可根据具体产品需要自定义，如设置，则在加群界面按返回，返回手Q主界面，不设置，按返回会返回到呼起产品界面
        //intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            startActivity(intent);
            return true;
        } catch (Exception e) {
            // 未安装手Q或安装的版本不支持
            Toast.makeText(SettingActivity.this, "未安装手Q或安装的版本不支持！", Toast.LENGTH_LONG).show();
            return false;
        }
    }


}
