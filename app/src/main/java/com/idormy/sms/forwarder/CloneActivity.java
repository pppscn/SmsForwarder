package com.idormy.sms.forwarder;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.alibaba.fastjson.JSON;
import com.hjq.permissions.OnPermissionCallback;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;
import com.hjq.toast.ToastUtils;
import com.idormy.sms.forwarder.model.vo.CloneInfoVo;
import com.idormy.sms.forwarder.receiver.BaseServlet;
import com.idormy.sms.forwarder.receiver.RebootBroadcastReceiver;
import com.idormy.sms.forwarder.sender.HttpServer;
import com.idormy.sms.forwarder.utils.CloneUtils;
import com.idormy.sms.forwarder.utils.Define;
import com.idormy.sms.forwarder.utils.FileUtils;
import com.idormy.sms.forwarder.utils.HttpUtils;
import com.idormy.sms.forwarder.utils.NetUtils;
import com.idormy.sms.forwarder.utils.SettingUtils;
import com.idormy.sms.forwarder.view.IPEditText;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class CloneActivity extends BaseActivity {
    private final String TAG = "CloneActivity";
    private Context context;
    private String serverIp;
    private String backupPath;
    private final String backupFile = "SmsForwarder.json";
    private IPEditText textServerIp;
    private TextView sendTxt;
    private TextView receiveTxt;
    private TextView backupPathTxt;
    private Button sendBtn;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_clone);
        Log.d(TAG, "onCreate: " + RebootBroadcastReceiver.class.getName());

        HttpUtils.init(this);
        HttpServer.init(this);
    }

    @SuppressWarnings({"rawtypes", "unchecked", "deprecation"})
    @SuppressLint("SetTextI18n")
    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");

        backupPathTxt = findViewById(R.id.backupPathTxt);
        // 申请储存权限
        XXPermissions.with(this).permission(Permission.Group.STORAGE).request(new OnPermissionCallback() {
            @Override
            public void onGranted(List<String> permissions, boolean all) {
                backupPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
                backupPathTxt.setText(backupPath + File.separator + backupFile);
            }

            @Override
            public void onDenied(List<String> permissions, boolean never) {
                if (never) {
                    ToastUtils.show(R.string.toast_denied_never);
                    // 如果是被永久拒绝就跳转到应用权限系统设置页面
                    XXPermissions.startPermissionActivity(CloneActivity.this, permissions);
                } else {
                    ToastUtils.show(R.string.toast_denied);
                }
                backupPathTxt.setText("未授权储存权限，该功能无法使用！");
            }
        });

        LinearLayout layoutNetwork = findViewById(R.id.layoutNetwork);
        LinearLayout layoutOffline = findViewById(R.id.layoutOffline);
        final RadioGroup radioGroupTypeCheck = findViewById(R.id.radioGroupTypeCheck);
        radioGroupTypeCheck.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.btnTypeOffline) {
                layoutNetwork.setVisibility(View.GONE);
                layoutOffline.setVisibility(View.VISIBLE);
            } else {
                layoutNetwork.setVisibility(View.VISIBLE);
                layoutOffline.setVisibility(View.GONE);
            }
        });

        sendBtn = findViewById(R.id.sendBtn);
        sendTxt = findViewById(R.id.sendTxt);
        TextView ipText = findViewById(R.id.ipText);
        textServerIp = findViewById(R.id.textServerIp);
        receiveTxt = findViewById(R.id.receiveTxt);
        Button receiveBtn = findViewById(R.id.receiveBtn);

        serverIp = NetUtils.getLocalIp(CloneActivity.this);
        ipText.setText(serverIp);

        if (HttpServer.asRunning()) {
            sendBtn.setText(R.string.stop);
            sendTxt.setText(R.string.server_has_started);
            textServerIp.setIP(serverIp);
        } else {
            sendBtn.setText(R.string.send);
            sendTxt.setText(R.string.server_has_stopped);
        }

        //发送
        sendBtn.setOnClickListener(v -> {
            if (!HttpServer.asRunning() && NetUtils.NETWORK_WIFI != NetUtils.getNetWorkStatus()) {
                ToastUtils.show(getString(R.string.no_wifi_network));
                return;
            }

            SettingUtils.switchEnableHttpServer(!SettingUtils.getSwitchEnableHttpServer());
            if (!HttpServer.update()) {
                SettingUtils.switchEnableHttpServer(!SettingUtils.getSwitchEnableHttpServer());
                return;
            }
            if (!HttpServer.asRunning()) {
                sendTxt.setText(R.string.server_has_stopped);
                textServerIp.setIP("");
                sendBtn.setText(R.string.send);
            } else {
                sendTxt.setText(R.string.server_has_started);
                textServerIp.setIP(serverIp);
                sendBtn.setText(R.string.stop);
            }
        });

        //接收
        receiveBtn.setOnClickListener(v -> {
            if (HttpServer.asRunning()) {
                receiveTxt.setText(R.string.sender_cannot_receive);
                ToastUtils.show(getString(R.string.sender_cannot_receive));
                return;
            }

            if (NetUtils.NETWORK_WIFI != NetUtils.getNetWorkStatus()) {
                receiveTxt.setText(R.string.no_wifi_network);
                ToastUtils.show(getString(R.string.no_wifi_network));
                return;
            }

            serverIp = textServerIp.getIP();
            if (serverIp == null || serverIp.isEmpty()) {
                receiveTxt.setText(R.string.invalid_server_ip);
                ToastUtils.show(getString(R.string.invalid_server_ip));
                return;
            }

            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            //设置读取超时时间
            OkHttpClient client = builder
                    .readTimeout(Define.REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .writeTimeout(Define.REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .connectTimeout(Define.REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .build();

            Map msgMap = new HashMap();
            msgMap.put("versionCode", SettingUtils.getVersionCode());
            msgMap.put("versionName", SettingUtils.getVersionName());

            String requestMsg = JSON.toJSONString(msgMap);
            Log.i(TAG, "requestMsg:" + requestMsg);
            RequestBody requestBody = RequestBody.create(MediaType.parse("application/json;charset=utf-8"), requestMsg);

            //请求链接：post 获取版本信息，get 下载备份文件
            final String requestUrl = "http://" + serverIp + ":" + Define.HTTP_SERVER_PORT + BaseServlet.CLONE_PATH + "?" + System.currentTimeMillis();
            Log.i(TAG, "requestUrl:" + requestUrl);

            //获取版本信息
            final Request request = new Request.Builder()
                    .url(requestUrl)
                    .addHeader("Content-Type", "application/json; charset=utf-8")
                    .post(requestBody)
                    .build();
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull final IOException e) {
                    ToastUtils.show(getString(R.string.tips_get_info_failed));
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    final String responseStr = Objects.requireNonNull(response.body()).string();
                    Log.d(TAG, "Response：" + response.code() + "，" + responseStr);

                    if (TextUtils.isEmpty(responseStr)) {
                        ToastUtils.show(getString(R.string.tips_get_info_failed));
                        return;
                    }

                    try {
                        CloneInfoVo cloneInfoVo = JSON.parseObject(responseStr, CloneInfoVo.class);
                        Log.d(TAG, cloneInfoVo.toString());

                        if (!SettingUtils.getVersionName().equals(cloneInfoVo.getVersionName())) {
                            ToastUtils.show(getString(R.string.tips_versions_inconsistent));
                            return;
                        }

                        if (CloneUtils.restoreSettings(cloneInfoVo)) {
                            ToastUtils.show(getString(R.string.tips_clone_done));
                        } else {
                            ToastUtils.show(getString(R.string.tips_clone_failed));
                        }

                    } catch (Exception e) {
                        ToastUtils.show(getString(R.string.tips_clone_failed) + e.getMessage());
                    }
                }
            });

        });

        Button exportBtn = findViewById(R.id.exportBtn);
        TextView exportTxt = findViewById(R.id.exportTxt);
        Button importBtn = findViewById(R.id.importBtn);
        TextView importTxt = findViewById(R.id.importTxt);

        //导出
        exportBtn.setOnClickListener(v -> {
            if (FileUtils.writeFileR(CloneUtils.exportSettings(), backupPath, backupFile, true)) {
                ToastUtils.show("导出配置成功！");
            } else {
                exportTxt.setText("导出失败，请检查写入权限！");
                ToastUtils.show("导出失败，请检查写入权限！");
            }
        });

        //导入
        importBtn.setOnClickListener(v -> {
            try {
                String responseStr = FileUtils.readFileI(backupPath, backupFile);
                if (TextUtils.isEmpty(responseStr)) {
                    ToastUtils.show(getString(R.string.tips_get_info_failed));
                    return;
                }

                CloneInfoVo cloneInfoVo = JSON.parseObject(responseStr, CloneInfoVo.class);
                Log.d(TAG, Objects.requireNonNull(cloneInfoVo).toString());

                if (!SettingUtils.getVersionName().equals(cloneInfoVo.getVersionName())) {
                    ToastUtils.show(getString(R.string.tips_versions_inconsistent));
                    return;
                }

                if (CloneUtils.restoreSettings(cloneInfoVo)) {
                    ToastUtils.show(getString(R.string.tips_clone_done));
                } else {
                    ToastUtils.show(getString(R.string.tips_clone_failed));
                }

            } catch (Exception e) {
                e.printStackTrace();
                importTxt.setText("还原失败：" + e.getMessage());
            }
        });

    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void onResume() {
        super.onResume();

        serverIp = NetUtils.getLocalIp(CloneActivity.this);
        TextView ipText = findViewById(R.id.ipText);
        ipText.setText(getString(R.string.local_ip) + serverIp);
    }

}
