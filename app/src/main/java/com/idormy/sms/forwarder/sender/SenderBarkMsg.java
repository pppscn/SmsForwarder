package com.idormy.sms.forwarder.sender;

import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;

import com.idormy.sms.forwarder.utils.LogUtil;
import com.idormy.sms.forwarder.utils.SettingUtil;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableEmitter;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class SenderBarkMsg extends SenderBaseMsg {

    static final String TAG = "SenderBarkMsg";

    public static void sendMsg(final long logId, final Handler handError, String barkServer, String barkIcon, String from, String content, String groupName) throws Exception {
        Log.i(TAG, "sendMsg barkServer:" + barkServer + " from:" + from + " content:" + content);

        if (barkServer == null || barkServer.isEmpty()) {
            return;
        }

        //特殊处理避免标题重复
        content = content.replaceFirst("^" + from + "(.*)", "").trim();

        barkServer += URLEncoder.encode(from, "UTF-8");
        barkServer += "/" + URLEncoder.encode(content, "UTF-8");
        barkServer += "?isArchive=1"; //自动保存
        barkServer += "&group=" + URLEncoder.encode(groupName, "UTF-8"); //增加支持分组
        if (!barkIcon.isEmpty()) {
            barkServer += "&icon=" + URLEncoder.encode(barkIcon, "UTF-8"); //指定推送消息图标
        }
        int isCode = content.indexOf("验证码");
        int isPassword = content.indexOf("动态密码");
        if (isCode != -1 || isPassword != -1) {
            Pattern p = Pattern.compile("(\\d{4,6})");
            Matcher m = p.matcher(content);
            if (m.find()) {
                System.out.println(m.group());
                barkServer += "&automaticallyCopy=1&copy=" + m.group();
            }
        }

        final String requestUrl = barkServer;
        Log.i(TAG, "requestUrl:" + requestUrl);

        Observable
                .create((ObservableEmitter<Object> emitter) -> {
                    Toast(handError, TAG, "开始请求接口...");

                    OkHttpClient client = new OkHttpClient();
                    final Request request = new Request.Builder().url(requestUrl).get().build();
                    Call call = client.newCall(request);
                    call.enqueue(new Callback() {
                        @Override
                        public void onFailure(@NonNull Call call, @NonNull final IOException e) {
                            LogUtil.updateLog(logId, 0, e.getMessage());
                            Toast(handError, TAG, "发送失败：" + e.getMessage());
                            emitter.onError(new RuntimeException("请求接口异常..."));
                        }

                        @Override
                        public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                            final String responseStr = Objects.requireNonNull(response.body()).string();
                            Log.d(TAG, "Response：" + response.code() + "，" + responseStr);
                            Toast(handError, TAG, "发送状态：" + responseStr);

                            //TODO:粗略解析是否发送成功
                            if (responseStr.contains("\"message\":\"success\"")) {
                                LogUtil.updateLog(logId, 1, responseStr);
                            } else {
                                LogUtil.updateLog(logId, 0, responseStr);
                            }
                        }
                    });

                }).retryWhen((Observable<Throwable> errorObservable) -> errorObservable
                .zipWith(Observable.just(
                        SettingUtil.getRetryDelayTime(1),
                        SettingUtil.getRetryDelayTime(2),
                        SettingUtil.getRetryDelayTime(3),
                        SettingUtil.getRetryDelayTime(4),
                        SettingUtil.getRetryDelayTime(5)
                ), (Throwable e, Integer time) -> time)
                .flatMap((Integer delay) -> {
                    Toast(handError, TAG, "请求接口异常，" + delay + "秒后重试");
                    return Observable.timer(delay, TimeUnit.SECONDS);
                }))
                .subscribe(System.out::println);

    }

}
