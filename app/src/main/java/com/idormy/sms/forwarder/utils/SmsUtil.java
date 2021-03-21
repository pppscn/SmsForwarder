package com.idormy.sms.forwarder.utils;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.telephony.SmsManager;
import android.util.Log;

import java.util.List;

public class SmsUtil {
    static String TAG = "SmsUtil";
    static Boolean hasInit = false;
    static Context context;


    public static void init(Context context1) {
        synchronized (hasInit) {
            if (hasInit) return;
            hasInit = true;
            context = context1;
        }
    }

    public static String sendSms(int subId, String mobiles, String message) {
        mobiles = mobiles.replace("；", ";");
        Log.d(TAG, "subId = " + subId + ", mobiles = " + mobiles + ", message = " + message);

        try {
            SmsManager smsManager = SmsManager.getSmsManagerForSubscriptionId(subId);
            PendingIntent sendPI = PendingIntent.getBroadcast(context, 0, new Intent(Context.TELEPHONY_SUBSCRIPTION_SERVICE), PendingIntent.FLAG_ONE_SHOT);
            PendingIntent deliverPI = PendingIntent.getBroadcast(context, 0, new Intent("DELIVERED_SMS_ACTION"), 0);

            List<String> divideContents = smsManager.divideMessage(message);
            for (String text : divideContents) {
                smsManager.sendTextMessage(mobiles, null, text, sendPI, deliverPI);
            }

            return null;
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            return e.getMessage();
        }
    }

}
