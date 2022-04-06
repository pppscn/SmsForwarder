package com.idormy.sms.forwarder.sender;

import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;

import com.alibaba.fastjson.JSON;
import com.idormy.sms.forwarder.utils.Define;
import com.idormy.sms.forwarder.utils.LogUtils;

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

@SuppressWarnings({"rawtypes", "unchecked", "deprecation", "RedundantThrows"})
public class SenderQyWxGroupRobotMsg extends SenderBaseMsg {

    static final String TAG = "SenderQyWxGroupRobotMsg";

    public static void sendMsg(final long logId, final Handler handError, final RetryIntercepter retryInterceptor, String webHook, String from, String content) throws Exception {
        Log.i(TAG, "sendMsg webHook:" + webHook + " from:" + from + " content:" + content);

        if (webHook == null || webHook.isEmpty()) {
            return;
        }

        Map textMsgMap = new HashMap();
        textMsgMap.put("msgtype", "text");
        Map textText = new HashMap();
        textText.put("content", content);
        textMsgMap.put("text", textText);

        Log.i(TAG, "requestUrl:" + webHook);
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
                .url(webHook)
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
