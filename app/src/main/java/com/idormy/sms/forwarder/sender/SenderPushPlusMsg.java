package com.idormy.sms.forwarder.sender;

import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;

import com.alibaba.fastjson.JSON;
import com.idormy.sms.forwarder.model.vo.PushPlusSettingVo;
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
public class SenderPushPlusMsg extends SenderBaseMsg {

    static final String TAG = "SenderPushPlusMsg";

    public static void sendMsg(final long logId, final Handler handError, final RetryIntercepter retryInterceptor, PushPlusSettingVo pushPlusSettingVo, String title, String content) throws Exception {

        //用户令牌
        String token = pushPlusSettingVo.getToken();
        if (token == null || token.isEmpty()) return;

        Map textMsgMap = new HashMap();

        //消息标题
        if (title != null && !title.isEmpty()) textMsgMap.put("title", title);

        //具体消息内容
        if (content == null || content.isEmpty()) return;
        textMsgMap.put("content", content);

        //群组编码，不填仅发送给自己；channel为webhook时无效
        String topic = pushPlusSettingVo.getTopic();
        if (topic != null && !topic.isEmpty()) textMsgMap.put("topic", topic);

        //发送模板
        String template = pushPlusSettingVo.getTemplate();
        if (template != null && !template.isEmpty()) textMsgMap.put("template", template);

        //发送渠道
        String channel = pushPlusSettingVo.getChannel();
        if (channel != null && !channel.isEmpty()) textMsgMap.put("channel", channel);

        //webhook编码，仅在channel使用webhook渠道和CP渠道时需要填写
        String webhook = pushPlusSettingVo.getChannel();
        if (webhook != null && !webhook.isEmpty()) textMsgMap.put("webhook", webhook);

        //发送结果回调地址
        String callbackUrl = pushPlusSettingVo.getCallbackUrl();
        if (callbackUrl != null && !callbackUrl.isEmpty()) textMsgMap.put("callbackUrl", callbackUrl);

        //毫秒时间戳。格式如：1632993318000。服务器时间戳大于此时间戳，则消息不会发送
        String validTime = pushPlusSettingVo.getValidTime();
        if (validTime != null && !validTime.isEmpty() && Integer.parseInt(validTime) > 0) {
            textMsgMap.put("timestamp", System.currentTimeMillis() + Integer.parseInt(validTime) * 1000L);
        }

        final String requestUrl = "http://www.pushplus.plus/send/" + token;
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
                if (responseStr.contains("\"code\":200")) {
                    LogUtils.updateLog(logId, 2, responseStr);
                } else {
                    LogUtils.updateLog(logId, 0, responseStr);
                }
            }
        });

    }

}
