package com.idormy.sms.forwarder.sender;

import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import com.alibaba.fastjson.JSON;
import com.idormy.sms.forwarder.model.vo.TelegramSettingVo;
import com.idormy.sms.forwarder.utils.Define;
import com.idormy.sms.forwarder.utils.LogUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Credentials;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@SuppressWarnings({"rawtypes", "unchecked", "deprecation"})
public class SenderTelegramMsg extends SenderBaseMsg {

    static final String TAG = "SenderTelegramMsg";

    public static void sendMsg(final long logId, final Handler handError, final RetryIntercepter retryInterceptor, TelegramSettingVo telegramSettingVo, final String from, final String text, final String method) throws Exception {
        Log.i(TAG, "sendMsg telegramSettingVo:" + telegramSettingVo.toString() + " text:" + text);

        String apiToken = telegramSettingVo.getApiToken();
        String chatId = telegramSettingVo.getChatId();
        if (apiToken == null || apiToken.isEmpty()) {
            return;
        }

        final String finalText = text.trim(); //.replaceAll("#", "井")

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

        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder();
        //设置代理
        if ((proxyType == Proxy.Type.HTTP || proxyType == Proxy.Type.SOCKS) && !TextUtils.isEmpty(proxyHost) && !TextUtils.isEmpty(proxyPort)) {
            //代理服务器的IP和端口号
            clientBuilder.proxy(new Proxy(proxyType, new InetSocketAddress(proxyHost, Integer.parseInt(proxyPort))));

            //代理的鉴权账号密码
            if (needProxyAuthenticator && (!TextUtils.isEmpty(proxyUsername) || !TextUtils.isEmpty(proxyPassword))) {
                clientBuilder.proxyAuthenticator((route, response) -> {
                    //设置代理服务器账号密码
                    String credential = Credentials.basic(proxyUsername, proxyPassword);
                    return response.request().newBuilder()
                            .header("Proxy-Authorization", credential)
                            .build();
                });
            }
        }
        //设置重试拦截器
        if (retryInterceptor != null) clientBuilder.addInterceptor(retryInterceptor);
        //设置读取超时时间
        OkHttpClient client = clientBuilder
                .readTimeout(Define.REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .writeTimeout(Define.REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .connectTimeout(Define.REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .build();

        final Request request;
        if (method != null && method.equals("GET")) {
            request = new Request.Builder()
                    .url(requestUrl + "?chat_id=" + chatId + "&text=" + URLEncoder.encode(finalText, "UTF-8"))
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
                LogUtils.updateLog(logId, 0, e.getMessage());
                Toast(handError, TAG, "发送失败：" + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                final String responseStr = Objects.requireNonNull(response.body()).string();
                Log.d(TAG, "Response：" + response.code() + "，" + responseStr);
                Toast(handError, TAG, "发送状态：" + responseStr);

                //TODO:粗略解析是否发送成功
                if (responseStr.contains("\"ok\":true")) {
                    LogUtils.updateLog(logId, 2, responseStr);
                } else {
                    LogUtils.updateLog(logId, 0, responseStr);
                }
            }
        });

    }

}
