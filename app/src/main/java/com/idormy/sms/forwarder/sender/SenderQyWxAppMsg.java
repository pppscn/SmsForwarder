package com.idormy.sms.forwarder.sender;

import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.idormy.sms.forwarder.model.SenderModel;
import com.idormy.sms.forwarder.model.vo.QYWXAppSettingVo;
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
public class SenderQyWxAppMsg extends SenderBaseMsg {

    static final String TAG = "SenderQyWxAppMsg";

    public static void sendMsg(final long logId, final Handler handError, final RetryIntercepter retryInterceptor, final SenderModel senderModel, final QYWXAppSettingVo qYWXAppSettingVo, String content) throws Exception {

        if (qYWXAppSettingVo == null) {
            Toast(handError, TAG, "参数错误");
            return;
        }

        String corpID = qYWXAppSettingVo.getCorpID();
        String agentID = qYWXAppSettingVo.getAgentID();
        String secret = qYWXAppSettingVo.getSecret();
        String toUser = qYWXAppSettingVo.getToUser();
        Boolean atAll = qYWXAppSettingVo.getAtAll();

        Log.i(TAG, "sendMsg corpID:" + corpID + " agentID:" + agentID + " secret:" + secret + " toUser:" + toUser + " content:" + content);

        if (corpID == null || corpID.isEmpty() || agentID == null || agentID.isEmpty() || secret == null || secret.isEmpty()) {
            return;
        }

        //TODO:获取有效access_token
        String accessToken = qYWXAppSettingVo.getAccessToken();
        if (accessToken == null || accessToken.isEmpty()) {

            String getTokenUrl = "https://qyapi.weixin.qq.com/cgi-bin/gettoken?";
            getTokenUrl += "corpid=" + corpID;
            getTokenUrl += "&corpsecret=" + secret;
            Log.d(TAG, "getTokenUrl：" + getTokenUrl);

            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            //设置重试拦截器
            if (retryInterceptor != null) builder.addInterceptor(retryInterceptor);
            //设置读取超时时间
            OkHttpClient client = builder
                    .readTimeout(Define.REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .writeTimeout(Define.REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .connectTimeout(Define.REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .build();

            final Request request = new Request.Builder().url(getTokenUrl).get().build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull final IOException e) {
                    LogUtils.updateLog(logId, 0, e.getMessage());
                    qYWXAppSettingVo.setAccessToken("");
                    qYWXAppSettingVo.setExpiresIn(0L);
                    if (senderModel != null) {
                        senderModel.setJsonSetting(JSON.toJSONString(qYWXAppSettingVo));
                        SenderUtil.updateSender(senderModel);
                    }
                    Log.d(TAG, "onFailure：" + e.getMessage());
                    Toast(handError, TAG, "获取access_token失败：" + e.getMessage());
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    final String json = Objects.requireNonNull(response.body()).string();
                    Log.d(TAG, "Code：" + response.code() + " Response: " + json);
                    JSONObject jsonObject = JSON.parseObject(json);
                    int errcode = jsonObject.getInteger("errcode");
                    if (errcode == 0) {
                        String access_token = jsonObject.getString("access_token");
                        long expires_in = System.currentTimeMillis() + (jsonObject.getInteger("expires_in") - 120) * 1000L; //提前2分钟过期
                        Log.d(TAG, "access_token：" + access_token);
                        Log.d(TAG, "expires_in：" + expires_in);

                        qYWXAppSettingVo.setAccessToken(access_token);
                        qYWXAppSettingVo.setExpiresIn(expires_in);
                        if (senderModel != null) {
                            senderModel.setJsonSetting(JSON.toJSONString(qYWXAppSettingVo));
                            SenderUtil.updateSender(senderModel);
                        }

                        sendTextMsg(retryInterceptor, logId, handError, agentID, toUser, content, access_token);
                    } else {
                        String errmsg = jsonObject.getString("errmsg");
                        LogUtils.updateLog(logId, 0, errmsg);
                        Log.d(TAG, "onFailure：" + errmsg);
                        Toast(handError, TAG, "获取access_token失败：" + errmsg);
                    }
                }

            });

        } else {
            sendTextMsg(retryInterceptor, logId, handError, agentID, toUser, content, accessToken);
        }

    }

    //发送文本消息
    public static void sendTextMsg(RetryIntercepter retryInterceptor, final long logId, final Handler handError, String agentID, String toUser, String content, String accessToken) {

        Map textMsgMap = new HashMap();
        textMsgMap.put("touser", toUser);
        textMsgMap.put("msgtype", "text");
        textMsgMap.put("agentid", agentID);

        Map textText = new HashMap();
        textText.put("content", content);
        textMsgMap.put("text", textText);

        final String requestUrl = "https://qyapi.weixin.qq.com/cgi-bin/message/send?access_token=" + accessToken;
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