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
import com.idormy.sms.forwarder.model.vo.SmsHubVo;
import com.idormy.sms.forwarder.model.vo.SmsVo;
import com.idormy.sms.forwarder.sender.SendUtil;
import com.idormy.sms.forwarder.utils.CommonUtil;
import com.idormy.sms.forwarder.utils.ContactHelper;
import com.idormy.sms.forwarder.utils.PhoneUtils;
import com.idormy.sms.forwarder.utils.SettingUtil;
import com.idormy.sms.forwarder.utils.SimUtil;
import com.idormy.sms.forwarder.utils.SmsHubActionHandler;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

@SuppressWarnings("CommentedOutCode")
public class PhoneStateReceiver extends BroadcastReceiver {
    private static final String TAG = "PhoneStateReceiver";
    private TelephonyManager mTelephonyManager;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!SettingUtil.getSwitchEnablePhone()) {
            return;
        }

        String action = intent.getAction();
        //Bundle bundle = intent.getExtras();
        //if (bundle != null) {
        //    for (String key : bundle.keySet()) {
        //        Log.e(TAG, key + " : " + (bundle.get(key) != null ? bundle.get(key) : "NULL"));
        //    }
        //}

        if (TelephonyManager.ACTION_PHONE_STATE_CHANGED.equals(action)) {
            //获取来电号码
            String phoneNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
            if (mTelephonyManager == null) {
                mTelephonyManager =
                        (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            }
            int state = mTelephonyManager.getCallState();
            Log.d(TAG, "Caller information: state=" + state + " phoneNumber = " + phoneNumber);
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

        int type = callInfo.getType();
        if ((type == 1 && !SettingUtil.getSwitchCallType1())
                || (type == 2 && !SettingUtil.getSwitchCallType2())
                || (type == 3 && !SettingUtil.getSwitchCallType3())) {
            Log.w(TAG, "Call record forwarding of this type is not enabled, no processing will be done!");
            return;
        }

        Log.d(TAG, callInfo.toString());
        String name = callInfo.getName();
        String viaNumber = callInfo.getViaNumber(); //来源号码

        //卡槽判断：获取卡槽失败时，默认为卡槽1
        String simInfo;
        int simId = 1;
        Log.d(TAG, "getSubscriptionId = " + callInfo.getSubscriptionId()); //TODO:这里的SubscriptionId跟短信的不一样
        if (callInfo.getSubscriptionId() != -1) {
            simId = SimUtil.getSimIdBySubscriptionId(callInfo.getSubscriptionId());
        }
        simInfo = simId == 2 ? SettingUtil.getAddExtraSim2() : SettingUtil.getAddExtraSim1(); //自定义备注优先
        simInfo = "SIM" + simId + "_" + simInfo;

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
            Log.w(TAG, "Repeated missed call broadcasts of the same card slot in the same second are no longer processed repeatedly (some models will receive two broadcasts)");
            return;
        }
        SettingUtil.setPrevNoticeHash(phoneNumber, currHash);

        SmsVo smsVo = new SmsVo(phoneNumber, getTypeText(context, type, name, viaNumber), new Date(), simInfo);
        Log.d(TAG, "send_msg" + smsVo);
        SendUtil.send_msg(context, smsVo, simId, "call");

        //SmsHubApi
        if (SettingUtil.getSwitchEnableSmsHubApi()) {
            SmsHubActionHandler.putData(new SmsHubVo(SmsHubVo.Type.phone, simId, getTypeText(context, type, name, viaNumber), phoneNumber));
        }
    }

    //获取通话类型：1.呼入 2.呼出 3.未接
    private String getTypeText(Context context, int type, String name, String viaNumber) {
        String str = context.getString(R.string.linkman) + name + "\n";
        if (!TextUtils.isEmpty(viaNumber)) str += context.getString(R.string.via_number) + viaNumber + "\n";
        str += context.getString(R.string.mandatory_type);
        if (type == 1) return str + context.getString(R.string.received_call);
        if (type == 2) return str + context.getString(R.string.local_outgoing_call);
        return str + context.getString(R.string.missed_call);
    }
}
