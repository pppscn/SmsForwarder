package com.idormy.sms.forwarder.sender;

import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import com.alibaba.fastjson.JSON;
import com.idormy.sms.forwarder.model.vo.TelegramSettingVo;
import com.idormy.sms.forwarder.utils.LogUtil;
import com.idormy.sms.forwarder.utils.SettingUtil;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableEmitter;
import okhttp3.Authenticator;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.ConnectionPool;
import okhttp3.Credentials;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@SuppressWarnings({"rawtypes", "unchecked", "deprecation", "ResultOfMethodCallIgnored"})
public class SenderTelegramMsg extends SenderBaseMsg {

    static final String TAG = "SenderTelegramMsg";

    public static void sendMsg(final long logId, final Handler handError, TelegramSettingVo telegramSettingVo, final String from, final String text, final String method) throws Exception {
        Log.i(TAG, "sendMsg telegramSettingVo:" + telegramSettingVo.toString() + " text:" + text);

        String apiToken = telegramSettingVo.getApiToken();
        String chatId = telegramSettingVo.getChatId();
        if (apiToken == null || apiToken.isEmpty()) {
            return;
        }

        //特殊处理避免标题重复
        final String finalText = text.replaceAll("#", "井").trim();

        if (!apiToken.startsWith("http")) {
            apiToken = "https://api.telegram.org/bot" + apiToken + "/sendMessage";
        }

        final String requestUrl = apiToken;
        Log.i(TAG, "requestUrl:" + requestUrl);

        //代理相关
        final Proxy.Type proxyType = telegramSettingVo.getProxyType();
        final String proxyHost = telegramSettingVo.getProxyHost();
        final String proxyPort = telegramSettingVo.getProxyPort();
        final Boolean needProxyAuthenticator = telegramSettingVo.getProxyAuthenticator();
        final String proxyUsername = telegramSettingVo.getProxyUsername();
        final String proxyPassword = telegramSettingVo.getProxyPassword();

        Observable
                .create((ObservableEmitter<Object> emitter) -> {
                    Toast(handError, TAG, "开始请求接口...");

                    try {
                        Proxy proxy = null;
                        Authenticator proxyAuthenticator = null;

                        if ((proxyType == Proxy.Type.HTTP || proxyType == Proxy.Type.SOCKS) && !TextUtils.isEmpty(proxyHost) && !TextUtils.isEmpty(proxyPort)) {
                            proxy = new Proxy(proxyType, new InetSocketAddress(proxyHost, Integer.parseInt(proxyPort)));

                            if (needProxyAuthenticator && (!TextUtils.isEmpty(proxyUsername) || !TextUtils.isEmpty(proxyPassword))) {
                                proxyAuthenticator = (route, response) -> {
                                    String credential = Credentials.basic("jesse", "password1");
                                    return response.request().newBuilder()
                                            .header("Authorization", credential)
                                            .build();
                                };
                            }
                        }

                        OkHttpClient client;
                        if (proxy != null && proxyAuthenticator != null) {
                            client = new OkHttpClient().newBuilder().proxy(proxy).proxyAuthenticator(proxyAuthenticator)
                                    .connectTimeout(120, TimeUnit.SECONDS).readTimeout(120, TimeUnit.SECONDS)
                                    .connectionPool(new ConnectionPool(5, 1, TimeUnit.SECONDS)).build();
                        } else if (proxy != null) {
                            client = new OkHttpClient().newBuilder().proxy(proxy)
                                    .connectTimeout(120, TimeUnit.SECONDS).readTimeout(120, TimeUnit.SECONDS)
                                    .connectionPool(new ConnectionPool(5, 1, TimeUnit.SECONDS)).build();
                        } else {
                            client = new OkHttpClient().newBuilder()
                                    .connectTimeout(120, TimeUnit.SECONDS).readTimeout(120, TimeUnit.SECONDS)
                                    .connectionPool(new ConnectionPool(5, 1, TimeUnit.SECONDS)).build();
                        }

                        final Request request;
                        if (method.equals("GET")) {
                            request = new Request.Builder()
                                    .url(requestUrl + "?chat_id=" + chatId + "&text=" + finalText)
                                    .build();
                        } else {
                            Map bodyMap = new HashMap();
                            bodyMap.put("chat_id", chatId);
                            bodyMap.put("text", finalText);
                            bodyMap.put("parse_mode", "HTML");

                            String requestMsg = JSON.toJSONString(bodyMap);
                            Log.i(TAG, "requestMsg:" + requestMsg);

                            RequestBody requestBody = RequestBody.create(MediaType.parse("application/json;charset=utf-8"), requestMsg);
                            request = new Request.Builder()
                                    .url(requestUrl)
                                    .addHeader("Content-Type", "application/json; charset=utf-8")
                                    .post(requestBody)
                                    .build();
                        }

                        client.newCall(request).enqueue(new Callback() {
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
                                if (responseStr.contains("\"ok\":true")) {
                                    LogUtil.updateLog(logId, 2, responseStr);
                                } else {
                                    LogUtil.updateLog(logId, 0, responseStr);
                                }
                            }
                        });

                    } catch (Exception e) {
                        LogUtil.updateLog(logId, 0, e.getMessage());
                        Log.e(TAG, e.getMessage(), e);
                        Toast(handError, TAG, "发送失败：" + e.getMessage());
                        emitter.onError(new RuntimeException("请求接口异常..."));
                    }
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
