package com.idormy.sms.forwarder.sender;

import android.os.Handler;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;

import com.alibaba.fastjson.JSON;
import com.idormy.sms.forwarder.utils.Define;
import com.idormy.sms.forwarder.utils.LogUtils;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@SuppressWarnings({"rawtypes", "unchecked", "deprecation"})
public class SenderDingdingMsg extends SenderBaseMsg {

    static final String TAG = "SenderDingdingMsg";

    public static void sendMsg(final long logId, final Handler handError, final RetryIntercepter retryInterceptor, String token, String secret, String atMobiles, Boolean atAll, String content) throws Exception {
        Log.i(TAG, "sendMsg token:" + token + " secret:" + secret + " atMobiles:" + atMobiles + " atAll:" + atAll + " content:" + content);

        if (token == null || token.isEmpty()) {
            return;
        }

        if (secret != null && !secret.isEmpty()) {
            Long timestamp = System.currentTimeMillis();
            String stringToSign = timestamp + "\n" + secret;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] signData = mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8));
            String sign = URLEncoder.encode(new String(Base64.encode(signData, Base64.NO_WRAP)), "UTF-8");
            token += "&timestamp=" + timestamp + "&sign=" + sign;
            Log.i(TAG, "token:" + token);
        }

        Map textMsgMap = new HashMap();
        textMsgMap.put("msgtype", "text");
        Map textText = new HashMap();
        textText.put("content", content);
        textMsgMap.put("text", textText);
        if (atMobiles != null || atAll != null) {
            Map AtMap = new HashMap();
            if (atMobiles != null) {
                String[] atMobilesArray = atMobiles.split(",");
                List<String> atMobilesList = new ArrayList<>();
                for (String atMobile : atMobilesArray
                ) {
                    if (TextUtils.isDigitsOnly(atMobile)) {
                        atMobilesList.add(atMobile);
                    }
                }
                if (!atMobilesList.isEmpty()) {
                    AtMap.put("atMobiles", atMobilesList);
                }
            }

            AtMap.put("isAtAll", false);
            if (atAll != null) {
                AtMap.put("isAtAll", atAll);
            }

            textMsgMap.put("at", AtMap);
        }

        final String requestUrl = "https://oapi.dingtalk.com/robot/send?access_token=" + token;
        Log.i(TAG, "requestUrl:" + requestUrl);
        final String requestMsg = JSON.toJSONString(textMsgMap);
        Log.i(TAG, "requestMsg:" + requestMsg);

        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        //设置重试拦截器
        if (retryInterceptor != null) builder.addInterceptor(retryInterceptor);
        //设置读取超时时间
        OkHttpClient client = builder
                .readTimeout(Define.REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .writeTimeout(Define.REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .connectTimeout(Define.REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .build();

        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json;charset=utf-8"), requestMsg);

        final Request request = new Request.Builder()
                .url(requestUrl)
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull final IOException e) {
                LogUtils.updateLog(logId, 0, e.getMessage());
                Toast(handError, TAG, "发送失败：" + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                final String responseStr = Objects.requireNonNull(response.body()).string();
                Log.d(TAG, "Response：" + response.code() + "，" + responseStr);
                Toast(handError, TAG, "发送状态：" + responseStr);

                //TODO:粗略解析是否发送成功
                if (responseStr.contains("\"errcode\":0")) {
                    LogUtils.updateLog(logId, 2, responseStr);
                } else {
                    LogUtils.updateLog(logId, 0, responseStr);
                }
            }
        });

    }

}