package com.idormy.sms.forwarder.sender;

import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;

import com.idormy.sms.forwarder.model.vo.GotifySettingVo;
import com.idormy.sms.forwarder.utils.LogUtil;

import java.io.IOException;
import java.util.Objects;

import io.reactivex.rxjava3.core.ObservableEmitter;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SenderGotifyMsg extends SenderBaseMsg {

    static final String TAG = "SenderGotifyMsg";

    public static void sendMsg(final long logId, final Handler handError, final ObservableEmitter<Object> emitter, GotifySettingVo gotifySettingVo, String title, String message) throws Exception {

        //具体消息内容
        if (message == null || message.isEmpty()) return;

        RequestBody formBody = new FormBody.Builder()
                .add("title", title)
                .add("message", message)
                .add("priority", gotifySettingVo.getPriority())
                .build();

        String requestUrl = gotifySettingVo.getWebServer();
        Log.i(TAG, "requestUrl:" + requestUrl);

        Request request = new Request.Builder().url(requestUrl).post(formBody).build();
        OkHttpClient client = new OkHttpClient();
        Call call = client.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull final IOException e) {
                LogUtil.updateLog(logId, 0, e.getMessage());
                Toast(handError, TAG, "发送失败：" + e.getMessage());
                if (emitter != null) emitter.onError(new RuntimeException("RxJava 请求接口异常..."));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                final String responseStr = Objects.requireNonNull(response.body()).string();
                Log.d(TAG, "Response：" + response.code() + "，" + responseStr);
                Toast(handError, TAG, "发送状态：" + responseStr);

                //TODO:粗略解析是否发送成功
                if (response.code() == 200) {
                    LogUtil.updateLog(logId, 2, responseStr);
                } else {
                    LogUtil.updateLog(logId, 1, responseStr);
                }
            }
        });

    }

}
