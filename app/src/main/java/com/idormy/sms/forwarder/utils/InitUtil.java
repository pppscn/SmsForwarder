package com.idormy.sms.forwarder.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

@SuppressWarnings("SynchronizeOnNonFinalField")
public class InitUtil {
    static Boolean hasInit = false;
    private static final String TAG = "InitUtil";
    @SuppressLint("StaticFieldLeak")
    private static Context context = null;

    public static void init(Context context1) {
        Log.d(TAG, "SmsForwarder init");
        //noinspection SynchronizeOnNonFinalField
        synchronized (hasInit) {
            if (hasInit) return;
            hasInit = true;
            context = context1;
            Log.d(TAG, "init context");
            SettingUtil.init(context);
        }


    }

}
