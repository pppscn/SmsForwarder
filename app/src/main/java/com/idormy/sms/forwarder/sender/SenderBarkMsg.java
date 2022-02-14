package com.idormy.sms.forwarder.sender;

import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import com.idormy.sms.forwarder.model.vo.BarkSettingVo;
import com.idormy.sms.forwarder.utils.Define;
import com.idormy.sms.forwarder.utils.LogUtil;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SenderBarkMsg extends SenderBaseMsg {

    static final String TAG = "SenderBarkMsg";

    public static void sendMsg(final long logId, final Handler handError, final RetryIntercepter retryInterceptor, BarkSettingVo barkSettingVo, String title, String content, String groupName) throws Exception {
        Log.i(TAG, "sendMsg barkServer:" + barkSettingVo.toString() + " title:" + title + " content:" + content);

        String requestUrl = barkSettingVo.getServer(); //推送地址
        Log.i(TAG, "requestUrl:" + requestUrl);
        if (requestUrl == null || requestUrl.isEmpty()) {
            return;
        }

        String icon = barkSettingVo.getIcon(); //消息图标
        String level = barkSettingVo.getLevel(); //时效性
        String sound = barkSettingVo.getSound(); //声音
        String badge = barkSettingVo.getBadge(); //角标
        String url = barkSettingVo.getUrl(); //链接
        //特殊处理避免标题重复
        content = content.replaceFirst("^" + title + "(.*)", "").trim();

        String Content_Type = "application/x-www-form-urlencoded";
        MultipartBody.Builder builder = new MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("title", title)
                .addFormDataPart("body", content)
                .addFormDataPart("isArchive", "1")
                .addFormDataPart("group", groupName);

        if (!TextUtils.isEmpty(icon)) builder.addFormDataPart("icon", icon);
        if (!TextUtils.isEmpty(level)) builder.addFormDataPart("level", level);
        if (!TextUtils.isEmpty(sound)) builder.addFormDataPart("sound", sound);
        if (!TextUtils.isEmpty(badge)) builder.addFormDataPart("badge", badge);
        if (!TextUtils.isEmpty(url)) builder.addFormDataPart("url", url);

        int isCode = content.indexOf("验证码");
        int isPassword = content.indexOf("动态密码");
        int isPassword2 = content.indexOf("短信密码");
        if (isCode != -1 || isPassword != -1 || isPassword2 != -1) {
            Pattern p = Pattern.compile("(\\d{4,6})");
            Matcher m = p.matcher(content);
            if (m.find()) {
                System.out.println(m.group());
                builder.addFormDataPart("automaticallyCopy", "1");
                builder.addFormDataPart("copy", m.group());
            }
        }

        RequestBody body = builder.build();
        Log.d(TAG, "method = POST, Body = " + body);

        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder();
        //设置重试拦截器
        if (retryInterceptor != null) clientBuilder.addInterceptor(retryInterceptor);
        //设置读取超时时间
        OkHttpClient client = clientBuilder
                .readTimeout(Define.REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .writeTimeout(Define.REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .connectTimeout(Define.REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .build();

        final Request request = new Request.Builder().url(requestUrl).method("POST", body).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull final IOException e) {
                LogUtil.updateLog(logId, 0, e.getMessage());
                Toast(handError, TAG, "发送失败：" + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                final String responseStr = Objects.requireNonNull(response.body()).string();
                Log.d(TAG, "Response：" + response.code() + "，" + responseStr);
                Toast(handError, TAG, "发送状态：" + responseStr);

                //TODO:粗略解析是否发送成功
                if (responseStr.contains("\"message\":\"success\"")) {
                    LogUtil.updateLog(logId, 2, responseStr);
                } else {
                    LogUtil.updateLog(logId, 0, responseStr);
                }
            }
        });

    }

}
