package com.idormy.sms.forwarder;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationManagerCompat;

import com.idormy.sms.forwarder.adapter.LogAdapter;
import com.idormy.sms.forwarder.model.vo.LogVo;
import com.idormy.sms.forwarder.model.vo.SmsVo;
import com.idormy.sms.forwarder.notify.NotifyHelper;
import com.idormy.sms.forwarder.notify.NotifyListener;
import com.idormy.sms.forwarder.sender.SendUtil;
import com.idormy.sms.forwarder.service.FrontService;
import com.idormy.sms.forwarder.service.NotifyService;
import com.idormy.sms.forwarder.utils.LogUtil;
import com.idormy.sms.forwarder.utils.NetUtil;
import com.idormy.sms.forwarder.utils.PhoneUtils;
import com.idormy.sms.forwarder.utils.SmsUtil;
import com.idormy.sms.forwarder.utils.aUtil;

import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements NotifyListener, RefreshListView.IRefreshListener {

    private final String TAG = "MainActivity";
    private static final String ACTION_NOTIFICATION_LISTENER_SETTINGS = "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS";
    // logVoList用于存储数据
    private List<LogVo> logVos = new ArrayList<>();
    private LogAdapter adapter;
    private RefreshListView listView;
    private Intent serviceIntent;
    private static final int REQUEST_CODE = 9999;
    private String currentType = "sms";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        LogUtil.init(this);
        Log.d(TAG, "onCreate");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //检查权限是否获取
        PackageManager pm = getPackageManager();
        PhoneUtils.CheckPermission(pm, this);

        //获取SIM信息
        PhoneUtils.init(this);

        //短信&网络组件初始化
        SmsUtil.init(this);
        NetUtil.init(this);

        //应用通知
        NotifyHelper.getInstance().setNotifyListener(this);

        //前台服务
        serviceIntent = new Intent(MainActivity.this, FrontService.class);
        serviceIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startService(serviceIntent);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");

        //是否关闭页面提示
        TextView help_tip = findViewById(R.id.help_tip);
        help_tip.setVisibility(MyApplication.showHelpTip ? View.VISIBLE : View.GONE);

        // 先拿到数据并放在适配器上
        initTLogs(); //初始化数据
        showList(logVos);

        //切换日志类别
        int typeCheckId = getTypeCheckId(currentType);
        final RadioGroup radioGroupTypeCheck = findViewById(R.id.radioGroupTypeCheck);
        radioGroupTypeCheck.check(typeCheckId);
        radioGroupTypeCheck.setOnCheckedChangeListener((group, checkedId) -> {
            RadioButton rb = findViewById(checkedId);
            currentType = (String) rb.getTag();
            initTLogs();
            showList(logVos);
        });

        // 为ListView注册一个监听器，当用户点击了ListView中的任何一个子项时，就会回调onItemClick()方法
        // 在这个方法中可以通过position参数判断出用户点击的是那一个子项
        listView.setOnItemClickListener((parent, view, position, id) -> {
            if (position <= 0) return;

            LogVo logVo = logVos.get(position - 1);
            logDetail(logVo);
        });

        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            if (position <= 0) return false;

            //定义AlertDialog.Builder对象，当长按列表项的时候弹出确认删除对话框
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle(R.string.delete_log_title);
            builder.setMessage(R.string.delete_log_tips);

            //添加AlertDialog.Builder对象的setPositiveButton()方法
            builder.setPositiveButton(R.string.confirm, (dialog, which) -> {
                Long id1 = logVos.get(position - 1).getId();
                Log.d(TAG, "id = " + id1);
                LogUtil.delLog(id1, null);
                initTLogs(); //初始化数据
                showList(logVos);
                Toast.makeText(getBaseContext(), R.string.delete_log_toast, Toast.LENGTH_SHORT).show();
            });

            //添加AlertDialog.Builder对象的setNegativeButton()方法
            builder.setNegativeButton(R.string.cancel, (dialog, which) -> {
            });

            builder.create().show();
            return true;
        });
    }

    private int getTypeCheckId(String currentType) {
        switch (currentType) {
            case "call":
                return R.id.btnTypeCall;
            case "app":
                return R.id.btnTypeApp;
            default:
                return R.id.btnTypeSms;
        }
    }

    @SuppressLint("ObsoleteSdkInt")
    @Override
    protected void onResume() {
        super.onResume();
        //第一次打开，未授权无法获取SIM信息，尝试在此重新获取
        if (MyApplication.SimInfoList.isEmpty()) {
            MyApplication.SimInfoList = PhoneUtils.getSimMultiInfo();
        }
        Log.d(TAG, "SimInfoList = " + MyApplication.SimInfoList.size());

        //开启读取通知栏权限
        if (!isNotificationListenerServiceEnabled(this)) {
            openNotificationAccess();
            toggleNotificationListenerService();
            Toast.makeText(this, "请先勾选《短信转发器》的读取通知栏权限!", Toast.LENGTH_LONG).show();
            return;
        }

        //省电优化设置为无限制
        if (Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            if (!isIgnoringBatteryOptimizations()) {
                Toast.makeText(this, "请将省电优化设置为无限制，有利于防止《短信转发器》被杀！!", Toast.LENGTH_LONG).show();
            }
        }
        startService(serviceIntent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        startService(serviceIntent);
    }

    @Override
    protected void onPause() {
        //当界面返回到桌面之后.清除通知设置当中的数据.减少内存占有
        //if (applicationList != null) {
        //    applicationList.setAdapter(null);
        //    adapter = null;
        //}
        super.onPause();
        startService(serviceIntent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE) {
            if (isNotificationListenerServiceEnabled(this)) {
                Toast.makeText(this, "通知服务已开启", Toast.LENGTH_SHORT).show();
                toggleNotificationListenerService();
            } else {
                Toast.makeText(this, "通知服务未开启", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // 权限判断相关
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    public void toggleNotificationListenerService() {
        PackageManager pm = getPackageManager();
        pm.setComponentEnabledSetting(new ComponentName(getApplicationContext(), NotifyService.class),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);

        pm.setComponentEnabledSetting(new ComponentName(getApplicationContext(), NotifyService.class),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
    }

    private static boolean isNotificationListenerServiceEnabled(Context context) {
        Set<String> packageNames = NotificationManagerCompat.getEnabledListenerPackages(context);
        return packageNames.contains(context.getPackageName());
    }

    /**
     * 判断系统是否已经关闭省电优化
     *
     * @return boolean
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    private boolean isIgnoringBatteryOptimizations() {
        boolean isIgnoring = false;
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (powerManager != null) {
            isIgnoring = powerManager.isIgnoringBatteryOptimizations(getPackageName());
        }
        if (!isIgnoring) {
            Intent i = new Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
            startActivity(i);
        }
        return isIgnoring;
    }

    private void openNotificationAccess() {
        startActivity(new Intent(ACTION_NOTIFICATION_LISTENER_SETTINGS));
    }


    // 初始化数据
    private void initTLogs() {
        logVos = LogUtil.getLog(null, null, currentType);
    }

    private void showList(List<LogVo> logVosN) {
        Log.d(TAG, "showList: " + logVosN);
        if (adapter == null) {
            // 将适配器上的数据传递给listView
            listView = findViewById(R.id.list_view_log);
            listView.setInterface(this);
            adapter = new LogAdapter(MainActivity.this, R.layout.item_log, logVosN);
            listView.setAdapter(adapter);
        } else {
            adapter.onDateChange(logVosN);
        }
    }

    @Override
    public void onRefresh() {
        Handler handler = new Handler();
        handler.postDelayed(() -> {
            // TODO Auto-generated method stub
            //获取最新数据
            initTLogs();
            //通知界面显示
            showList(logVos);
            //通知listview 刷新数据完毕；
            listView.refreshComplete();
        }, 2000);
    }

    public void logDetail(LogVo logVo) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle(R.string.details);
        String simInfo = logVo.getSimInfo();
        if (simInfo != null) {
            builder.setMessage(logVo.getFrom() + "\n\n" + logVo.getContent() + "\n\n" + logVo.getSimInfo() + "\n\n" + logVo.getRule() + "\n\n" + aUtil.utc2Local(logVo.getTime()) + "\n\nResponse：" + logVo.getForwardResponse());
        } else {
            builder.setMessage(logVo.getFrom() + "\n\n" + logVo.getContent() + "\n\n" + logVo.getRule() + "\n\n" + aUtil.utc2Local(logVo.getTime()) + "\n\nResponse：" + logVo.getForwardResponse());
        }
        //删除
        builder.setNegativeButton(R.string.del, (dialog, which) -> {
            Long id = logVo.getId();
            Log.d(TAG, "id = " + id);
            LogUtil.delLog(id, null);
            initTLogs(); //初始化数据
            showList(logVos);
            Toast.makeText(MainActivity.this, R.string.delete_log_toast, Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
        builder.show();
    }

    public void toAppList() {
        Intent intent = new Intent(this, AppListActivity.class);
        startActivity(intent);
    }

    public void toClone() {
        Intent intent = new Intent(this, CloneActivity.class);
        startActivity(intent);
    }

    public void toSetting() {
        Intent intent = new Intent(this, SettingActivity.class);
        startActivity(intent);
    }

    public void toAbout() {
        Intent intent = new Intent(this, AboutActivity.class);
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
        builder.setTitle(R.string.clear_logs_tips)
                .setPositiveButton(R.string.confirm, (dialog, which) -> {
                    // TODO Auto-generated method stub
                    LogUtil.delLog(null, null);
                    initTLogs();
                    adapter.add(logVos);
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
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.to_app_list:
                toAppList();
                return true;
            case R.id.to_clone:
                toClone();
                return true;
            case R.id.to_setting:
                toSetting();
                return true;
            case R.id.to_about:
                toAbout();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    //设置menu图标显示
    @Override
    public boolean onMenuOpened(int featureId, Menu menu) {
        if (featureId == Window.FEATURE_ACTION_BAR && menu != null) {
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
     * 收到通知
     *
     * @param type 通知类型
     */
    @Override
    public void onReceiveMessage(int type) {
        Log.d(TAG, "收到通知=" + type);
    }

    /**
     * 移除通知
     *
     * @param type 通知类型
     */
    @Override
    public void onRemovedMessage(int type) {
        Log.d(TAG, "移除通知=" + type);
    }

    /**
     * 收到通知
     *
     * @param sbn 状态栏通知
     */
    @Override
    public void onReceiveMessage(StatusBarNotification sbn) {
        if (sbn.getNotification() == null) return;
        if (sbn.getNotification().extras == null) return;

        //推送通知的应用包名
        String packageName = sbn.getPackageName();
        //通知标题
        String title = sbn.getNotification().extras.get("android.title").toString();
        //通知内容
        String text = sbn.getNotification().extras.get("android.text").toString();
        if (text.isEmpty() && sbn.getNotification().tickerText != null) {
            text = sbn.getNotification().tickerText.toString();
        }
        //通知时间
        String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINESE).format(new Date(sbn.getPostTime()));

        Log.d(TAG, String.format(
                Locale.getDefault(),
                "onNotificationPosted：\n应用包名：%s\n消息标题：%s\n消息内容：%s\n消息时间：%s\n",
                packageName, title, text, time)
        );

        //不处理空消息
        if (title.isEmpty() && text.isEmpty()) return;

        SmsVo smsVo = new SmsVo(packageName, text, new Date(), title);
        Log.d(TAG, "send_msg" + smsVo.toString());
        SendUtil.send_msg(this, smsVo, 1, "app");
    }

    /**
     * 移除掉通知栏消息
     *
     * @param sbn 状态栏通知
     */
    @Override
    public void onRemovedMessage(StatusBarNotification sbn) {
        Log.d(TAG, "移除掉通知栏消息");
    }

}
