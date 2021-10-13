package com.idormy.sms.forwarder.sender;

import static com.idormy.sms.forwarder.model.SenderModel.TYPE_BARK;
import static com.idormy.sms.forwarder.model.SenderModel.TYPE_DINGDING;
import static com.idormy.sms.forwarder.model.SenderModel.TYPE_EMAIL;
import static com.idormy.sms.forwarder.model.SenderModel.TYPE_FEISHU;
import static com.idormy.sms.forwarder.model.SenderModel.TYPE_QYWX_APP;
import static com.idormy.sms.forwarder.model.SenderModel.TYPE_QYWX_GROUP_ROBOT;
import static com.idormy.sms.forwarder.model.SenderModel.TYPE_SERVER_CHAN;
import static com.idormy.sms.forwarder.model.SenderModel.TYPE_SMS;
import static com.idormy.sms.forwarder.model.SenderModel.TYPE_TELEGRAM;
import static com.idormy.sms.forwarder.model.SenderModel.TYPE_WEB_NOTIFY;

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
import com.idormy.sms.forwarder.model.vo.FeiShuSettingVo;
import com.idormy.sms.forwarder.model.vo.QYWXAppSettingVo;
import com.idormy.sms.forwarder.model.vo.QYWXGroupRobotSettingVo;
import com.idormy.sms.forwarder.model.vo.ServerChanSettingVo;
import com.idormy.sms.forwarder.model.vo.SmsSettingVo;
import com.idormy.sms.forwarder.model.vo.SmsVo;
import com.idormy.sms.forwarder.model.vo.TelegramSettingVo;
import com.idormy.sms.forwarder.model.vo.WebNotifySettingVo;
import com.idormy.sms.forwarder.utils.LogUtil;
import com.idormy.sms.forwarder.utils.NetUtil;
import com.idormy.sms.forwarder.utils.RuleUtil;

import java.util.List;

public class SendUtil {
    private static final String TAG = "SendUtil";

    public static void send_msg_list(Context context, List<SmsVo> smsVoList, int simId) {
        Log.i(TAG, "send_msg_list size: " + smsVoList.size());
        for (SmsVo smsVo : smsVoList) {
            SendUtil.send_msg(context, smsVo, simId);
        }
    }

    public static void send_msg(Context context, SmsVo smsVo, int simId) {
        Log.i(TAG, "send_msg smsVo:" + smsVo);
        RuleUtil.init(context);
        LogUtil.init(context);

        String key = "SIM" + simId;
        List<RuleModel> ruleList = RuleUtil.getRule(null, key);
        if (!ruleList.isEmpty()) {
            SenderUtil.init(context);
            for (RuleModel ruleModel : ruleList) {
                //规则匹配发现需要发送
                try {
                    if (ruleModel.checkMsg(smsVo)) {
                        List<SenderModel> senderModels = SenderUtil.getSender(ruleModel.getSenderId(), null);
                        for (SenderModel senderModel : senderModels
                        ) {
                            long logId = LogUtil.addLog(new LogModel(smsVo.getMobile(), smsVo.getContent(), smsVo.getSimInfo(), ruleModel.getId()));
                            SendUtil.senderSendMsgNoHandError(smsVo, senderModel, logId);
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

        String testSim = smsVo.getSimInfo().substring(0, 4);
        String ruleSim = ruleModel.getSimSlot();

        if (!ruleSim.equals("ALL") && !ruleSim.equals(testSim)) {
            throw new Exception("接收卡槽未匹配中规则");
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
            SendUtil.senderSendMsg(handError, smsVo, senderModel, 0);
        }
    }

    public static void senderSendMsgNoHandError(SmsVo smsVo, SenderModel senderModel, long logId) {
        SendUtil.senderSendMsg(null, smsVo, senderModel, logId);
    }

    public static void senderSendMsg(Handler handError, SmsVo smsVo, SenderModel senderModel, long logId) {

        Log.i(TAG, "senderSendMsg smsVo:" + smsVo + "senderModel:" + senderModel);
        switch (senderModel.getType()) {
            case TYPE_DINGDING:
                //try phrase json setting
                if (senderModel.getJsonSetting() != null) {
                    DingDingSettingVo dingDingSettingVo = JSON.parseObject(senderModel.getJsonSetting(), DingDingSettingVo.class);
                    if (dingDingSettingVo != null) {
                        try {
                            SenderDingdingMsg.sendMsg(logId, handError, dingDingSettingVo.getToken(), dingDingSettingVo.getSecret(), dingDingSettingVo.getAtMobiles(), dingDingSettingVo.getAtAll(), smsVo.getSmsVoForSend());
                        } catch (Exception e) {
                            LogUtil.updateLog(logId, 0, e.getMessage());
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
                            SenderMailMsg.sendEmail(logId, handError, emailSettingVo.getHost(), emailSettingVo.getPort(), emailSettingVo.getSsl(), emailSettingVo.getFromEmail(), emailSettingVo.getNickname(),
                                    emailSettingVo.getPwd(), emailSettingVo.getToEmail(), smsVo.getMobile(), smsVo.getSmsVoForSend());
                        } catch (Exception e) {
                            LogUtil.updateLog(logId, 0, e.getMessage());
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
                            SenderBarkMsg.sendMsg(logId, handError, barkSettingVo.getServer(), barkSettingVo.getIcon(), smsVo.getMobile(), smsVo.getSmsVoForSend(), senderModel.getName());
                        } catch (Exception e) {
                            LogUtil.updateLog(logId, 0, e.getMessage());
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
                            SenderWebNotifyMsg.sendMsg(logId, handError, webNotifySettingVo.getWebServer(), webNotifySettingVo.getWebParams(), webNotifySettingVo.getSecret(), webNotifySettingVo.getMethod(), smsVo.getMobile(), smsVo.getSmsVoForSend());
                        } catch (Exception e) {
                            LogUtil.updateLog(logId, 0, e.getMessage());
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
                            SenderQyWxGroupRobotMsg.sendMsg(logId, handError, qywxGroupRobotSettingVo.getWebHook(), smsVo.getMobile(), smsVo.getSmsVoForSend());
                        } catch (Exception e) {
                            LogUtil.updateLog(logId, 0, e.getMessage());
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
                            SenderQyWxAppMsg.sendMsg(logId, handError, qYWXAppSettingVo.getCorpID(), qYWXAppSettingVo.getAgentID(), qYWXAppSettingVo.getSecret(), qYWXAppSettingVo.getToUser(), smsVo.getSmsVoForSend(), false);
                        } catch (Exception e) {
                            LogUtil.updateLog(logId, 0, e.getMessage());
                            Log.e(TAG, "senderSendMsg: qywx_app error " + e.getMessage());
                        }
                    }
                }
                break;

            case TYPE_SERVER_CHAN:
                //try phrase json setting
                if (senderModel.getJsonSetting() != null) {
                    ServerChanSettingVo serverChanSettingVo = JSON.parseObject(senderModel.getJsonSetting(), ServerChanSettingVo.class);
                    if (serverChanSettingVo != null) {
                        try {
                            SenderServerChanMsg.sendMsg(logId, handError, serverChanSettingVo.getSendKey(), smsVo.getMobile(), smsVo.getSmsVoForSend());
                        } catch (Exception e) {
                            LogUtil.updateLog(logId, 0, e.getMessage());
                            Log.e(TAG, "senderSendMsg: SenderServerChanMsg error " + e.getMessage());
                        }
                    }
                }
                break;

            case TYPE_TELEGRAM:
                //try phrase json setting
                if (senderModel.getJsonSetting() != null) {
                    TelegramSettingVo telegramSettingVo = JSON.parseObject(senderModel.getJsonSetting(), TelegramSettingVo.class);
                    if (telegramSettingVo != null) {
                        try {
                            SenderTelegramMsg.sendMsg(logId, handError, telegramSettingVo.getApiToken(), telegramSettingVo.getChatId(), smsVo.getMobile(), smsVo.getSmsVoForSend());
                        } catch (Exception e) {
                            LogUtil.updateLog(logId, 0, e.getMessage());
                            Log.e(TAG, "senderSendMsg: SenderTelegramMsg error " + e.getMessage());
                        }
                    }
                }
                break;

            case TYPE_SMS:
                //try phrase json setting
                if (senderModel.getJsonSetting() != null) {
                    SmsSettingVo smsSettingVo = JSON.parseObject(senderModel.getJsonSetting(), SmsSettingVo.class);
                    if (smsSettingVo != null) {
                        //仅当无网络时启用
                        if (smsSettingVo.getOnlyNoNetwork() && 0 != NetUtil.getNetWorkStatus()) {
                            String msg = "仅当无网络时启用，当前网络状态：" + NetUtil.getNetWorkStatus();
                            LogUtil.updateLog(logId, 0, msg);
                            Log.d(TAG, msg);
                            return;
                        }
                        try {
                            int simSlot = smsSettingVo.getSimSlot() - 1;
                            if (simSlot < 0) { //原进原出
                                simSlot = Integer.parseInt(smsVo.getSimInfo().substring(3, 4)) - 1;
                                Log.d(TAG, "simSlot = " + simSlot);
                            }
                            SenderSmsMsg.sendMsg(logId, handError, simSlot, smsSettingVo.getMobiles(), smsSettingVo.getOnlyNoNetwork(), smsVo.getMobile(), smsVo.getSmsVoForSend());
                        } catch (Exception e) {
                            LogUtil.updateLog(logId, 0, e.getMessage());
                            Log.e(TAG, "senderSendMsg: SenderSmsMsg error " + e.getMessage());
                        }
                    }
                }
                break;

            case TYPE_FEISHU:
                //try phrase json setting
                if (senderModel.getJsonSetting() != null) {
                    FeiShuSettingVo feiShuSettingVo = JSON.parseObject(senderModel.getJsonSetting(), FeiShuSettingVo.class);
                    if (feiShuSettingVo != null) {
                        try {
                            SenderFeishuMsg.sendMsg(logId, handError, feiShuSettingVo.getWebhook(), feiShuSettingVo.getSecret(), smsVo.getSmsVoForSend());
                        } catch (Exception e) {
                            LogUtil.updateLog(logId, 0, e.getMessage());
                            Log.e(TAG, "senderSendMsg: feishu error " + e.getMessage());
                        }
                    }
                }
                break;

            default:
                break;
        }
    }

}
