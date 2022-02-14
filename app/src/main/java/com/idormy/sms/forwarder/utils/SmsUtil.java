package com.idormy.sms.forwarder.utils;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.telephony.SmsManager;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.Objects;

@SuppressWarnings("SynchronizeOnNonFinalField")
public class SmsUtil {
    static final String TAG = "SmsUtil";
    static Boolean hasInit = false;
    @SuppressLint("StaticFieldLeak")
    static Context context;

    public static void init(Context context1) {
        synchronized (hasInit) {
            if (hasInit) return;
            hasInit = true;
            context = context1;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP_MR1)
    public static String sendSms(int subId, String mobiles, String message) {
        mobiles = mobiles.replace("；", ";").replace("，", ";").replace(",", ";");
        Log.d(TAG, "subId = " + subId + ", mobiles = " + mobiles + ", message = " + message);

        String[] mobileArray = mobiles.split(";");
        for (String mobile : mobileArray) {
            try {
                SmsManager smsManager = SmsManager.getSmsManagerForSubscriptionId(subId);

                int sendFlags = Build.VERSION.SDK_INT >= 30 ? PendingIntent.FLAG_IMMUTABLE : PendingIntent.FLAG_ONE_SHOT;
                PendingIntent sendPI = PendingIntent.getBroadcast(context, 0, new Intent(Context.TELEPHONY_SUBSCRIPTION_SERVICE), sendFlags);

                int deliverFlags = Build.VERSION.SDK_INT >= 30 ? PendingIntent.FLAG_IMMUTABLE : 0;
                PendingIntent deliverPI = PendingIntent.getBroadcast(context, 0, new Intent("DELIVERED_SMS_ACTION"), deliverFlags);

                ArrayList<PendingIntent> sentPendingIntents = new ArrayList<>();
                ArrayList<PendingIntent> deliveredPendingIntents = new ArrayList<>();
                ArrayList<String> divideContents = smsManager.divideMessage(message);

                for (int i = 0; i < divideContents.size(); i++) {
                    sentPendingIntents.add(i, sendPI);
                    deliveredPendingIntents.add(i, deliverPI);
                }
                smsManager.sendMultipartTextMessage(mobile, null, divideContents, sentPendingIntents, deliveredPendingIntents);

            } catch (Exception e) {
                Log.e(TAG, Objects.requireNonNull(e.getMessage()));
                return e.getMessage();
            }
        }

        return null;
    }

}
