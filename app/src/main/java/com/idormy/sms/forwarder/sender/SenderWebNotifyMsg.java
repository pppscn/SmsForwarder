package com.idormy.sms.forwarder.sender;

import android.os.Bundle;
import android.os.Handler;
import android.util.Base64;
import android.util.Log;

import com.idormy.sms.forwarder.utils.CertUtils;
import com.idormy.sms.forwarder.utils.LogUtil;

import java.io.IOException;
import java.net.URLEncoder;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static com.idormy.sms.forwarder.SenderActivity.NOTIFY;

public class SenderWebNotifyMsg {

    static String TAG = "SenderWebNotifyMsg";

    public static void sendMsg(final long logId, final Handler handError, String webServer, String secret, String method, String from, String content) throws Exception {
        Log.i(TAG, "sendMsg webServer:" + webServer + " from:" + from + " content:" + content);

        if (webServer == null || webServer.isEmpty()) {
            return;
        }

        Long timestamp = System.currentTimeMillis();
        String sign = "";
        if (secret != null && !secret.isEmpty()) {
            String stringToSign = timestamp + "\n" + secret;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes("UTF-8"), "HmacSHA256"));
            byte[] signData = mac.doFinal(stringToSign.getBytes("UTF-8"));
            sign = URLEncoder.encode(new String(Base64.encode(signData, Base64.NO_WRAP)), "UTF-8");
            Log.i(TAG, "sign:" + sign);
        }

        Request request;
        if (method.equals("GET")) {
            webServer += (webServer.contains("?") ? "&" : "?") + "from=" + URLEncoder.encode(from, "UTF-8");
            webServer += "&content=" + URLEncoder.encode(content, "UTF-8");
            if (secret != null && !secret.isEmpty()) {
                webServer += "&timestamp=" + timestamp;
                webServer += "&sign=" + sign;
            }

            Log.d(TAG, "method = GET, Url = " + webServer);
            request = new Request.Builder().url(webServer).get().build();
        } else {
            MultipartBody.Builder builder = new MultipartBody.Builder().setType(MultipartBody.FORM)
                    .addFormDataPart("from", from)
                    .addFormDataPart("content", content);
            if (secret != null && !secret.isEmpty()) {
                builder.addFormDataPart("timestamp", String.valueOf(timestamp));
                builder.addFormDataPart("sign", sign);
            }

            RequestBody body = builder.build();
            Log.d(TAG, "method = POST, Body = " + body);
            request = new Request.Builder().url(webServer).method("POST", body).build();
        }

        OkHttpClient client = new OkHttpClient().newBuilder()
                //忽略https证书
                .sslSocketFactory(CertUtils.getSSLSocketFactory(), CertUtils.getX509TrustManager())
                .hostnameVerifier(CertUtils.getHostnameVerifier())
                .build();
        Call call = client.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, final IOException e) {
                LogUtil.updateLog(logId, 0, e.getMessage());
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
                Log.d(TAG, "Code：" + response.code() + " Response：" + responseStr);

                //返回http状态200即为成功
                if (200 == response.code()) {
                    LogUtil.updateLog(logId, 1, responseStr);
                } else {
                    LogUtil.updateLog(logId, 0, responseStr);
                }

                if (handError != null) {
                    android.os.Message msg = new android.os.Message();
                    msg.what = NOTIFY;
                    Bundle bundle = new Bundle();
                    bundle.putString("DATA", "发送状态：" + responseStr);
                    msg.setData(bundle);
                    handError.sendMessage(msg);
                }

            }
        });
    }


}
