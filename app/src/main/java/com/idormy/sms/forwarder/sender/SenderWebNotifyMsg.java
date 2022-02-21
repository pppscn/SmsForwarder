package com.idormy.sms.forwarder.sender;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;

import com.alibaba.fastjson.JSON;
import com.idormy.sms.forwarder.model.vo.SmsVo;
import com.idormy.sms.forwarder.utils.CertUtils;
import com.idormy.sms.forwarder.utils.Define;
import com.idormy.sms.forwarder.utils.LogUtil;
import com.idormy.sms.forwarder.utils.SettingUtil;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@SuppressWarnings({"deprecation"})
public class SenderWebNotifyMsg extends SenderBaseMsg {

    static final String TAG = "SenderWebNotifyMsg";

    public static void sendMsg(final long logId, final Handler handError, final RetryIntercepter retryInterceptor, String webServer, String webParams, String secret, String method, SmsVo smsVo, String smsTemplate, String regexReplace) throws Exception {
        String from = smsVo.getMobile();
        String content = smsVo.getSmsVoForSend(smsTemplate, regexReplace);
        Log.i(TAG, "sendMsg webServer:" + webServer + " webParams:" + webParams + " from:" + from + " content:" + content);

        if (webServer == null || webServer.isEmpty()) {
            return;
        }

        Long timestamp = System.currentTimeMillis();
        String orgContent = smsVo.getContent();
        String deviceMark = SettingUtil.getAddExtraDeviceMark().trim();
        String appVersion = SettingUtil.getVersionName();
        String simInfo = smsVo.getSimInfo();
        @SuppressLint("SimpleDateFormat") String receiveTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(smsVo.getDate());

        String sign = "";
        if (secret != null && !secret.isEmpty()) {
            String stringToSign = timestamp + "\n" + secret;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] signData = mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8));
            sign = URLEncoder.encode(new String(Base64.encode(signData, Base64.NO_WRAP)), "UTF-8");
            Log.i(TAG, "sign:" + sign);
        }

        Request request;
        if (method.equals("GET") && TextUtils.isEmpty(webParams)) {
            webServer += (webServer.contains("?") ? "&" : "?") + "from=" + URLEncoder.encode(from, "UTF-8");
            webServer += "&content=" + URLEncoder.encode(content, "UTF-8");
            if (secret != null && !secret.isEmpty()) {
                webServer += "&timestamp=" + timestamp;
                webServer += "&sign=" + sign;
            }

            Log.d(TAG, "method = GET, Url = " + webServer);
            request = new Request.Builder().url(webServer).get().build();
        } else if (method.equals("GET") && !TextUtils.isEmpty(webParams)) {
            webParams = webParams.replace("[from]", URLEncoder.encode(from, "UTF-8"))
                    .replace("[content]", URLEncoder.encode(content, "UTF-8"))
                    .replace("[msg]", URLEncoder.encode(content, "UTF-8"))
                    .replace("[org_content]", URLEncoder.encode(orgContent, "UTF-8"))
                    .replace("[device_mark]", URLEncoder.encode(deviceMark, "UTF-8"))
                    .replace("[app_version]", URLEncoder.encode(appVersion, "UTF-8"))
                    .replace("[title]", URLEncoder.encode(simInfo, "UTF-8"))
                    .replace("[card_slot]", URLEncoder.encode(simInfo, "UTF-8"))
                    .replace("[receive_time]", URLEncoder.encode(receiveTime, "UTF-8"))
                    .replace("\n", "%0A");
            if (secret != null && !secret.isEmpty()) {
                webParams = webParams.replace("[timestamp]", String.valueOf(timestamp))
                        .replace("[sign]", URLEncoder.encode(sign, "UTF-8"));
            }
            webServer += (webServer.contains("?") ? "&" : "?") + webParams;

            Log.d(TAG, "method = GET, Url = " + webServer);
            request = new Request.Builder().url(webServer).get().build();
        } else if (webParams != null && webParams.contains("[msg]")) {
            String bodyMsg;
            String contentType = "application/x-www-form-urlencoded";
            if (webParams.startsWith("{")) {
                contentType = "application/json;charset=utf-8";
                bodyMsg = webParams.replace("[from]", from)
                        .replace("[content]", escapeJson(content))
                        .replace("[msg]", escapeJson(content))
                        .replace("[org_content]", escapeJson(orgContent))
                        .replace("[device_mark]", escapeJson(deviceMark))
                        .replace("[app_version]", appVersion)
                        .replace("[title]", escapeJson(simInfo))
                        .replace("[card_slot]", escapeJson(simInfo))
                        .replace("[receive_time]", receiveTime);
            } else {
                bodyMsg = webParams.replace("[from]", URLEncoder.encode(from, "UTF-8"))
                        .replace("[content]", URLEncoder.encode(content, "UTF-8"))
                        .replace("[msg]", URLEncoder.encode(content, "UTF-8"))
                        .replace("[org_content]", URLEncoder.encode(orgContent, "UTF-8"))
                        .replace("[device_mark]", URLEncoder.encode(deviceMark, "UTF-8"))
                        .replace("[app_version]", URLEncoder.encode(appVersion, "UTF-8"))
                        .replace("[title]", URLEncoder.encode(simInfo, "UTF-8"))
                        .replace("[card_slot]", URLEncoder.encode(simInfo, "UTF-8"))
                        .replace("[receive_time]", URLEncoder.encode(receiveTime, "UTF-8"));
            }
            RequestBody body = RequestBody.create(MediaType.parse(contentType), bodyMsg);
            request = new Request.Builder()
                    .url(webServer)
                    .addHeader("Content-Type", contentType)
                    .method("POST", body)
                    .build();
            Log.d(TAG, "method = POST webParams, Body = " + bodyMsg);
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

        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder();
        //设置重试拦截器
        if (retryInterceptor != null) clientBuilder.addInterceptor(retryInterceptor);
        //忽略https证书
        clientBuilder.sslSocketFactory(CertUtils.getSSLSocketFactory(), CertUtils.getX509TrustManager()).hostnameVerifier(CertUtils.getHostnameVerifier());
        //设置读取超时时间
        OkHttpClient client = clientBuilder
                .readTimeout(Define.REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .writeTimeout(Define.REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .connectTimeout(Define.REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull final IOException e) {
                LogUtil.updateLog(logId, 0, e.getMessage());
                Toast(handError, TAG, "发送失败：" + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                final String responseStr = Objects.requireNonNull(response.body()).string();
                Log.d(TAG, "Response：" + response.code() + "，" + responseStr);
                Toast(handError, TAG, "发送状态：" + responseStr);

                //返回http状态200即为成功
                if (200 == response.code()) {
                    LogUtil.updateLog(logId, 2, responseStr);
                } else {
                    LogUtil.updateLog(logId, 0, responseStr);
                }
            }
        });

    }

    //JSON需要转义的字符
    private static String escapeJson(String str) {
        if (str == null) return "null";

        String jsonStr = JSON.toJSONString(str);
        return jsonStr.length() >= 2 ? jsonStr.substring(1, jsonStr.length() - 1) : jsonStr;
    }

}