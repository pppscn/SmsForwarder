package com.idormy.sms.forwarder.sender;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.idormy.sms.forwarder.model.LogModel;
import com.idormy.sms.forwarder.model.RuleModel;
import com.idormy.sms.forwarder.model.SenderModel;
import com.idormy.sms.forwarder.model.vo.BarkSettingVo;
import com.idormy.sms.forwarder.model.vo.DingDingSettingVo;
import com.idormy.sms.forwarder.model.vo.EmailSettingVo;
import com.idormy.sms.forwarder.model.vo.QYWXAppSettingVo;
import com.idormy.sms.forwarder.model.vo.QYWXGroupRobotSettingVo;
import com.idormy.sms.forwarder.model.vo.SmsVo;
import com.idormy.sms.forwarder.model.vo.WebNotifySettingVo;
import com.idormy.sms.forwarder.utils.LogUtil;
import com.idormy.sms.forwarder.utils.RuleUtil;
import com.idormy.sms.forwarder.utils.SettingUtil;

import java.util.List;

import static com.idormy.sms.forwarder.model.SenderModel.TYPE_BARK;
import static com.idormy.sms.forwarder.model.SenderModel.TYPE_DINGDING;
import static com.idormy.sms.forwarder.model.SenderModel.TYPE_EMAIL;
import static com.idormy.sms.forwarder.model.SenderModel.TYPE_QYWX_APP;
import static com.idormy.sms.forwarder.model.SenderModel.TYPE_QYWX_GROUP_ROBOT;
import static com.idormy.sms.forwarder.model.SenderModel.TYPE_WEB_NOTIFY;

public class SendUtil {
    private static String TAG = "SendUtil";

    public static void send_msg(String msg) {
        if (SettingUtil.using_dingding()) {
            try {
                SenderDingdingMsg.sendMsg(msg);
            } catch (Exception e) {
                Log.d(TAG, "发送出错：" + e.getMessage());
            }

        }
        if (SettingUtil.using_email()) {
//            SenderMailMsg.send(SettingUtil.get_send_util_email(Define.SP_MSG_SEND_UTIL_EMAIL_TOADD_KEY),"转发",msg);
        }

    }

    public static void send_msg_list(Context context, List<SmsVo> smsVoList) {
        Log.i(TAG, "send_msg_list size: " + smsVoList.size());
        for (SmsVo smsVo : smsVoList) {
            SendUtil.send_msg(context, smsVo);
        }
    }

    public static void send_msg(Context context, SmsVo smsVo) {
        Log.i(TAG, "send_msg smsVo:" + smsVo);
        RuleUtil.init(context);
        LogUtil.init(context);

        List<RuleModel> rulelist = RuleUtil.getRule(null, null);
        if (!rulelist.isEmpty()) {
            SenderUtil.init(context);
            for (RuleModel ruleModel : rulelist) {
                //规则匹配发现需要发送
                try {
                    if (ruleModel.checkMsg(smsVo)) {
                        List<SenderModel> senderModels = SenderUtil.getSender(ruleModel.getSenderId(), null);
                        for (SenderModel senderModel : senderModels
                        ) {
                            LogUtil.addLog(new LogModel(smsVo.getMobile(), smsVo.getContent(), smsVo.getSimInfo(), senderModel.getId()));
                            SendUtil.senderSendMsgNoHandError(smsVo, senderModel);
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "send_msg: fail checkMsg:", e);
                }
            }
        }
    }

    public static void sendMsgByRuleModelSenderId(final Handler handError, RuleModel ruleModel, SmsVo smsVo, Long senderId) throws Exception {
        if (senderId == null) {
            throw new Exception("先新建并选择发送方");
        }

        if (!ruleModel.checkMsg(smsVo)) {
            throw new Exception("短信未匹配中规则");
        }

        List<SenderModel> senderModels = SenderUtil.getSender(senderId, null);
        if (senderModels.isEmpty()) {
            throw new Exception("未找到发送方");
        }

        for (SenderModel senderModel : senderModels
        ) {
            SendUtil.senderSendMsg(handError, smsVo, senderModel);
        }
    }

    public static void senderSendMsgNoHandError(SmsVo smsVo, SenderModel senderModel) {
        SendUtil.senderSendMsg(null, smsVo, senderModel);
    }

    public static void senderSendMsg(Handler handError, SmsVo smsVo, SenderModel senderModel) {

        Log.i(TAG, "senderSendMsg smsVo:" + smsVo + "senderModel:" + senderModel);
        switch (senderModel.getType()) {
            case TYPE_DINGDING:
                //try phrase json setting
                if (senderModel.getJsonSetting() != null) {
                    DingDingSettingVo dingDingSettingVo = JSON.parseObject(senderModel.getJsonSetting(), DingDingSettingVo.class);
                    if (dingDingSettingVo != null) {
                        try {
                            SenderDingdingMsg.sendMsg(handError, dingDingSettingVo.getToken(), dingDingSettingVo.getSecret(), dingDingSettingVo.getAtMobils(), dingDingSettingVo.getAtAll(), smsVo.getSmsVoForSend());
                        } catch (Exception e) {
                            Log.e(TAG, "senderSendMsg: dingding error " + e.getMessage());
                        }
                    }
                }
                break;

            case TYPE_EMAIL:
                //try phrase json setting
                if (senderModel.getJsonSetting() != null) {
                    EmailSettingVo emailSettingVo = JSON.parseObject(senderModel.getJsonSetting(), EmailSettingVo.class);
                    if (emailSettingVo != null) {
                        try {
                            SenderMailMsg.sendEmail(handError, emailSettingVo.getHost(), emailSettingVo.getPort(), emailSettingVo.getSsl(), emailSettingVo.getFromEmail(),
                                    emailSettingVo.getPwd(), emailSettingVo.getToEmail(), smsVo.getMobile(), smsVo.getSmsVoForSend());
                        } catch (Exception e) {
                            Log.e(TAG, "senderSendMsg: SenderMailMsg error " + e.getMessage());
                        }
                    }
                }
                break;

            case TYPE_BARK:
                //try phrase json setting
                if (senderModel.getJsonSetting() != null) {
                    BarkSettingVo barkSettingVo = JSON.parseObject(senderModel.getJsonSetting(), BarkSettingVo.class);
                    if (barkSettingVo != null) {
                        try {
                            SenderBarkMsg.sendMsg(handError, barkSettingVo.getServer(), smsVo.getMobile(), smsVo.getSmsVoForSend());
                        } catch (Exception e) {
                            Log.e(TAG, "senderSendMsg: SenderBarkMsg error " + e.getMessage());
                        }
                    }
                }
                break;

            case TYPE_WEB_NOTIFY:
                //try phrase json setting
                if (senderModel.getJsonSetting() != null) {
                    WebNotifySettingVo webNotifySettingVo = JSON.parseObject(senderModel.getJsonSetting(), WebNotifySettingVo.class);
                    if (webNotifySettingVo != null) {
                        try {
                            SenderWebNotifyMsg.sendMsg(handError, webNotifySettingVo.getToken(), webNotifySettingVo.getSecret(), smsVo.getMobile(), smsVo.getSmsVoForSend());
                        } catch (Exception e) {
                            Log.e(TAG, "senderSendMsg: SenderWebNotifyMsg error " + e.getMessage());
                        }
                    }
                }
                break;

            case TYPE_QYWX_GROUP_ROBOT:
                //try phrase json setting
                if (senderModel.getJsonSetting() != null) {
                    QYWXGroupRobotSettingVo qywxGroupRobotSettingVo = JSON.parseObject(senderModel.getJsonSetting(), QYWXGroupRobotSettingVo.class);
                    if (qywxGroupRobotSettingVo != null) {
                        try {
                            SenderQyWxGroupRobotMsg.sendMsg(handError, qywxGroupRobotSettingVo.getWebHook(), smsVo.getMobile(), smsVo.getSmsVoForSend());
                        } catch (Exception e) {
                            Log.e(TAG, "senderSendMsg: SenderQyWxGroupRobotMsg error " + e.getMessage());
                        }
                    }
                }
                break;

            case TYPE_QYWX_APP:
                //try phrase json setting
                if (senderModel.getJsonSetting() != null) {
                    QYWXAppSettingVo qYWXAppSettingVo = JSON.parseObject(senderModel.getJsonSetting(), QYWXAppSettingVo.class);
                    if (qYWXAppSettingVo != null) {
                        try {
                            SenderQyWxAppMsg.sendMsg(handError, qYWXAppSettingVo.getCorpID(), qYWXAppSettingVo.getAgentID(), qYWXAppSettingVo.getSecret(), qYWXAppSettingVo.getToUser(), smsVo.getSmsVoForSend(), false);
                        } catch (Exception e) {
                            Log.e(TAG, "senderSendMsg: qywx_app error " + e.getMessage());
                        }
                    }
                }
                break;

            default:
                break;
        }
    }

}
