package com.idormy.sms.forwarder.utils;

import android.util.Base64;
import android.util.Log;

import java.io.IOException;
import java.net.URLEncoder;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class DingdingMsg {

    static String TAG = "DingdingMsg";

    public static void sendMsg(String msg) throws Exception {

        String webhook_token = SettingUtil.get_using_dingding_token();
        String webhook_secret = SettingUtil.get_using_dingding_secret();
        if (webhook_token.equals("")) {
            return;
        }
        if (!webhook_secret.equals("")) {
            Long timestamp = System.currentTimeMillis();

            String stringToSign = timestamp + "\n" + webhook_secret;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhook_secret.getBytes("UTF-8"), "HmacSHA256"));
            byte[] signData = mac.doFinal(stringToSign.getBytes("UTF-8"));
            String sign = URLEncoder.encode(new String(Base64.encode(signData, Base64.NO_WRAP)), "UTF-8");
            webhook_token += "&timestamp=" + timestamp + "&sign=" + sign;
            Log.i(TAG, "webhook_token:" + webhook_token);

        }

        final String msgf = msg;
        String textMsg = "{ \"msgtype\": \"text\", \"text\": {\"content\": \"" + msg + "\"}}";
        OkHttpClient client = new OkHttpClient();
        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json;charset=utf-8"),
                textMsg);

        final Request request = new Request.Builder()
                .url(webhook_token)
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .post(requestBody)
                .build();
        Call call = client.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.d(TAG, "onFailure：" + e.getMessage());
                SendHistory.addHistory("钉钉转发:" + msgf + "onFailure：" + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseStr = response.body().string();
                Log.d(TAG, "Code：" + String.valueOf(response.code()) + responseStr);
                SendHistory.addHistory("钉钉转发:" + msgf + "Code：" + String.valueOf(response.code()) + responseStr);
            }
        });
    }
}
