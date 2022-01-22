package com.idormy.sms.forwarder.sender;

import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;

import com.idormy.sms.forwarder.utils.LogUtil;

import java.io.IOException;
import java.util.Objects;

import io.reactivex.rxjava3.core.ObservableEmitter;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SenderServerChanMsg extends SenderBaseMsg {

    static final String TAG = "SenderServerChanMsg";

    public static void sendMsg(final long logId, final Handler handError, final ObservableEmitter<Object> emitter, final String sendKey, final String title, final String desp) throws Exception {
        Log.i(TAG, "sendMsg sendKey:" + sendKey + " title:" + title + " desp:" + desp);

        if (sendKey == null || sendKey.isEmpty()) {
            return;
        }

        final String requestUrl = "https://sctapi.ftqq.com/" + sendKey + ".send";
        Log.i(TAG, "requestUrl:" + requestUrl);
        //特殊处理避免标题重复
        final String requestMsg = desp.replaceFirst("^" + title + "(.*)", "").trim();
        Log.i(TAG, "requestMsg:" + requestMsg);

        OkHttpClient client = new OkHttpClient().newBuilder().build();
        MultipartBody.Builder builder = new MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("title", title)
                .addFormDataPart("desp", requestMsg);

        RequestBody body = builder.build();
        Request request = new Request.Builder().url(requestUrl).method("POST", body).build();
        Call call = client.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull final IOException e) {
                LogUtil.updateLog(logId, 0, e.getMessage());
                Toast(handError, TAG, "发送失败：" + e.getMessage());
                if (emitter != null) emitter.onError(new Exception("RxJava 请求接口异常..."));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                final String responseStr = Objects.requireNonNull(response.body()).string();
                Log.d(TAG, "Response：" + response.code() + "，" + responseStr);
                Toast(handError, TAG, "发送状态：" + responseStr);

                //TODO:粗略解析是否发送成功
                if (responseStr.contains("\"code\":0")) {
                    LogUtil.updateLog(logId, 2, responseStr);
                } else {
                    LogUtil.updateLog(logId, 0, responseStr);
                }
            }
        });

    }

}
