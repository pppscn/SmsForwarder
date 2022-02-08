package com.idormy.sms.forwarder.sender;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.idormy.sms.forwarder.model.vo.SmsHubVo;
import com.idormy.sms.forwarder.utils.HttpUtil;
import com.idormy.sms.forwarder.utils.SettingUtil;
import com.idormy.sms.forwarder.utils.SmsHubActionHandler;

import java.util.List;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

/**
 * 主动发送短信轮询任务
 *
 * @author xxc
 * 2022/1/10 9:53
 */
public class SmsHubApiTask extends TimerTask {
    public static final long DELAY_SECONDS = 30;
    private static final String TAG = "SmsHubApiTask";
    private static final SmsHubActionHandler.SmsHubMode smsHubMode = SmsHubActionHandler.SmsHubMode.client;
    private static Boolean hasInit = false;
    private static Timer sendApiTimer;
    @SuppressLint("StaticFieldLeak")
    private static Context context;

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

    @Override
    public void run() {
        try {
            SmsHubVo smsHubVo = SmsHubVo.heartbeatInstance();
            List<SmsHubVo> data = SmsHubActionHandler.getData(smsHubMode);
            if (data != null && data.size() > 0) {
                smsHubVo.setChildren(data);
            }
            smsHubVo.setChildren(data);
            String url = SettingUtil.getSmsHubApiUrl();
            HttpUtil.asyncPostJson(TAG, url, smsHubVo, response -> {
                //HttpUtil.Toast(TAG, "Response：" + response.code() + "，" + responseStr);
                if (response.code() == 200) {
                    String responseStr = Objects.requireNonNull(response.body()).string();
                    List<SmsHubVo> vos = JSON.parseArray(responseStr, SmsHubVo.class);
                    for (SmsHubVo vo : vos) {
                        SmsHubActionHandler.handle(TAG, vo);
                    }
                    SmsHubActionHandler.putData(smsHubMode, vos.toArray(new SmsHubVo[0]));
                }
            }, null);
        } catch (Exception e) {
            HttpUtil.Toast(TAG, "SmsHubApiTask 执行出错,请检查问题后重新开启" + e.getMessage());
            cancelTimer();
            SettingUtil.switchEnableSmsHubApi(false);
        }
    }
}
