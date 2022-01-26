package com.idormy.sms.forwarder;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.alibaba.fastjson.JSON;
import com.idormy.sms.forwarder.model.vo.CloneInfoVo;
import com.idormy.sms.forwarder.receiver.BaseServlet;
import com.idormy.sms.forwarder.receiver.RebootBroadcastReceiver;
import com.idormy.sms.forwarder.sender.HttpServer;
import com.idormy.sms.forwarder.utils.BackupDbTask;
import com.idormy.sms.forwarder.utils.Define;
import com.idormy.sms.forwarder.utils.DownloadUtil;
import com.idormy.sms.forwarder.utils.HttpUtil;
import com.idormy.sms.forwarder.utils.NetUtil;
import com.idormy.sms.forwarder.utils.SettingUtil;
import com.idormy.sms.forwarder.view.IPEditText;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
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

public class CloneActivity extends AppCompatActivity {
    private final String TAG = "CloneActivity";
    private Context context;
    private String serverIp;
    public static final String DATABASE_NAME = "sms_forwarder.db";
    private IPEditText textServerIp;
    private TextView sendTxt;
    private TextView receiveTxt;
    private Button sendBtn;
    public static final int TOAST = 0x9731994;
    public static final int DOWNLOAD = 0x9731995;

    //消息处理者,创建一个Handler的子类对象,目的是重写Handler的处理消息的方法(handleMessage())
    @SuppressWarnings("deprecation")
    @SuppressLint("HandlerLeak")
    private final Handler handError = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == TOAST) {
                Toast.makeText(CloneActivity.this, msg.getData().getString("DATA"), Toast.LENGTH_LONG).show();
            } else if (msg.what == DOWNLOAD) {
                String savePath = context.getCacheDir().getPath() + File.separator + BackupDbTask.BACKUP_FILE;
                Log.d(TAG, savePath);
                downloadFile(msg.getData().getString("URL"), context.getCacheDir().getPath(), BackupDbTask.BACKUP_FILE, msg.getData().getString("INFO"));
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);

        context = CloneActivity.this;

        setContentView(R.layout.activity_clone);
        Log.d(TAG, "onCreate: " + RebootBroadcastReceiver.class.getName());

        HttpUtil.init(this);
        HttpServer.init(this);
    }

    @SuppressWarnings({"rawtypes", "unchecked", "deprecation"})
    @SuppressLint("SetTextI18n")
    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");

        sendBtn = findViewById(R.id.sendBtn);
        sendTxt = findViewById(R.id.sendTxt);
        TextView ipText = findViewById(R.id.ipText);
        textServerIp = findViewById(R.id.textServerIp);
        receiveTxt = findViewById(R.id.receiveTxt);
        Button receiveBtn = findViewById(R.id.receiveBtn);

        serverIp = NetUtil.getLocalIp(CloneActivity.this);
        ipText.setText(serverIp);

        if (HttpServer.asRunning()) {
            sendBtn.setText(R.string.stop);
            sendTxt.setText(R.string.server_has_started);
            textServerIp.setIP(serverIp);
        } else {
            sendBtn.setText(R.string.send);
            sendTxt.setText(R.string.server_has_stopped);
        }
        sendBtn.setOnClickListener(v -> {
            if (!HttpServer.asRunning() && NetUtil.NETWORK_WIFI != NetUtil.getNetWorkStatus()) {
                Toast(handError, TAG, getString(R.string.no_wifi_network));
                return;
            }

            //备份文件
            BackupDbTask task = new BackupDbTask(this);
            String backup_version = task.doInBackground(BackupDbTask.COMMAND_BACKUP);
            Log.d(TAG, "backup_version = " + backup_version);

            SettingUtil.switchEnableHttpServer(!SettingUtil.getSwitchEnableHttpServer());
            if (!HttpServer.update()) {
                SettingUtil.switchEnableHttpServer(!SettingUtil.getSwitchEnableHttpServer());
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

        receiveBtn.setOnClickListener(v -> {
            if (HttpServer.asRunning()) {
                receiveTxt.setText(R.string.sender_cannot_receive);
                Toast(handError, TAG, getString(R.string.sender_cannot_receive));
                return;
            }

            if (NetUtil.NETWORK_WIFI != NetUtil.getNetWorkStatus()) {
                receiveTxt.setText(R.string.no_wifi_network);
                Toast(handError, TAG, getString(R.string.no_wifi_network));
                return;
            }

            serverIp = textServerIp.getIP();
            if (serverIp == null || serverIp.isEmpty()) {
                receiveTxt.setText(R.string.invalid_server_ip);
                Toast(handError, TAG, getString(R.string.invalid_server_ip));
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
            msgMap.put("versionCode", SettingUtil.getVersionCode());
            msgMap.put("versionName", SettingUtil.getVersionName());

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
                    Toast(handError, TAG, getString(R.string.tips_get_info_failed));
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    final String responseStr = Objects.requireNonNull(response.body()).string();
                    Log.d(TAG, "Response：" + response.code() + "，" + responseStr);

                    if (TextUtils.isEmpty(responseStr)) {
                        Toast(handError, TAG, getString(R.string.tips_get_info_failed));
                        return;
                    }

                    try {
                        CloneInfoVo cloneInfoVo = JSON.parseObject(responseStr, CloneInfoVo.class);
                        if (SettingUtil.getVersionCode() != cloneInfoVo.getVersionCode()) {
                            Toast(handError, TAG, getString(R.string.tips_versions_inconsistent));
                            return;
                        }

                        //下载备份文件
                        Message msg = new Message();
                        msg.what = DOWNLOAD;
                        Bundle bundle = new Bundle();
                        bundle.putString("URL", requestUrl);
                        bundle.putString("INFO", responseStr);
                        msg.setData(bundle);
                        handError.sendMessage(msg);

                    } catch (Exception e) {
                        Toast(handError, TAG, getString(R.string.tips_clone_failed) + e.getMessage());
                    }
                }
            });

        });

    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void onResume() {
        super.onResume();

        serverIp = NetUtil.getLocalIp(CloneActivity.this);
        TextView ipText = findViewById(R.id.ipText);
        ipText.setText(getString(R.string.local_ip) + serverIp);
    }

    /**
     * 文件下载
     *
     * @param url 下载链接
     */
    public void downloadFile(String url, final String destFileDir, final String destFileName, final String cloneInfo) {
        ProgressDialog progressDialog = new ProgressDialog(context);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setTitle(getString(R.string.tips_downloading));
        progressDialog.setMessage(getString(R.string.tips_please_wait));
        progressDialog.setProgress(0);
        progressDialog.setMax(100);
        progressDialog.show();
        progressDialog.setCancelable(false);
        DownloadUtil.get().download(url, destFileDir, destFileName, new DownloadUtil.OnDownloadListener() {
            @Override
            public void onDownloadSuccess(File file) {
                if (progressDialog.isShowing()) {
                    Toast(handError, TAG, getString(R.string.tips_download_done));
                    progressDialog.dismiss();
                }
                //下载完成进行相关逻辑操作
                Log.d(TAG, file.getPath());

                //还原数据库
                BackupDbTask task = new BackupDbTask(context);
                String backup_version = task.doInBackground(BackupDbTask.COMMAND_RESTORE);
                Log.d(TAG, "backup_version = " + backup_version);

                //应用配置
                CloneInfoVo cloneInfoVo = JSON.parseObject(cloneInfo, CloneInfoVo.class);
                System.out.println(cloneInfoVo.toString());
                SettingUtil.init(context);
                SettingUtil.switchEnableSms(cloneInfoVo.isEnableSms());
                SettingUtil.switchEnablePhone(cloneInfoVo.isEnablePhone());
                SettingUtil.switchCallType1(cloneInfoVo.isCallType1());
                SettingUtil.switchCallType2(cloneInfoVo.isCallType2());
                SettingUtil.switchCallType3(cloneInfoVo.isCallType3());
                SettingUtil.switchEnableAppNotify(cloneInfoVo.isEnableAppNotify());
                SettingUtil.switchCancelAppNotify(cloneInfoVo.isCancelAppNotify());
                SettingUtil.smsHubApiUrl(cloneInfoVo.getSmsHubApiUrl());
                SettingUtil.setBatteryLevelAlarmMin(cloneInfoVo.getBatteryLevelAlarmMin());
                SettingUtil.setBatteryLevelAlarmMax(cloneInfoVo.getBatteryLevelAlarmMax());
                SettingUtil.switchBatteryLevelAlarmOnce(cloneInfoVo.isBatteryLevelAlarmOnce());
                SettingUtil.setRetryTimes(cloneInfoVo.getRetryTimes());
                SettingUtil.setDelayTime(cloneInfoVo.getDelayTime());
                SettingUtil.switchSmsTemplate(cloneInfoVo.isEnableSmsTemplate());
                SettingUtil.setSmsTemplate(cloneInfoVo.getSmsTemplate());

                Toast(handError, TAG, getString(R.string.tips_clone_done));
            }

            @Override
            public void onDownloading(int progress) {
                progressDialog.setProgress(progress);
            }

            @SuppressLint("SetTextI18n")
            @Override
            public void onDownloadFailed(Exception e) {
                //下载异常进行相关提示操作
                Log.e(TAG, getString(R.string.tips_download_failed) + e.getMessage());
                Toast(handError, TAG, getString(R.string.tips_download_failed) + e.getMessage());
            }
        });
    }

    public static void Toast(Handler handError, String Tag, String data) {
        Log.i(Tag, data);
        if (handError != null) {
            Message msg = new Message();
            msg.what = TOAST;
            Bundle bundle = new Bundle();
            bundle.putString("DATA", data);
            msg.setData(bundle);
            handError.sendMessage(msg);
        }
    }
}
