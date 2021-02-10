package com.idormy.sms.forwarder.utils;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static com.idormy.sms.forwarder.SenderActivity.NOTIFY;

public class SenderQyWxGroupRobotMsg {

    static String TAG = "SenderQyWxGroupRobotMsg";

    public static void sendMsg(final Handler handError, String webHook, String from, String content) throws Exception {
        Log.i(TAG, "sendMsg webHook:" + webHook + " from:" + from + " content:" + content);

        if (webHook == null || webHook.isEmpty()) {
            return;
        }

        String textMsg = "{ \"msgtype\": \"text\", \"text\": {\"content\": \"" + from + " : " + content + "\"}}";
        OkHttpClient client = new OkHttpClient();
        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json;charset=utf-8"),
                textMsg);

        final Request request = new Request.Builder()
                .url(webHook)
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .post(requestBody)
                .build();
        Call call = client.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, final IOException e) {
                Log.d(TAG, "onFailure：" + e.getMessage());

//                SendHistory.addHistory("钉钉转发:"+msgf+"onFailure：" + e.getMessage());

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
                Log.d(TAG, "Code：" + String.valueOf(response.code()) + responseStr);

                if (handError != null) {
                    android.os.Message msg = new android.os.Message();
                    msg.what = NOTIFY;
                    Bundle bundle = new Bundle();
                    bundle.putString("DATA", "发送状态：" + responseStr);
                    msg.setData(bundle);
                    handError.sendMessage(msg);
                    Log.d(TAG, "Coxxyyde：" + String.valueOf(response.code()) + responseStr);
                }

            }
        });
    }

}
