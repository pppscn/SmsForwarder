package com.idormy.sms.forwarder.sender;

import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;

import com.alibaba.fastjson.JSON;
import com.idormy.sms.forwarder.model.vo.PushPlusSettingVo;
import com.idormy.sms.forwarder.utils.LogUtil;
import com.idormy.sms.forwarder.utils.SettingUtil;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableEmitter;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@SuppressWarnings({"ResultOfMethodCallIgnored", "rawtypes", "unchecked", "deprecation"})
public class SenderPushPlusMsg extends SenderBaseMsg {

    static final String TAG = "SenderPushPlusMsg";

    public static void sendMsg(final long logId, final Handler handError, PushPlusSettingVo pushPlusSettingVo, String title, String content) throws Exception {

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

        Observable
                .create((ObservableEmitter<Object> emitter) -> {
                    Toast(handError, TAG, "开始请求接口...");

                    OkHttpClient client = new OkHttpClient();
                    RequestBody requestBody = RequestBody.create(MediaType.parse("application/json;charset=utf-8"), requestMsg);

                    final Request request = new Request.Builder()
                            .url(requestUrl)
                            .addHeader("Content-Type", "application/json; charset=utf-8")
                            .post(requestBody)
                            .build();
                    Call call = client.newCall(request);
                    call.enqueue(new Callback() {
                        @Override
                        public void onFailure(@NonNull Call call, @NonNull final IOException e) {
                            LogUtil.updateLog(logId, 0, e.getMessage());
                            Toast(handError, TAG, "发送失败：" + e.getMessage());
                            emitter.onError(new RuntimeException("请求接口异常..."));
                        }

                        @Override
                        public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                            final String responseStr = Objects.requireNonNull(response.body()).string();
                            Log.d(TAG, "Response：" + response.code() + "，" + responseStr);
                            Toast(handError, TAG, "发送状态：" + responseStr);

                            //TODO:粗略解析是否发送成功
                            if (responseStr.contains("\"code\":200")) {
                                LogUtil.updateLog(logId, 2, responseStr);
                            } else {
                                LogUtil.updateLog(logId, 0, responseStr);
                            }
                        }
                    });

                }).retryWhen((Observable<Throwable> errorObservable) -> errorObservable
                .zipWith(Observable.just(
                        SettingUtil.getRetryDelayTime(1),
                        SettingUtil.getRetryDelayTime(2),
                        SettingUtil.getRetryDelayTime(3),
                        SettingUtil.getRetryDelayTime(4),
                        SettingUtil.getRetryDelayTime(5)
                ), (Throwable e, Integer time) -> time)
                .flatMap((Integer delay) -> {
                    Toast(handError, TAG, "请求接口异常，" + delay + "秒后重试");
                    return Observable.timer(delay, TimeUnit.SECONDS);
                }))
                .subscribe(System.out::println);
    }

}
