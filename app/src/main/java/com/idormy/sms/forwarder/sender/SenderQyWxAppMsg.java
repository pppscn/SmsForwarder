package com.idormy.sms.forwarder.sender;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.idormy.sms.forwarder.MyApplication;
import com.idormy.sms.forwarder.utils.LogUtil;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static com.idormy.sms.forwarder.SenderActivity.NOTIFY;

public class SenderQyWxAppMsg {

    static String TAG = "SenderQyWxAppMsg";

    public static void sendMsg(final long logId, final Handler handError, String corpID, String agentID, String secret, String toUser, String content, boolean forceRefresh) throws Exception {
        Log.i(TAG, "sendMsg corpID:" + corpID + " agentID:" + agentID + " secret:" + secret + " toUser:" + toUser + " content:" + content + " forceRefresh:" + forceRefresh);

        if (corpID == null || corpID.isEmpty() || agentID == null || agentID.isEmpty() || secret == null || secret.isEmpty()) {
            return;
        }

        //TODO:判断access_token是否失效
        if (forceRefresh == true
                || MyApplication.QyWxAccessToken == null || MyApplication.QyWxAccessToken.isEmpty()
                || System.currentTimeMillis() > MyApplication.QyWxAccessTokenExpiresIn) {
            String gettokenUrl = "https://qyapi.weixin.qq.com/cgi-bin/gettoken?";
            gettokenUrl += "corpid=" + corpID;
            gettokenUrl += "&corpsecret=" + secret;
            Log.d(TAG, "gettokenUrl：" + gettokenUrl);

            OkHttpClient client = new OkHttpClient();
            final Request request = new Request.Builder().url(gettokenUrl).get().build();
            Call call = client.newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, final IOException e) {
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
                public void onResponse(Call call, Response response) throws IOException {
                    final String json = response.body().string();
                    Log.d(TAG, "Code：" + response.code() + " Response: " + json);
                    JSONObject jsonObject = JSON.parseObject(json);
                    int errcode = jsonObject.getInteger("errcode");
                    if (errcode == 0) {
                        MyApplication.QyWxAccessToken = jsonObject.getString("access_token");
                        MyApplication.QyWxAccessTokenExpiresIn = System.currentTimeMillis() + (jsonObject.getInteger("expires_in") - 120) * 1000; //提前2分钟过期
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
        String sendUrl = "https://qyapi.weixin.qq.com/cgi-bin/message/send?access_token=" + MyApplication.QyWxAccessToken;
        Log.d(TAG, "sendUrl：" + sendUrl);

        Map textMsgMap = new HashMap();
        textMsgMap.put("touser", toUser);
        textMsgMap.put("msgtype", "text");
        textMsgMap.put("agentid", agentID);

        Map textText = new HashMap();
        textText.put("content", content);
        textMsgMap.put("text", textText);

        String textMsg = JSON.toJSONString(textMsgMap);
        Log.d(TAG, "textMsg：" + textMsg);

        OkHttpClient client = new OkHttpClient();
        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json;charset=utf-8"), textMsg);

        final Request request = new Request.Builder()
                .url(sendUrl)
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .post(requestBody)
                .build();
        Call call = client.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, final IOException e) {
                LogUtil.updateLog(logId, 0, e.getMessage());
                Log.d(TAG, "onFailure：" + e.getMessage());
                if (handError != null) {
                    android.os.Message msg = new android.os.Message();
                    msg.what = NOTIFY;
                    Bundle bundle = new Bundle();
                    bundle.putString("DATA", "发送失败：" + e.getMessage());
                    msg.setData(bundle);
                    handError.sendMessage(msg);
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseStr = response.body().string();
                Log.d(TAG, "Code：" + String.valueOf(response.code()) + " Response: " + responseStr);

                //TODO:粗略解析是否发送成功
                if (responseStr.contains("\"errcode\":0")) {
                    LogUtil.updateLog(logId, 1, responseStr);
                } else {
                    LogUtil.updateLog(logId, 0, responseStr);
                }

                if (handError != null) {
                    android.os.Message msg = new android.os.Message();
                    msg.what = NOTIFY;
                    Bundle bundle = new Bundle();
                    bundle.putString("DATA", "发送状态：" + responseStr);
                    msg.setData(bundle);
                    handError.sendMessage(msg);
                    Log.d(TAG, "Code：" + String.valueOf(response.code()) + " Response: " + responseStr);
                }

            }
        });
    }

}