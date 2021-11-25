package com.idormy.sms.forwarder.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import com.idormy.sms.forwarder.R;
import com.idormy.sms.forwarder.model.CallInfo;
import com.idormy.sms.forwarder.model.PhoneBookEntity;
import com.idormy.sms.forwarder.model.vo.SmsVo;
import com.idormy.sms.forwarder.sender.SendUtil;
import com.idormy.sms.forwarder.utils.ContactHelper;
import com.idormy.sms.forwarder.utils.PhoneUtils;
import com.idormy.sms.forwarder.utils.SettingUtil;
import com.idormy.sms.forwarder.utils.SimUtil;

import java.util.Date;
import java.util.List;

public class PhoneStateReceiver extends BroadcastReceiver {
    private static final String TAG = "PhoneStateReceiver";
    private TelephonyManager mTelephonyManager;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!SettingUtil.getSwitchEnablePhone()) {
            return;
        }

        String action = intent.getAction();
        if (TelephonyManager.ACTION_PHONE_STATE_CHANGED.equals(action)) {
            //获取来电号码
            String phoneNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
            if (mTelephonyManager == null) {
                mTelephonyManager =
                        (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            }
            int state = mTelephonyManager.getCallState();
            Log.d(TAG, "来电信息：state=" + state + " phoneNumber = " + phoneNumber);
            switch (state) {
                case TelephonyManager.CALL_STATE_RINGING:
                    if (!TextUtils.isEmpty(phoneNumber)) {
                        sendReceiveCallMsg(context, phoneNumber);
                    }
                    break;
                case TelephonyManager.CALL_STATE_IDLE:
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    break;
            }

        }

    }

    private void sendReceiveCallMsg(Context context, String phoneNumber) {
        int simId = 0;
        String name = "";
        String simInfo = "";

        //获取后一条通话记录
        CallInfo callInfo = PhoneUtils.getLastCallInfo();
        if (callInfo != null) {
            Log.d(TAG, callInfo.toString());

            simId = SimUtil.getSimIdBySubscriptionId(callInfo.getSubscriptionId());
            //自定义备注优先
            simInfo = simId == 2 ? SettingUtil.getAddExtraSim2() : SettingUtil.getAddExtraSim1();
            if (!simInfo.isEmpty()) {
                simInfo = "SIM" + simId + "_" + simInfo;
            } else {
                simInfo = SimUtil.getSimInfo(simId);
            }

            name = callInfo.getName();
        } else {
            List<PhoneBookEntity> contacts = ContactHelper.getInstance().getContactByNumber(context, phoneNumber);
            if (contacts != null && contacts.size() > 0) {
                PhoneBookEntity phoneBookEntity = contacts.get(0);
                name = phoneBookEntity.getName();
            }
        }

        if (TextUtils.isEmpty(name)) {
            name = context.getString(R.string.unknown_number);
        }
        SmsVo smsVo = new SmsVo(phoneNumber, name + context.getString(R.string.calling), new Date(), simInfo);
        Log.d(TAG, "send_msg" + smsVo.toString());
        SendUtil.send_msg(context, smsVo, simId, "call");
    }
}
