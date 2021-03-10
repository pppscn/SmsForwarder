package com.idormy.sms.forwarder.utils;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.idormy.sms.forwarder.MyApplication;

import java.util.HashMap;
import java.util.Map;


public class SimUtil {
    private static String TAG = "SimUtil";

    //获取卡槽ID
    public static int getSimId(Bundle bundle) {
        int whichSIM = -1;
        if (bundle == null) {
            return whichSIM;
        }

        if (bundle.containsKey("subscription")) {
            whichSIM = bundle.getInt("subscription");
        }
        if (whichSIM >= 0 && whichSIM < 5) {
            /*In some device Subscription id is return as subscriber id*/
            //TODO：不确定能不能直接返回
            Log.d(TAG, "whichSIM >= 0 && whichSIM < 5：" + whichSIM);
        } else {
            if (bundle.containsKey("simId")) {
                whichSIM = bundle.getInt("simId");
            } else if (bundle.containsKey("com.android.phone.extra.slot")) {
                whichSIM = bundle.getInt("com.android.phone.extra.slot");
            } else {
                String keyName = "";
                for (String key : bundle.keySet()) {
                    if (key.contains("sim"))
                        keyName = key;
                }
                if (bundle.containsKey(keyName)) {
                    whichSIM = bundle.getInt(keyName);
                }
            }
        }

        Log.d(TAG, " Slot Number " + whichSIM);
        return whichSIM;
    }


    //获取SIM卡信息
    public static void getSimInfo(MyApplication appContext, String Line1Number) {
        try {
            Uri uri = Uri.parse("content://telephony/siminfo"); //访问raw_contacts表
            ContentResolver resolver = appContext.getContentResolver();
            Cursor cursor = resolver.query(uri, new String[]{"_id", "icc_id", "sim_id", "display_name", "carrier_name", "name_source", "color", "number", "display_number_format", "data_roaming", "mcc", "mnc"}, "sim_id >= 0", null, "_id");
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    Log.d(TAG, "_id: " + cursor.getString(cursor.getColumnIndex("_id")));
                    Log.d(TAG, "sim_id: " + cursor.getString(cursor.getColumnIndex("sim_id")));
                    Log.d(TAG, "carrier_name: " + cursor.getString(cursor.getColumnIndex("carrier_name")));
                    Log.d(TAG, "display_name: " + cursor.getString(cursor.getColumnIndex("display_name")));
                    Map<String, String> sim = new HashMap();
                    String id = cursor.getString(cursor.getColumnIndex("_id"));
                    sim.put("_id", id);
                    sim.put("sim_id", cursor.getString(cursor.getColumnIndex("sim_id")));
                    sim.put("carrier_name", cursor.getString(cursor.getColumnIndex("carrier_name")));
                    sim.put("display_name", cursor.getString(cursor.getColumnIndex("display_name")));
                    sim.put("phone_number", Line1Number);
                    if (Line1Number != "Unknown") {
                        Line1Number = "Unknown";
                    }
                    MyApplication.SimInfo.put(id, sim);
                }
                cursor.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "getSimInfo fail：" + e.getMessage());
            //Toast.makeText(MainActivity.this, "获取SIM卡信息失败：请先手动设置", Toast.LENGTH_LONG).show();
        }
    }
}
