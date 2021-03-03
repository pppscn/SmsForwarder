package com.idormy.sms.forwarder;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.idormy.sms.forwarder.BroadCastReceiver.SmsForwarderBroadcastReceiver;
import com.idormy.sms.forwarder.adapter.LogAdapter;
import com.idormy.sms.forwarder.model.vo.LogVo;
import com.idormy.sms.forwarder.utils.LogUtil;
import com.idormy.sms.forwarder.utils.aUtil;
import com.umeng.analytics.MobclickAgent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements ReFlashListView.IReflashListener {

    private IntentFilter intentFilter;
    private SmsForwarderBroadcastReceiver smsBroadcastReceiver;
    private String TAG = "MainActivity";
    // logVoList用于存储数据
    private List<LogVo> logVos = new ArrayList<>();
    private LogAdapter adapter;
    private ReFlashListView listView;
    //SIM卡信息
    //private Map<String, Map> SimInfo = new HashMap();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "oncreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        LogUtil.init(this);
        // 先拿到数据并放在适配器上
        initTLogs(); //初始化数据
        showList(logVos);

        // 为ListView注册一个监听器，当用户点击了ListView中的任何一个子项时，就会回调onItemClick()方法
        // 在这个方法中可以通过position参数判断出用户点击的是那一个子项
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                LogVo logVo = logVos.get(position - 1);
                logDetail(logVo);
            }
        });

        //检查权限是否获取
        checkPermission();

        //获取本机号码(注意：这里获取的不一定是卡槽1的)
        TelephonyManager mTelephonyMgr = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        String Line1Number = mTelephonyMgr.getLine1Number();
        Log.d(TAG, "Line1Number: " + Line1Number);

        //获取SIM卡信息
        getSimInfo(Line1Number);
        //MyApplication appContext = ((MyApplication) getApplicationContext());
        //appContext.setSimInfo(SimInfo);
    }

    // 初始化数据
    private void initTLogs() {
        logVos = LogUtil.getLog(null, null);
    }

    private void showList(List<LogVo> logVosN) {
        Log.d(TAG, "showList: " + logVosN);
        if (adapter == null) {
            // 将适配器上的数据传递给listView
            listView = findViewById(R.id.list_view_log);
            listView.setInterface(this);
            adapter = new LogAdapter(MainActivity.this, R.layout.tlog_item, logVosN);

            listView.setAdapter(adapter);
        } else {
            adapter.onDateChange(logVosN);
        }
    }

    @Override
    public void onReflash() {
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {

            @Override
            public void run() {
                // TODO Auto-generated method stub
                //获取最新数据
                initTLogs();
                //通知界面显示
                showList(logVos);
                //通知listview 刷新数据完毕；
                listView.reflashComplete();
            }
        }, 2000);
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
        //取消注册广播
        unregisterReceiver(smsBroadcastReceiver);
    }

    public void logDetail(LogVo logVo) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("详情");
        String simInfo = logVo.getSimInfo();
        if (simInfo != null) {
            builder.setMessage(logVo.getFrom() + "\n\n" + logVo.getContent() + "\n\n" + logVo.getSimInfo() + "\n\n" + logVo.getRule() + "\n\n" + aUtil.utc2Local(logVo.getTime()));
        } else {
            builder.setMessage(logVo.getFrom() + "\n\n" + logVo.getContent() + "\n\n" + logVo.getRule() + "\n\n" + aUtil.utc2Local(logVo.getTime()));
        }
        builder.show();
    }

    public void toSetting() {
        Intent intent = new Intent(this, SettingActivity.class);
        startActivity(intent);
    }

    public void toRuleSetting(View view) {
        Intent intent = new Intent(this, RuleActivity.class);
        startActivity(intent);
    }

    public void toSendSetting(View view) {
        Intent intent = new Intent(this, SenderActivity.class);
        startActivity(intent);
    }

    public void cleanLog(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("确定要清空转发记录吗？")
                .setPositiveButton("清空", new DialogInterface.OnClickListener() {// 积极

                    @Override
                    public void onClick(DialogInterface dialog,
                                        int which) {
                        // TODO Auto-generated method stub
                        LogUtil.delLog(null, null);
                        initTLogs();
                        adapter.add(logVos);
                    }
                });
        builder.show();

    }

    //按返回键不退出回到桌面
    @Override
    public void onBackPressed() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addCategory(Intent.CATEGORY_HOME);
        startActivity(intent);
    }

    // 检查权限是否获取（android6.0及以上系统可能默认关闭权限，且没提示）
    private void checkPermission() {
        PackageManager pm = getPackageManager();
        boolean permission_receive_boot = (PackageManager.PERMISSION_GRANTED == pm.checkPermission("android.permission.RECEIVE_BOOT_COMPLETED", this.getPackageName()));
        boolean permission_readsms = (PackageManager.PERMISSION_GRANTED == pm.checkPermission("android.permission.READ_SMS", this.getPackageName()));
        boolean permission_read_phone_state = (PackageManager.PERMISSION_GRANTED == pm.checkPermission("android.permission.READ_PHONE_STATE", this.getPackageName()));
        boolean permission_read_phone_numbers = (PackageManager.PERMISSION_GRANTED == pm.checkPermission("android.permission.READ_PHONE_NUMBERS", this.getPackageName()));

        if (!(permission_receive_boot && permission_readsms && permission_read_phone_state && permission_read_phone_numbers)) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.RECEIVE_BOOT_COMPLETED,
                    Manifest.permission.READ_SMS,
                    Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.READ_PHONE_NUMBERS,
            }, 0x01);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.to_setting:
                toSetting();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;

    }

    @Override
    protected void onResume() {
        super.onResume();
        MobclickAgent.onResume(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        MobclickAgent.onPause(this);
    }

    //获取SIM卡信息
    private void getSimInfo(String Line1Number) {
        Uri uri = Uri.parse("content://telephony/siminfo"); //访问raw_contacts表
        MyApplication appContext = ((MyApplication) getApplicationContext());
        ContentResolver resolver = appContext.getContentResolver();
        Cursor cursor = resolver.query(uri, new String[]{"_id", "icc_id", "sim_id", "display_name", "carrier_name", "name_source", "color", "number", "display_number_format", "data_roaming", "mcc", "mnc"}, "sim_id >= 0", null, "_id");
        if (cursor != null) {
            while (cursor.moveToNext()) {
                Log.d(TAG, "_id: " + cursor.getString(cursor.getColumnIndex("_id")));
                Log.d(TAG, "sim_id: " + cursor.getString(cursor.getColumnIndex("sim_id")));
                Log.d(TAG, "carrier_name: " + cursor.getString(cursor.getColumnIndex("carrier_name")));
                Log.d(TAG, "display_name: " + cursor.getString(cursor.getColumnIndex("display_name")));
                Map<String, String> sim = new HashMap();
                String id = cursor.getString(cursor.getColumnIndex("_id"));
                sim.put("_id", id);
                sim.put("sim_id", cursor.getString(cursor.getColumnIndex("sim_id")));
                sim.put("carrier_name", cursor.getString(cursor.getColumnIndex("carrier_name")));
                sim.put("display_name", cursor.getString(cursor.getColumnIndex("display_name")));
                sim.put("phone_number", Line1Number);
                if (Line1Number != "Unknown") {
                    Line1Number = "Unknown";
                }
                MyApplication.SimInfo.put(id, sim);
            }
            cursor.close();
        }
    }

}
