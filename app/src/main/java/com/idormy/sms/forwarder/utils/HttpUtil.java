package com.idormy.sms.forwarder.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.hjq.toast.ToastUtils;
import com.idormy.sms.forwarder.sender.RetryIntercepter;

import java.io.IOException;
import java.net.URLEncoder;
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

@SuppressWarnings("unchecked")
public class HttpUtil {
    private static OkHttpClient client;
    private static OkHttpClient retryClient;
    private static final String TAG = "HttpUtil";
    private static Boolean hasInit = false;
    @SuppressLint("StaticFieldLeak")
    private static Context context;
    private static final MediaType MEDIA_TYPE_JSON = MediaType.parse("application/json;charset=utf-8");

    @SuppressLint("HandlerLeak")
    public static void init(Context context) {
        //noinspection SynchronizeOnNonFinalField
        synchronized (hasInit) {
            if (hasInit) return;

            hasInit = true;
            HttpUtil.context = context;
            OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder();
            //设置读取超时时间
            clientBuilder
                    .readTimeout(Define.REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .writeTimeout(Define.REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .connectTimeout(Define.REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            client = clientBuilder.build();
            //设置重试拦截器
            int retryTimes = SettingUtil.getRetryTimes();
            int delayTime = SettingUtil.getDelayTime();
            if (retryTimes > 0)
                clientBuilder.addInterceptor(new RetryIntercepter.Builder().executionCount(retryTimes).retryInterval(delayTime).build());
            retryClient = clientBuilder.build();
        }
    }

    public static void asyncGet(String tag, String url, Object param, Lamda.Consumer<Response> onResponse, Lamda.Consumer<Exception> onFailure, boolean doRetry) {
        StringBuilder resUrl = appendQueryStr(tag, url, param);
        Request request = new Request.Builder().url(resUrl.toString()).get().build();
        Lamda.Func<Call, String> func = call -> {
            call.enqueue(new Callback0(tag, onResponse, onFailure));
            return null;
        };
        callAndCatch(tag, request, func, doRetry);
    }

    public static void asyncPostJson(String tag, String url, Object param, Lamda.Consumer<Response> onResponse, Lamda.Consumer<Exception> onFailure, boolean doRetry) {
        String jsonString = JSON.toJSONString(param);
        Request request = new Request.Builder().url(url).post(RequestBody.create(jsonString, MEDIA_TYPE_JSON)).build();
        Lamda.Func<Call, String> func = call -> {
            call.enqueue(new Callback0(tag, onResponse, onFailure));
            return null;
        };
        callAndCatch(tag, request, func, doRetry);
    }

    public static String postJson(String tag, String url, Object param, boolean doRetry) {
        String jsonString = JSON.toJSONString(param);
        Request request = new Request.Builder().url(url).post(RequestBody.create(jsonString, MEDIA_TYPE_JSON)).build();
        Lamda.Func<Call, String> func = call -> {
            Response response = call.execute();
            if (response.code() == 200) {
                return Objects.requireNonNull(response.body()).toString();
            }
            return null;
        };
        return callAndCatch(tag, request, func, doRetry);
    }

    public static String get(String tag, String url, Object param, boolean doRetry) {
        StringBuilder resUrl = appendQueryStr(tag, url, param);
        Request request = new Request.Builder().url(resUrl.toString()).get().build();
        Lamda.Func<Call, String> func = call -> {
            Response response = call.execute();
            if (response.code() == 200) {
                return Objects.requireNonNull(response.body()).toString();
            }
            return null;
        };
        return callAndCatch(tag, request, func, doRetry);
    }

    public static void Toast(String Tag, String data) {
        Log.i(Tag, data);
        try {
            ToastUtils.delayedShow(Tag + "-" + data, 3000);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("deprecation")
    @NonNull
    public static StringBuilder appendQueryStr(String tag, String url, Object param) {
        StringBuilder resUrl = new StringBuilder(url);
        if (!url.contains("?")) {
            resUrl.append("?");
        } else {
            resUrl.append("&");
        }
        Map<String, String> paramMap = param instanceof Map ? (Map<String, String>) param
                : JSON.parseObject(JSON.toJSONString(param), new TypeReference<Map<String, String>>() {
        }.getType());
        for (Map.Entry<String, String> entry : paramMap.entrySet()) {
            if (entry.getValue() != null) {
                resUrl.append(URLEncoder.encode(entry.getKey())).append("=").append(URLEncoder.encode(entry.getValue()));
            }
        }
        Log.i(tag, "url:" + resUrl);
        return resUrl;
    }

    public static String callAndCatch(String tag, Request request, Lamda.Func<Call, String> func, boolean doRetry) {
        try {
            Call call = (doRetry ? retryClient : client).newCall(request);
            return func.execute(call);
        } catch (Exception e) {
            Toast(tag, "请求失败：" + e.getMessage());
            Log.e(tag, "请求失败：" + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public static class Callback0 implements Callback {
        public Callback0(String tag, Lamda.Consumer<Response> onResponse, Lamda.Consumer<Exception> onFailure) {
            this.tag = tag;
            this.onResponse = onResponse;
            this.onFailure = onFailure;
        }

        public Callback0(String tag, Lamda.Consumer<Response> onResponse) {
            this.tag = tag;
            this.onResponse = onResponse;
        }

        private final String tag;
        private final Lamda.Consumer<Response> onResponse;
        private Lamda.Consumer<Exception> onFailure;

        @Override
        public void onFailure(@NonNull Call call, @NonNull final IOException e) {
            Toast(tag, "onFailure：" + e.getMessage());
            Log.d(tag, "onFailure：" + e.getMessage());
            if (onFailure != null) {
                onFailure.executeThrowRunTimeExcp(e);
            }
        }

        @Override
        public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
            Log.d(tag, "onResponse：" + response.code() + ":" + JSON.toJSONString(Objects.requireNonNull(response.body())));
            if (onResponse != null)
                onResponse.executeThrowRunTimeExcp(response);
        }

        public String getTag() {
            return tag;
        }
    }

}
