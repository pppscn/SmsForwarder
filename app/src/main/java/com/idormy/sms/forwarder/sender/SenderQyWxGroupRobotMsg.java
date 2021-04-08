package com.idormy.sms.forwarder.sender;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import com.alibaba.fastjson.JSON;
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

public class SenderQyWxGroupRobotMsg {

    static String TAG = "SenderQyWxGroupRobotMsg";

    public static void sendMsg(final long logId, final Handler handError, String webHook, String from, String content) throws Exception {
        Log.i(TAG, "sendMsg webHook:" + webHook + " from:" + from + " content:" + content);

        if (webHook == null || webHook.isEmpty()) {
            return;
        }

        //String textMsg = "{ \"msgtype\": \"text\", \"text\": {\"content\": \"" + from + " : " + content + "\"}}";
        Map textMsgMap = new HashMap();
        textMsgMap.put("msgtype", "text");
        Map textText = new HashMap();
        textText.put("content", content);
        textMsgMap.put("text", textText);
        String textMsg = JSON.toJSONString(textMsgMap);
        Log.i(TAG, "textMsg:" + textMsg);

        OkHttpClient client = new OkHttpClient();
        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json;charset=utf-8"), textMsg);

        final Request request = new Request.Builder()
                .url(webHook)
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
                Log.d(TAG, "Code：" + String.valueOf(response.code()) + responseStr);

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
                    Log.d(TAG, "Coxxyyde：" + String.valueOf(response.code()) + responseStr);
                }
            }
        });
    }

}
