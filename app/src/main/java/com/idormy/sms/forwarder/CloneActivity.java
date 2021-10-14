package com.idormy.sms.forwarder;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.idormy.sms.forwarder.receiver.RebootBroadcastReceiver;
import com.idormy.sms.forwarder.utils.LogUtil;
import com.idormy.sms.forwarder.utils.NetUtil;
import com.idormy.sms.forwarder.view.IPEditText;
import com.koushikdutta.async.http.WebSocket;
import com.koushikdutta.async.http.server.AsyncHttpServer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class CloneActivity extends AppCompatActivity {
    private final String TAG = "com.idormy.sms.forwarder.CloneActivity";
    private Context context;
    private boolean isRunning = false;
    private String serverIp;
    private final String DATABASE_NAME = "sms_forwarder.db";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);

        context = CloneActivity.this;

        setContentView(R.layout.activity_clone);
        Log.d(TAG, "onCreate: " + RebootBroadcastReceiver.class.getName());

    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");

        IPEditText textServerIp = findViewById(R.id.textServerIp);

        List<WebSocket> _sockets = new ArrayList<>();
        AsyncHttpServer server = new AsyncHttpServer();

        TextView sendTxt = findViewById(R.id.sendTxt);
        TextView receiveTxt = findViewById(R.id.receiveTxt);

        Button sendBtn = findViewById(R.id.sendBtn);
        sendBtn.setOnClickListener(v -> {
            if (NetUtil.NETWORK_WIFI != NetUtil.getNetWorkStatus()) {
                Toast.makeText(CloneActivity.this, R.string.no_wifi_network, Toast.LENGTH_SHORT).show();
                return;
            } else {
                serverIp = NetUtil.getLocalIp(CloneActivity.this);
                TextView ipText = findViewById(R.id.ipText);
                ipText.setText(getString(R.string.local_ip) + serverIp);
            }
            if (!isRunning) {
                isRunning = true;
                server.get("/", (request, response) -> {
                    File file = context.getDatabasePath(DATABASE_NAME);
                    response.getHeaders().add("Content-Disposition", "attachment;filename=" + DATABASE_NAME);
                    response.sendFile(file);
                });
                server.listen(5000);
                Toast.makeText(CloneActivity.this, R.string.server_has_started, Toast.LENGTH_SHORT).show();
                sendTxt.setText(R.string.server_has_started);
                textServerIp.setIP(serverIp);
                sendBtn.setText(R.string.stop);
            } else {
                isRunning = false;
                server.stop();
                Toast.makeText(CloneActivity.this, R.string.server_has_stopped, Toast.LENGTH_SHORT).show();
                sendTxt.setText(R.string.server_has_stopped);
                textServerIp.setIP("");
                sendBtn.setText(R.string.send);
            }
        });

        Button receiveBtn = findViewById(R.id.receiveBtn);
        receiveBtn.setOnClickListener(v -> {
            if (isRunning) {
                receiveTxt.setText(R.string.sender_cannot_receive);
                Toast.makeText(CloneActivity.this, R.string.sender_cannot_receive, Toast.LENGTH_SHORT).show();
                return;
            }

            if (NetUtil.NETWORK_WIFI != NetUtil.getNetWorkStatus()) {
                receiveTxt.setText(R.string.no_wifi_network);
                Toast.makeText(CloneActivity.this, R.string.no_wifi_network, Toast.LENGTH_SHORT).show();
                return;
            }

            serverIp = textServerIp.getIP();
            if (serverIp == null || serverIp.isEmpty()) {
                receiveTxt.setText(R.string.invalid_server_ip);
                Toast.makeText(CloneActivity.this, R.string.invalid_server_ip, Toast.LENGTH_SHORT).show();
                return;
            }

            //下载连接
            final String url = "http://" + serverIp + ":5000/";
            Log.d(TAG, url);
            //保存路径
            final String savePath = context.getCacheDir().getPath() + File.separator + DATABASE_NAME;
            Log.d(TAG, savePath);
            final long startTime = System.currentTimeMillis();
            Log.i(TAG, "startTime=" + startTime);
            OkHttpClient okHttpClient = new OkHttpClient();
            Request request = new Request.Builder().url(url).addHeader("Connection", "close").build();
            okHttpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    e.printStackTrace();
                    //Toast.makeText(CloneActivity.this, R.string.download_failed + e.getMessage(), Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) {
                    InputStream is = null;
                    byte[] buf = new byte[2048];
                    int len;
                    FileOutputStream fos = null;

                    try {
                        is = Objects.requireNonNull(response.body()).byteStream();
                        long total = Objects.requireNonNull(response.body()).contentLength();
                        File file = new File(savePath, url.substring(url.lastIndexOf("/") + 1));
                        fos = new FileOutputStream(file);
                        long sum = 0;
                        while ((len = is.read(buf)) != -1) {
                            fos.write(buf, 0, len);
                            sum += len;
                            int progress = (int) (sum * 1.0f / total * 100);
                            Log.e(TAG, "download progress : " + progress);
                        }
                        fos.flush();
                        Log.e(TAG, "download success");
                        Log.e(TAG, "totalTime=" + (System.currentTimeMillis() - startTime));
                        //Toast.makeText(CloneActivity.this, R.string.download_success, Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        e.printStackTrace();
                        //Toast.makeText(CloneActivity.this, R.string.download_failed + e.getMessage(), Toast.LENGTH_SHORT).show();
                    } finally {
                        try {
                            if (is != null) is.close();
                        } catch (IOException ignored) {
                        }
                        try {
                            if (fos != null) fos.close();
                        } catch (IOException ignored) {
                        }
                    }
                }
            });

            //TODO:替换sqlite
            File dbFile = new File(savePath);
            FileInputStream fis;
            try {
                fis = new FileInputStream(dbFile);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                return;
            }

            String outFileName = context.getDatabasePath(DATABASE_NAME).getAbsolutePath();
            Log.d(TAG, outFileName);

            // Open the empty db as the output stream
            OutputStream output;
            try {
                output = new FileOutputStream(outFileName);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                return;
            }

            // Transfer bytes from the input file to the output file
            byte[] buffer = new byte[1024];
            int length;
            while (true) {
                try {
                    if (!((length = fis.read(buffer)) > 0)) break;
                    output.write(buffer, 0, length);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            // Close the streams
            try {
                output.flush();
                output.close();
                fis.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            LogUtil.delLog(null, null);

            receiveTxt.setText(R.string.download_success);
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
}
