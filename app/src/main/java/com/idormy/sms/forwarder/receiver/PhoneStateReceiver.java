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
import com.idormy.sms.forwarder.utils.CommonUtil;
import com.idormy.sms.forwarder.utils.ContactHelper;
import com.idormy.sms.forwarder.utils.PhoneUtils;
import com.idormy.sms.forwarder.utils.SettingUtil;
import com.idormy.sms.forwarder.utils.SimUtil;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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
                //包括响铃、第三方来电等待
                case TelephonyManager.CALL_STATE_RINGING:
                    break;
                //空闲态(没有通话活动)
                case TelephonyManager.CALL_STATE_IDLE:
                    if (!TextUtils.isEmpty(phoneNumber)) {
                        try {
                            //必须休眠才能获取来电记录
                            Thread.sleep(1000);

                            sendReceiveCallMsg(context, phoneNumber);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    break;
                //包括dialing拨号中、active接通、hold挂起等
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    break;
            }

        }

    }

    private void sendReceiveCallMsg(Context context, String phoneNumber) {

        //获取后一条通话记录
        CallInfo callInfo = PhoneUtils.getLastCallInfo(phoneNumber);
        if (callInfo == null) return;

        if (callInfo.getType() != 3) {
            Log.d(TAG, "非未接来电不处理！");
            return;
        }

        String name = callInfo.getName();
        Log.d(TAG, "getSubscriptionId = " + callInfo.getSubscriptionId());
        int simId = SimUtil.getSimIdBySubscriptionId(callInfo.getSubscriptionId());
        String simInfo = simId == 2 ? SettingUtil.getAddExtraSim2() : SettingUtil.getAddExtraSim1(); //自定义备注优先
        if (!simInfo.isEmpty()) {
            simInfo = "SIM" + simId + "_" + simInfo;
        } else {
            simInfo = SimUtil.getSimInfo(simId);
        }

        if (TextUtils.isEmpty(name)) {
            List<PhoneBookEntity> contacts = ContactHelper.getInstance().getContactByNumber(context, phoneNumber);
            if (contacts != null && contacts.size() > 0) {
                PhoneBookEntity phoneBookEntity = contacts.get(0);
                name = phoneBookEntity.getName();
            }
            if (TextUtils.isEmpty(name)) name = context.getString(R.string.unknown_number);
        }

        //TODO:同一卡槽同一秒的重复未接来电广播不再重复处理（部分机型会收到两条广播？）
        String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINESE).format(new Date());
        String prevHash = SettingUtil.getPrevNoticeHash(phoneNumber);
        String currHash = CommonUtil.MD5(phoneNumber + simInfo + time);
        Log.d(TAG, "prevHash=" + prevHash + " currHash=" + currHash);
        if (prevHash != null && prevHash.equals(currHash)) {
            Log.w(TAG, "同一卡槽同一秒的重复未接来电广播不再重复处理（部分机型会收到两条广播）");
            return;
        }
        SettingUtil.setPrevNoticeHash(phoneNumber, currHash);

        SmsVo smsVo = new SmsVo(phoneNumber, name + context.getString(R.string.calling), new Date(), simInfo);
        Log.d(TAG, "send_msg" + smsVo.toString());
        SendUtil.send_msg(context, smsVo, simId, "call");
    }
}
