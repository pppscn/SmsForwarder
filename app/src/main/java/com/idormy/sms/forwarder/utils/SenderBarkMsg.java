package com.idormy.sms.forwarder.utils;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static com.idormy.sms.forwarder.SenderActivity.NOTIFY;

public class SenderBarkMsg {

    static String TAG = "SenderBarkMsg";

    public static void sendMsg(final Handler handError, String barkServer, String from, String content) throws Exception {
        Log.i(TAG, "sendMsg barkServer:" + barkServer + " from:" + from + " content:" + content);

        if (barkServer == null || barkServer.isEmpty()) {
            return;
        }

        barkServer += URLEncoder.encode(from, "UTF-8");
        String body = "短信内容：" + content + "\n转发时间：" + (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        barkServer += "/" + URLEncoder.encode(content, "UTF-8");
        barkServer += "?isArchive=1"; //自动保存
        int isCode = content.indexOf("验证码");
        int isPassword = content.indexOf("动态密码");
        if (isCode != -1 || isPassword != -1) {
            Pattern p = Pattern.compile("(\\d{4,6})");
            Matcher m = p.matcher(content);
            if (m.find()) {
                System.out.println(m.group());
                barkServer += "&automaticallyCopy=1&copy=" + m.group();
            }
        }

        OkHttpClient client = new OkHttpClient();
        final Request request = new Request.Builder()
                .url(barkServer)
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .get()
                .build();
        Call call = client.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, final IOException e) {
                Log.d(TAG, "onFailure：" + e.getMessage());

                if (handError != null) {
                    android.os.Message msg = new android.os.Message();
                    msg.what = NOTIFY;
                    Bundle bundle = new Bundle();
                    bundle.putString("DATA", "发送失败：" + e.getMessage());
                    msg.setData(bundle);
                    handError.sendMessage(msg);
                }

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseStr = response.body().string();
                Log.d(TAG, "Code：" + response.code() + responseStr);

                if (handError != null) {
                    android.os.Message msg = new android.os.Message();
                    msg.what = NOTIFY;
                    Bundle bundle = new Bundle();
                    bundle.putString("DATA", "发送状态：" + responseStr);
                    msg.setData(bundle);
                    handError.sendMessage(msg);
                    Log.d(TAG, "Response：" + response.code() + responseStr);
                }

            }
        });
    }

}
