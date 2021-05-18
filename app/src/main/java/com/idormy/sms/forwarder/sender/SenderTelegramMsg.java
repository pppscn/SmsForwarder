package com.idormy.sms.forwarder.sender;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
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

public class SenderTelegramMsg {

    static String TAG = "SenderTelegramMsg";

    public static void sendMsg(final long logId, final Handler handError, String apiToken, String chatId, String from, String text) throws Exception {
        Log.i(TAG, "sendMsg apiToken:" + apiToken + " chatId:" + chatId + " text:" + text);

        if (apiToken == null || apiToken.isEmpty()) {
            return;
        }

        //特殊处理避免标题重复
        text = text.replaceFirst("^" + from + "(.*)", "").replaceAll("#", "井").trim();

        String sendUrl = "https://api.telegram.org/bot" + apiToken + "/sendMessage";
        Log.d(TAG, "sendUrl：" + sendUrl);

        Map bodyMap = new HashMap();
        bodyMap.put("chat_id", chatId);
        bodyMap.put("text", text);
        bodyMap.put("parse_mode", "HTML");
        String bodyMsg = JSON.toJSONString(bodyMap);
        Log.d(TAG, "body：" + bodyMsg);

        OkHttpClient client = new OkHttpClient();
        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json;charset=utf-8"), bodyMsg);

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
                    Message msg = new Message();
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
                Log.d(TAG, "Code：" + response.code() + responseStr);

                //TODO:粗略解析是否发送成功
                if (responseStr.contains("\"ok\":true")) {
                    LogUtil.updateLog(logId, 1, responseStr);
                } else {
                    LogUtil.updateLog(logId, 0, responseStr);
                }

                if (handError != null) {
                    Message msg = new Message();
                    msg.what = NOTIFY;
                    Bundle bundle = new Bundle();
                    bundle.putString("DATA", "发送状态：" + responseStr);
                    msg.setData(bundle);
                    handError.sendMessage(msg);
                    Log.d(TAG, "Response：" + response.code() + responseStr);
                }

            }
        });
    }

}
