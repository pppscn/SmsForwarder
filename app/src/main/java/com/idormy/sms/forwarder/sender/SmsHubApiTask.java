package com.idormy.sms.forwarder.sender;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.idormy.sms.forwarder.model.vo.SmsHubVo;
import com.idormy.sms.forwarder.utils.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * 主动发送短信轮询任务
 * @author xxc
 * 2022/1/10 9:53
 */
public class SmsHubApiTask extends TimerTask {
    private static Boolean hasInit = false;
    public static final long DELAY_SECONDS = 30;
    private static final String TAG = "SmsHubApiTask";
    private static Timer sendApiTimer;
    @SuppressLint("StaticFieldLeak")
    private static Context context;
    private static final SmsHubActionHandler.SmsHubMode smsHubMode = SmsHubActionHandler.SmsHubMode.client;


    @SuppressLint("HandlerLeak")
    public static void init(Context context) {
        //noinspection SynchronizeOnNonFinalField
        synchronized (hasInit) {
            if (hasInit) return;

            hasInit = true;
            SmsHubApiTask.context = context;
            SmsHubActionHandler.init(SmsHubApiTask.context);
        }
    }

    @Override
    public void run() {
        List<SmsHubVo> data = SmsHubActionHandler.getData(smsHubMode);
        SmsHubVo smsHubVo = SmsHubVo.heartbeatInstance(data);
        String url = SettingUtil.getSmsHubApiUrl();
        boolean asRetry = data != null && data.size() > 0;
        AtomicBoolean isSusess = new AtomicBoolean(false);
        Runnable runnable = () -> {
            HttpUtil.asyncPostJson(TAG, url, smsHubVo, response -> {
                //HttpUtil.Toast(TAG, "Response：" + response.code() + "，" + responseStr);
                if (response.code() == 200) {
                    isSusess.set(true);
                    String responseStr = Objects.requireNonNull(response.body()).string();
                    List<SmsHubVo> vos = JSON.parseArray(responseStr, SmsHubVo.class);
                    for (SmsHubVo vo : vos) {
                        SmsHubActionHandler.handle(TAG, vo);
                    }
                    SmsHubActionHandler.putData(smsHubMode, vos.toArray(new SmsHubVo[0]));
                }
            }, null, asRetry);
        };
        if (asRetry) {
            new Thread(runnable).start();
        } else {
            runnable.run();
        }
    }


    public static void updateTimer() {
        cancelTimer();
        if (SettingUtil.getSwitchEnableSmsHubApi()) {
            SmsHubVo.getDevInfoMap(true);
            startTimer();
        } else {
            Log.d(TAG, "Cancel SmsHubApiTaskTimer");
            HttpUtil.Toast(TAG, "Cancel SmsHubApiTaskTimer");
        }
    }

    private static void cancelTimer() {
        if (sendApiTimer != null) {
            sendApiTimer.cancel();
            sendApiTimer = null;
        }
    }

    private static void startTimer() {
        Log.d(TAG, "Start SmsHubApiTimer");
        if (SettingUtil.getSwitchEnableSmsHubApi()) {
            long seconds = SmsHubApiTask.DELAY_SECONDS;
            Log.d(TAG, "SmsHubApiTimer started  " + seconds);
            sendApiTimer = new Timer("SmsHubApiTimer", true);
            sendApiTimer.schedule(new SmsHubApiTask(), 3000, seconds * 1000);
        }
    }
}
