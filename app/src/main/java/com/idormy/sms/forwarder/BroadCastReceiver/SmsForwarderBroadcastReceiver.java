package com.idormy.sms.forwarder.BroadCastReceiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

import com.idormy.sms.forwarder.MyApplication;
import com.idormy.sms.forwarder.model.vo.SmsVo;
import com.idormy.sms.forwarder.utils.SendUtil;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class SmsForwarderBroadcastReceiver extends BroadcastReceiver {
    private String TAG = "SmsForwarderBroadcastReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {

        String receiveAction = intent.getAction();
        Log.d(TAG, "onReceive intent " + receiveAction);
        if (receiveAction.equals("android.provider.Telephony.SMS_RECEIVED")) {
            try {

                Bundle extras = intent.getExtras();
                Object[] object = (Object[]) Objects.requireNonNull(extras).get("pdus");
                if (object != null) {

                    //获取接收手机号
                    String simInfoId = String.valueOf(capturedSimSlot(extras));
                    Log.d("SIM_SLOT", " Slot Number " + simInfoId);
                    Map<String, String> sim = MyApplication.SimInfo.get(simInfoId);
                    String phoneNumber = "SIM-" + sim.get("sim_id") + "_" + sim.get("carrier_name") + "_" + sim.get("phone_number");

                    List<SmsVo> smsVoList = new ArrayList<>();
                    String format = intent.getStringExtra("format");
                    Map<String, String> mobileToContent = new HashMap<>();
                    Date date = new Date();
                    for (Object pdus : object) {
                        byte[] pdusMsg = (byte[]) pdus;
                        SmsMessage sms = SmsMessage.createFromPdu(pdusMsg, format);
                        String mobile = sms.getOriginatingAddress();//发送短信的手机号
                        if (mobile == null) {
                            continue;
                        }
                        //下面是获取短信的发送时间
                        date = new Date(sms.getTimestampMillis());

                        String content = mobileToContent.get(mobile);
                        if (content == null) content = "";

                        content += sms.getMessageBody();//短信内容
                        mobileToContent.put(mobile, content);

                    }
                    for (String mobile : mobileToContent.keySet()) {
                        smsVoList.add(new SmsVo(mobile, mobileToContent.get(mobile), date, phoneNumber));
                    }
                    Log.d(TAG, "短信：" + smsVoList);
                    SendUtil.send_msg_list(context, smsVoList);

                }

            } catch (Throwable throwable) {
                Log.e(TAG, "解析短信失败：" + throwable.getMessage());
            }

        }

    }

    //获取卡槽ID
    private int capturedSimSlot(Bundle bundle) {
        int whichSIM = -1;
        if (bundle.containsKey("subscription")) {
            whichSIM = bundle.getInt("subscription");
        }
        if (whichSIM >= 0 && whichSIM < 5) {
            /*In some device Subscription id is return as subscriber id*/
            //TODO：不确定能不能直接返回
            return whichSIM;
        }

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
        return whichSIM;
    }

}