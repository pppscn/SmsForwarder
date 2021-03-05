package com.idormy.sms.forwarder.utils;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Base64;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.idormy.sms.forwarder.MyApplication;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

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

    public static void sendMsg(String msg) throws Exception {

        String webhook_token = "https://oapi.dingtalk.com/robot/send?access_token=" + SettingUtil.get_using_dingding_token();
        String webhook_secret = SettingUtil.get_using_dingding_secret();
        if (webhook_token.equals("")) {
            return;
        }
        if (!webhook_secret.equals("")) {
            Long timestamp = System.currentTimeMillis();

            String stringToSign = timestamp + "\n" + webhook_secret;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhook_secret.getBytes("UTF-8"), "HmacSHA256"));
            byte[] signData = mac.doFinal(stringToSign.getBytes("UTF-8"));
            String sign = URLEncoder.encode(new String(Base64.encode(signData, Base64.NO_WRAP)), "UTF-8");
            webhook_token += "&timestamp=" + timestamp + "&sign=" + sign;
            Log.i(TAG, "webhook_token:" + webhook_token);

        }

        final String msgf = msg;
        String textMsg = "{ \"msgtype\": \"text\", \"text\": {\"content\": \"" + msg + "\"}}";
        OkHttpClient client = new OkHttpClient();
        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json;charset=utf-8"),
                textMsg);

        final Request request = new Request.Builder()
                .url(webhook_token)
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .post(requestBody)
                .build();
        Call call = client.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.d(TAG, "onFailure：" + e.getMessage());
                SendHistory.addHistory("钉钉转发:" + msgf + "onFailure：" + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseStr = response.body().string();
                Log.d(TAG, "Code：" + String.valueOf(response.code()) + responseStr);
                SendHistory.addHistory("钉钉转发:" + msgf + "Code：" + String.valueOf(response.code()) + responseStr);
            }
        });
    }

    public static void sendMsg(final Handler handError, String corpID, String agentID, String secret, String toUser, String content, boolean forceRefresh) throws Exception {
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

                        sendTextMsg(handError, agentID, toUser, content);
                    } else {
                        String errmsg = jsonObject.getString("errmsg");
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
            sendTextMsg(handError, agentID, toUser, content);
        }

    }

    //发送文本消息
    public static void sendTextMsg(final Handler handError, String agentID, String toUser, String content) {
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