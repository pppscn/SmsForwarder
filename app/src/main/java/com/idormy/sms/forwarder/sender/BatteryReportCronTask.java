package com.idormy.sms.forwarder.sender;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.idormy.sms.forwarder.MyApplication;
import com.idormy.sms.forwarder.model.vo.SmsVo;
import com.idormy.sms.forwarder.utils.BatteryUtils;
import com.idormy.sms.forwarder.utils.SettingUtils;

import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class BatteryReportCronTask {
    private static final String TAG = "BatteryReportCronTask";
    private volatile static BatteryReportCronTask singleton;
    private static Timer timer;

    private BatteryReportCronTask() {
    }

    public static BatteryReportCronTask getSingleton() {
        if (singleton == null) {
            synchronized (BatteryReportCronTask.class) {
                if (singleton == null) {
                    singleton = new BatteryReportCronTask();
                }
            }
        }
        return singleton;
    }

    public void updateTimer() {
        cancelTimer();
        if (SettingUtils.getSwitchEnableBatteryCron()) {
            startTimer();
        } else {
            Log.d(TAG, "Cancel Task");
        }
    }

    private void cancelTimer() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    private void startTimer() {
        String startTime = SettingUtils.getBatteryCronStartTime();
        int interval = SettingUtils.getBatteryCronInterval();
        Log.i(TAG, "Task started, startTime: " + startTime + ", interval: " + interval);

        int hour = Integer.parseInt(startTime.split(":")[0]);
        int minute = Integer.parseInt(startTime.split(":")[1]);

        Calendar startTimeCalendar = Calendar.getInstance();
        startTimeCalendar.set(Calendar.HOUR_OF_DAY, hour);
        startTimeCalendar.set(Calendar.MINUTE, minute);
        startTimeCalendar.set(Calendar.SECOND, 0);

        Calendar currentTimeCalendar = Calendar.getInstance();
        if (startTimeCalendar.before(currentTimeCalendar)) {
            //首次发送时间在当前时间之前，日期加一天
            startTimeCalendar.add(Calendar.DATE, 1);
        }
        Log.d(TAG, startTimeCalendar.getTime().toString());

        timer = new Timer("BatteryReportCronTimer", true);
        timer.schedule(new Task(), startTimeCalendar.getTime(), interval * 60 * 1000L);
    }

    static class Task extends TimerTask {
        @Override
        public void run() {
            IntentFilter intentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent intent = MyApplication.getContext().registerReceiver(null, intentFilter);
            String msg = BatteryUtils.getBatteryInfo(intent);
            sendMessage(MyApplication.getContext(), msg);
        }

        //发送信息
        private void sendMessage(Context context, String msg) {
            Log.i(TAG, msg);
            try {
                SmsVo smsVo = new SmsVo("88888888", msg, new Date(), "电池状态定时推送");
                SendUtil.send_msg(context, smsVo, 1, "app");
            } catch (Exception e) {
                Log.e(TAG, "sendMessage e:" + e.getMessage());
            }
        }
    }
}
