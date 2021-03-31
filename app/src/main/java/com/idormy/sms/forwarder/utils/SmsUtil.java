package com.idormy.sms.forwarder.utils;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.telephony.SmsManager;
import android.util.Log;

import java.util.ArrayList;

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

            ArrayList<PendingIntent> sentPendingIntents = new ArrayList<PendingIntent>();
            ArrayList<PendingIntent> deliveredPendingIntents = new ArrayList<PendingIntent>();
            ArrayList<String> divideContents = smsManager.divideMessage(message);

            for (int i = 0; i < divideContents.size(); i++) {
                sentPendingIntents.add(i, sendPI);
                deliveredPendingIntents.add(i, deliverPI);
            }
            smsManager.sendMultipartTextMessage(mobiles, null, divideContents, sentPendingIntents, deliveredPendingIntents);

            return null;
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            return e.getMessage();
        }
    }

}
