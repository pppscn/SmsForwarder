package com.idormy.sms.forwarder.sender;

import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;

import com.alibaba.fastjson.JSON;
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

@SuppressWarnings({"rawtypes", "unchecked", "deprecation", "ResultOfMethodCallIgnored"})
public class SenderQyWxGroupRobotMsg extends SenderBaseMsg {

    static final String TAG = "SenderQyWxGroupRobotMsg";

    public static void sendMsg(final long logId, final Handler handError, String webHook, String from, String content) throws Exception {
        Log.i(TAG, "sendMsg webHook:" + webHook + " from:" + from + " content:" + content);

        if (webHook == null || webHook.isEmpty()) {
            return;
        }

        Map textMsgMap = new HashMap();
        textMsgMap.put("msgtype", "text");
        Map textText = new HashMap();
        textText.put("content", content);
        textMsgMap.put("text", textText);

        final String requestUrl = webHook;
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
                            if (responseStr.contains("\"errcode\":0")) {
                                LogUtil.updateLog(logId, 1, responseStr);
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
