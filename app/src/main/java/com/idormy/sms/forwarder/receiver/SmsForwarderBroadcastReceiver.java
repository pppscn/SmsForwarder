package com.idormy.sms.forwarder.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

import com.idormy.sms.forwarder.model.vo.SmsVo;
import com.idormy.sms.forwarder.sender.SendUtil;
import com.idormy.sms.forwarder.utils.SettingUtil;
import com.idormy.sms.forwarder.utils.SimUtil;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class SmsForwarderBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        String receiveAction = intent.getAction();
        String TAG = "SmsForwarderBroadcastReceiver";
        Log.d(TAG, "onReceive intent " + receiveAction);
        if ("android.provider.Telephony.SMS_RECEIVED".equals(receiveAction)) {
            try {

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
                        if (!simInfo.isEmpty()) {
                            simInfo = "SIM" + simId + "_" + simInfo;
                        } else {
                            simInfo = SimUtil.getSimInfo(simId);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "获取接收手机号失败：" + e.getMessage());
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

                        content += sms.getMessageBody();//短信内容
                        mobileToContent.put(mobile, content);

                    }
                    for (String mobile : mobileToContent.keySet()) {
                        smsVoList.add(new SmsVo(mobile, mobileToContent.get(mobile), date, simInfo));
                    }
                    Log.d(TAG, "短信：" + smsVoList);
                    SendUtil.send_msg_list(context, smsVoList, simId);

                }

            } catch (Throwable throwable) {
                Log.e(TAG, "解析短信失败：" + throwable.getMessage());
            }

        }

    }

}