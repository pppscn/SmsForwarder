package com.idormy.sms.forwarder.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.idormy.sms.forwarder.model.LogModel;
import com.idormy.sms.forwarder.model.vo.SmsHubVo;
import com.idormy.sms.forwarder.sender.SmsHubApiTask;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class SmsHubActionHandler {
    public static final long RULE_ID = -999L;
    private static Boolean hasInit = false;

    private static ConcurrentHashMap<SmsHubMode, List<SmsHubVo>> cache;

    public enum SmsHubMode {
        server, client
    }

    @SuppressLint("StaticFieldLeak")
    private static Context context;

    @SuppressLint("HandlerLeak")
    public static void init(Context context) {
        //noinspection SynchronizeOnNonFinalField
        synchronized (hasInit) {
            if (hasInit) return;

            hasInit = true;
            SmsHubActionHandler.context = context;
            cache = new ConcurrentHashMap<>();
            for (SmsHubMode smsHubMode : SmsHubMode.values()) {
                cache.put(smsHubMode, new ArrayList<>());
            }
        }
    }

    public static synchronized int size(SmsHubMode smsHubMode) {
        return Objects.requireNonNull(cache.get(smsHubMode)).size();
    }

    public static synchronized List<SmsHubVo> getData(SmsHubMode smsHubMode) {
        List<SmsHubVo> smsHubVoList = cache.get(smsHubMode);
        assert smsHubVoList != null;
        if (smsHubVoList.size() > 0) {
            cache.put(smsHubMode, new ArrayList<>());
            return smsHubVoList;
        } else {
            return null;
        }
    }

    private static long falg = System.currentTimeMillis();

    public static synchronized void putData(SmsHubMode smsHubMode, SmsHubVo... smsHubVos) {
        if (isEnable(smsHubMode)) {
            if (smsHubMode == SmsHubMode.server) {
                long l = falg;
                falg = System.currentTimeMillis();
                if (System.currentTimeMillis() - l > SmsHubApiTask.DELAY_SECONDS * 3) {
                    return;
                }
            }
            Objects.requireNonNull(cache.get(smsHubMode)).addAll(Arrays.asList(smsHubVos));
        }
    }

    public static synchronized void putData(SmsHubVo... smsHubVos) {
        for (SmsHubMode smsHubMode : SmsHubMode.values()) {
            putData(smsHubMode, smsHubVos);
        }
    }

    private static boolean isEnable(SmsHubMode smsHubMode) {
        boolean enable = false;
        if (smsHubMode == SmsHubMode.client) {
            enable = SettingUtil.getSwitchEnableSmsHubApi();
        } else if (smsHubMode == SmsHubMode.server) {
            enable = SettingUtil.getSwitchEnableHttpServer();
        }
        return enable;
    }

    public static void handle(String tag, SmsHubVo vo) {
        Log.i(tag, JSON.toJSONString(vo));
        String action = vo.getAction();
        if (SmsHubVo.Action.send.code().equals(action)) {
            send(tag, vo);
        } else {
            String errMsg = "暂不支持的action[" + action + "]";
            vo.setErrMsg(errMsg);
            vo.setAction(SmsHubVo.Action.failure.code());
        }
        vo.setTs(Long.toString(System.currentTimeMillis()));
    }

    public static void send(String tag, SmsHubVo vo) {
        boolean failure = true;
        String msg = "";
        Long logId = null;
        try {
            if (SmsHubVo.Action.send.code().equals(vo.getAction())) {
                vo.setType(SmsHubVo.Type.sms.code());
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {
                    int subscriptionIdBySimId = SimUtil.getSubscriptionIdBySimId(Integer.parseInt(vo.getChannel()) - 1);
                    msg = SmsUtil.sendSms(subscriptionIdBySimId, vo.getTarget(), vo.getContent());
                    String simInfo = "SIM" + (subscriptionIdBySimId + 1);
                    vo.setChannel(simInfo);
                    logId = LogUtil.addLog(new LogModel(vo.getType(), vo.getTarget(), vo.getContent(), simInfo, RULE_ID));
                    if (msg == null) {
                        failure = false;
                        HttpUtil.Toast(tag, "短信发送成功");
                        Log.i(tag, "短信发送成功");
                        vo.setAction(SmsHubVo.Action.suessces.code());
                        LogUtil.updateLog(logId, 2, SmsHubVo.Action.suessces.code());
                    }
                } else {
                    msg = "api<22";
                }
            }
        } catch (Exception e) {
            msg += e.getMessage();
            e.printStackTrace();
        }
        if (failure) {
            msg = "短信发送失败:" + msg;
            HttpUtil.Toast(tag, msg);
            Log.i(tag, msg);
            vo.setAction(SmsHubVo.Action.failure.code());
            vo.setErrMsg(msg);
            if (logId != null) {
                LogUtil.updateLog(logId, 0, msg);
            }
        }
    }
}
