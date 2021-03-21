package com.idormy.sms.forwarder.sender;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.idormy.sms.forwarder.utils.LogUtil;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static com.idormy.sms.forwarder.SenderActivity.NOTIFY;

public class SenderServerChanMsg {

    static String TAG = "SenderServerChanMsg";

    public static void sendMsg(final long logId, final Handler handError, String sendKey, String title, String desp) throws Exception {
        Log.i(TAG, "sendMsg sendKey:" + sendKey + " title:" + title + " desp:" + desp);

        if (sendKey == null || sendKey.isEmpty()) {
            return;
        }

        //特殊处理避免标题重复
        desp = desp.replaceFirst("^" + title + "(.*)", "").trim();

        String sendUrl = "https://sctapi.ftqq.com/" + sendKey + ".send";

        OkHttpClient client = new OkHttpClient().newBuilder().build();
        MultipartBody.Builder builder = new MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("title", title)
                .addFormDataPart("desp", desp);

        RequestBody body = builder.build();
        Request request = new Request.Builder().url(sendUrl).method("POST", body).build();
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
                if (responseStr.contains("\"code\":0")) {
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
