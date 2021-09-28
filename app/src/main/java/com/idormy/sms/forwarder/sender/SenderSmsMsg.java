package com.idormy.sms.forwarder.sender;

import android.os.Handler;
import android.util.Log;

import com.idormy.sms.forwarder.utils.LogUtil;
import com.idormy.sms.forwarder.utils.SettingUtil;
import com.idormy.sms.forwarder.utils.SimUtil;
import com.idormy.sms.forwarder.utils.SmsUtil;

import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableEmitter;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class SenderSmsMsg extends SenderBaseMsg {

    static final String TAG = "SenderSmsMsg";

    public static void sendMsg(final long logId, final Handler handError, int simSlot, String mobiles, Boolean onlyNoNetwork, String from, String text) throws Exception {
        Log.i(TAG, "sendMsg simSlot:" + simSlot + " mobiles:" + mobiles + " onlyNoNetwork:" + onlyNoNetwork + " from:" + from + " text:" + text);

        //TODO：simSlot转subId
        final int subId = SimUtil.getSubscriptionIdBySimId(simSlot);

        Observable
                .create((ObservableEmitter<Object> emitter) -> {
                    Toast(handError, TAG, "开始发送短信...");

                    String res = SmsUtil.sendSms(subId, mobiles, text);

                    //TODO:粗略解析是否发送成功
                    if (res == null) {
                        LogUtil.updateLog(logId, 1, "发送成功");
                    } else {
                        LogUtil.updateLog(logId, 0, res);
                        Toast(handError, TAG, "短信发送失败");
                        emitter.onError(new RuntimeException("短信发送异常..."));
                    }

                }).retryWhen((Observable<Throwable> errorObservable) -> errorObservable
                .zipWith(Observable.just(
                        SettingUtil.getRetryDelayTime(1),
                        SettingUtil.getRetryDelayTime(2),
                        SettingUtil.getRetryDelayTime(3),
                        SettingUtil.getRetryDelayTime(4),
                        SettingUtil.getRetryDelayTime(5)
                ), (Throwable e, Integer time) -> time)
                .flatMap((Integer delay) -> {
                    Toast(handError, TAG, "短信发送异常，" + delay + "秒后重试");
                    return Observable.timer(delay, TimeUnit.SECONDS);
                }))
                .subscribe(System.out::println);
    }

}
