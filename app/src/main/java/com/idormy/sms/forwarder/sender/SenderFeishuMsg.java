package com.idormy.sms.forwarder.sender;

import android.os.Handler;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;

import com.alibaba.fastjson.JSON;
import com.idormy.sms.forwarder.utils.LogUtil;
import com.idormy.sms.forwarder.utils.SettingUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableEmitter;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@SuppressWarnings("ALL")
public class SenderFeishuMsg extends SenderBaseMsg {

    static final String TAG = "SenderFeishuMsg";

    static final String MSG_TEMPLATE = "{\n" +
            "  \"config\": {\n" +
            "    \"wide_screen_mode\": true\n" +
            "  },\n" +
            "  \"elements\": [\n" +
            "    {\n" +
            "      \"fields\": [\n" +
            "        {\n" +
            "          \"is_short\": true,\n" +
            "          \"text\": {\n" +
            "            \"content\": \"**时间**\\n${MSG_TIME}\",\n" +
            "            \"tag\": \"lark_md\"\n" +
            "          }\n" +
            "        },\n" +
            "        {\n" +
            "          \"is_short\": true,\n" +
            "          \"text\": {\n" +
            "            \"content\": \"**来源**\\n${MSG_FROM}\",\n" +
            "            \"tag\": \"lark_md\"\n" +
            "          }\n" +
            "        }\n" +
            "      ],\n" +
            "      \"tag\": \"div\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"tag\": \"div\",\n" +
            "      \"text\": {\n" +
            "        \"content\": \"${MSG_CONTENT}\",\n" +
            "        \"tag\": \"lark_md\"\n" +
            "      }\n" +
            "    },\n" +
            "    {\n" +
            "      \"tag\": \"hr\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"elements\": [\n" +
            "        {\n" +
            "          \"content\": \"[SmsForwarder](https://github.com/pppscn/SmsForwarder)\",\n" +
            "          \"tag\": \"lark_md\"\n" +
            "        }\n" +
            "      ],\n" +
            "      \"tag\": \"note\"\n" +
            "    }\n" +
            "  ],\n" +
            "  \"header\": {\n" +
            "    \"template\": \"turquoise\",\n" +
            "    \"title\": {\n" +
            "      \"content\": \"${MSG_TITLE}\",\n" +
            "      \"tag\": \"plain_text\"\n" +
            "    }\n" +
            "  }\n" +
            "}";

    public static void sendMsg(final long logId, final Handler handError, String webhook, String secret, String from, Date date, String content) throws Exception {
        Log.i(TAG, "sendMsg webhook:" + webhook + " secret:" + secret + " content:" + content);

        if (webhook == null || webhook.isEmpty()) {
            return;
        }

        Map textMsgMap = new HashMap();

        //签名校验
        if (secret != null && !secret.isEmpty()) {
            Long timestamp = System.currentTimeMillis() / 1000;
            //把timestamp+"\n"+密钥当做签名字符串
            String stringToSign = timestamp + "\n" + secret;
            Log.i(TAG, "stringToSign = " + stringToSign);

            //使用HmacSHA256算法计算签名
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(stringToSign.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] signData = mac.doFinal(new byte[]{});
            String sign = new String(Base64.encode(signData, Base64.NO_WRAP));

            textMsgMap.put("timestamp", timestamp);
            textMsgMap.put("sign", sign);
        }

        //组装报文
        textMsgMap.put("msg_type", "interactive");
        textMsgMap.put("card", "${CARD_BODY}");

        final String requestUrl = webhook;
        Log.i(TAG, "requestUrl:" + requestUrl);
        final String requestMsg = JSON.toJSONString(textMsgMap).replace("\"${CARD_BODY}\"", buildMsg(from, date, content));
        Log.i(TAG, "requestMsg:" + requestMsg);

        Observable
                .create((ObservableEmitter<Object> emitter) -> {
                    Toast(handError, TAG, "开始请求接口...");

                    OkHttpClient client = new OkHttpClient();
                    RequestBody requestBody = RequestBody.create(MediaType.parse("application/json;charset=utf-8"), requestMsg);

                    final Request request = new Request.Builder()
                            .url(requestUrl)
                            .addHeader("Content-Type", "application/json; charset=utf-8")
                            .post(requestBody)
                            .build();
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
                            if (responseStr.contains("\"StatusCode\":0")) {
                                LogUtil.updateLog(logId, 2, responseStr);
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

    private static String buildMsg(String from, Date date, String content) {
        String msgTitle = jsonInnerStr("【" + SettingUtil.getAddExtraDeviceMark().trim() + "】来自" + from + "的通知");
        String msgTime = jsonInnerStr(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date));
        String msgFrom = jsonInnerStr(from);
        String msgContent = jsonInnerStr(content);
        return MSG_TEMPLATE.replace("${MSG_TITLE}", msgTitle)
                .replace("${MSG_TIME}", msgTime)
                .replace("${MSG_FROM}", msgFrom)
                .replace("${MSG_CONTENT}", msgContent);
    }

    private static String jsonInnerStr(String string) {
        if (string == null) {
            return "null";
        }
        String jsonStr = JSON.toJSONString(string);
        return jsonStr.length() >= 2 ? jsonStr.substring(1, jsonStr.length() - 1) : jsonStr;
    }

}
