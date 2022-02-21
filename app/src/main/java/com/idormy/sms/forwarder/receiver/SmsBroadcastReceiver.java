package com.idormy.sms.forwarder.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.idormy.sms.forwarder.model.vo.SmsHubVo;
import com.idormy.sms.forwarder.model.vo.SmsVo;
import com.idormy.sms.forwarder.sender.SendUtil;
import com.idormy.sms.forwarder.utils.SettingUtil;
import com.idormy.sms.forwarder.utils.SimUtil;
import com.idormy.sms.forwarder.utils.SmsHubActionHandler;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class SmsBroadcastReceiver extends BroadcastReceiver {

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onReceive(Context context, Intent intent) {

        String receiveAction = intent.getAction();
        String TAG = "SmsBroadcastReceiver";
        Log.d(TAG, "onReceive intent " + receiveAction);

        String SMS_RECEIVED_ACTION = "android.provider.Telephony.SMS_RECEIVED";
        String SMS_DELIVER_ACTION = "android.provider.Telephony.SMS_DELIVER";

        if (SMS_RECEIVED_ACTION.equals(receiveAction) || SMS_DELIVER_ACTION.equals(receiveAction)) {
            try {
                if (!SettingUtil.getSwitchEnableSms()) {
                    return;
                }

                Bundle extras = intent.getExtras();
                Object[] object = (Object[]) Objects.requireNonNull(extras).get("pdus");
                if (object != null) {

                    //接收手机卡信息
                    String simInfo = "";
                    //卡槽ID，默认卡槽为1
                    int simId = 1;
                    try {
                        if (extras.containsKey("simId")) {
                            simId = extras.getInt("simId");
                        } else if (extras.containsKey("subscription")) {
                            simId = SimUtil.getSimIdBySubscriptionId(extras.getInt("subscription"));
                        }

                        //自定义备注优先
                        simInfo = simId == 2 ? SettingUtil.getAddExtraSim2() : SettingUtil.getAddExtraSim1();
                        simInfo = "SIM" + simId + "_" + simInfo;
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to get the receiving phone number:" + e.getMessage());
                    }

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

                        content += sms.getMessageBody().trim();//短信内容
                        mobileToContent.put(mobile, content);

                    }

                    for (String mobile : mobileToContent.keySet()) {
                        smsVoList.add(new SmsVo(mobile, mobileToContent.get(mobile), date, simInfo));
                    }

                    Log.d(TAG, "SMS: " + smsVoList);
                    SendUtil.send_msg_list(context, smsVoList, simId, "sms");

                    //SmsHubApi
                    if (SettingUtil.getSwitchEnableSmsHubApi()) {
                        List<SmsHubVo> smsHubVos = new ArrayList<>();
                        for (String mobile : mobileToContent.keySet()) {
                            smsHubVos.add(new SmsHubVo(SmsHubVo.Type.sms, simId, mobileToContent.get(mobile), mobile));
                        }
                        SmsHubActionHandler.putData(smsHubVos.toArray(new SmsHubVo[0]));
                    }
                }

            } catch (Throwable throwable) {
                Log.e(TAG, "Parsing SMS failed: " + throwable.getMessage());
            }

        }

    }

}
