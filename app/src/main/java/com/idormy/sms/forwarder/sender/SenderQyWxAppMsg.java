package com.idormy.sms.forwarder.sender;

import static com.idormy.sms.forwarder.SenderActivity.NOTIFY;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.idormy.sms.forwarder.MyApplication;
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
public class SenderQyWxAppMsg extends SenderBaseMsg {

    static final String TAG = "SenderQyWxAppMsg";

    public static void sendMsg(final long logId, final Handler handError, String corpID, String agentID, String secret, String toUser, String content, boolean forceRefresh) throws Exception {
        Log.i(TAG, "sendMsg corpID:" + corpID + " agentID:" + agentID + " secret:" + secret + " toUser:" + toUser + " content:" + content + " forceRefresh:" + forceRefresh);

        if (corpID == null || corpID.isEmpty() || agentID == null || agentID.isEmpty() || secret == null || secret.isEmpty()) {
            return;
        }

        //TODO:判断access_token是否失效
        if (forceRefresh
                || MyApplication.QyWxAccessToken == null || MyApplication.QyWxAccessToken.isEmpty()
                || System.currentTimeMillis() > MyApplication.QyWxAccessTokenExpiresIn) {
            String getTokenUrl = "https://qyapi.weixin.qq.com/cgi-bin/gettoken?";
            getTokenUrl += "corpid=" + corpID;
            getTokenUrl += "&corpsecret=" + secret;
            Log.d(TAG, "getTokenUrl：" + getTokenUrl);

            OkHttpClient client = new OkHttpClient();
            final Request request = new Request.Builder().url(getTokenUrl).get().build();
            Call call = client.newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull final IOException e) {
                    LogUtil.updateLog(logId, 0, e.getMessage());
                    Log.d(TAG, "onFailure：" + e.getMessage());
                    if (handError != null) {
                        Message msg = new Message();
                        msg.what = NOTIFY;
                        Bundle bundle = new Bundle();
                        bundle.putString("DATA", "获取access_token失败：" + e.getMessage());
                        msg.setData(bundle);
                        handError.sendMessage(msg);
                    }
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    final String json = Objects.requireNonNull(response.body()).string();
                    Log.d(TAG, "Code：" + response.code() + " Response: " + json);
                    JSONObject jsonObject = JSON.parseObject(json);
                    int errcode = jsonObject.getInteger("errcode");
                    if (errcode == 0) {
                        MyApplication.QyWxAccessToken = jsonObject.getString("access_token");
                        MyApplication.QyWxAccessTokenExpiresIn = System.currentTimeMillis() + (jsonObject.getInteger("expires_in") - 120) * 1000L; //提前2分钟过期
                        Log.d(TAG, "access_token：" + MyApplication.QyWxAccessToken);
                        Log.d(TAG, "expires_in：" + MyApplication.QyWxAccessTokenExpiresIn);

                        sendTextMsg(logId, handError, agentID, toUser, content);
                    } else {
                        String errmsg = jsonObject.getString("errmsg");
                        LogUtil.updateLog(logId, 0, errmsg);
                        Log.d(TAG, "onFailure：" + errmsg);
                        if (handError != null) {
                            Message msg = new Message();
                            msg.what = NOTIFY;
                            Bundle bundle = new Bundle();
                            bundle.putString("DATA", "获取access_token失败：" + errmsg);
                            msg.setData(bundle);
                            handError.sendMessage(msg);
                        }
                    }
                }

            });
        } else {
            sendTextMsg(logId, handError, agentID, toUser, content);
        }

    }

    //发送文本消息
    public static void sendTextMsg(final long logId, final Handler handError, String agentID, String toUser, String content) {

        Map textMsgMap = new HashMap();
        textMsgMap.put("touser", toUser);
        textMsgMap.put("msgtype", "text");
        textMsgMap.put("agentid", agentID);

        Map textText = new HashMap();
        textText.put("content", content);
        textMsgMap.put("text", textText);

        final String requestUrl = "https://qyapi.weixin.qq.com/cgi-bin/message/send?access_token=" + MyApplication.QyWxAccessToken;
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