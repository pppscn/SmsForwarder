package com.idormy.sms.forwarder.sender;

import static com.idormy.sms.forwarder.model.SenderModel.STATUS_OFF;
import static com.idormy.sms.forwarder.model.SenderModel.TYPE_BARK;
import static com.idormy.sms.forwarder.model.SenderModel.TYPE_DINGDING;
import static com.idormy.sms.forwarder.model.SenderModel.TYPE_EMAIL;
import static com.idormy.sms.forwarder.model.SenderModel.TYPE_FEISHU;
import static com.idormy.sms.forwarder.model.SenderModel.TYPE_GOTIFY;
import static com.idormy.sms.forwarder.model.SenderModel.TYPE_PUSHPLUS;
import static com.idormy.sms.forwarder.model.SenderModel.TYPE_QYWX_APP;
import static com.idormy.sms.forwarder.model.SenderModel.TYPE_QYWX_GROUP_ROBOT;
import static com.idormy.sms.forwarder.model.SenderModel.TYPE_SERVER_CHAN;
import static com.idormy.sms.forwarder.model.SenderModel.TYPE_SMS;
import static com.idormy.sms.forwarder.model.SenderModel.TYPE_TELEGRAM;
import static com.idormy.sms.forwarder.model.SenderModel.TYPE_WEB_NOTIFY;

import android.annotation.SuppressLint;
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
import com.idormy.sms.forwarder.model.vo.GotifySettingVo;
import com.idormy.sms.forwarder.model.vo.LogVo;
import com.idormy.sms.forwarder.model.vo.PushPlusSettingVo;
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
import com.idormy.sms.forwarder.utils.SettingUtil;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SendUtil {
    private static final String TAG = "SendUtil";

    public static void send_msg_list(Context context, List<SmsVo> smsVoList, int simId, String type) {
        Log.i(TAG, "send_msg_list size: " + smsVoList.size());
        for (SmsVo smsVo : smsVoList) {
            SendUtil.send_msg(context, smsVo, simId, type);
        }
    }

    public static void send_msg(Context context, SmsVo smsVo, int simId, String type) {
        Log.i(TAG, "send_msg smsVo:" + smsVo);
        RuleUtil.init(context);
        LogUtil.init(context);

        String key = "SIM" + simId;
        List<RuleModel> ruleList = RuleUtil.getRule(null, key, type, "1"); //只取已启用的规则
        if (!ruleList.isEmpty()) {
            Log.d(TAG, ruleList.toString());
            SenderUtil.init(context);
            for (RuleModel ruleModel : ruleList) {
                //规则匹配发现需要发送
                try {
                    if (ruleModel.checkMsg(smsVo)) {
                        List<SenderModel> senderModels = SenderUtil.getSender(ruleModel.getSenderId(), null);
                        for (SenderModel senderModel : senderModels
                        ) {
                            long logId = LogUtil.addLog(new LogModel(type, smsVo.getMobile(), smsVo.getContent(), smsVo.getSimInfo(), ruleModel.getId()));
                            String smsTemplate = ruleModel.getSwitchSmsTemplate() ? ruleModel.getSmsTemplate() : "";
                            String regexReplace = ruleModel.getSwitchRegexReplace() ? ruleModel.getRegexReplace() : "";
                            SendUtil.senderSendMsgNoHandError(smsVo, senderModel, logId, smsTemplate, regexReplace);
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "send_msg: fail checkMsg:", e);
                }
            }
        }
    }

    /**
     * 从日志获取消息内容并尝试重发
     * 根据当前rule和sender来重发，而不是失败时设置的规则
     *
     * @param context 上下文
     * @param handler 回调，用于刷新日志列表
     * @param logVo   日志
     */
    public static void resendMsgByLog(Context context, Handler handler, LogVo logVo) {
        Log.d(TAG, logVo.toString());
        @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        Date date = new Date();
        try {
            date = sdf.parse(logVo.getTime());
        } catch (ParseException e) {
            Log.e(TAG, "SimpleDateFormat parse error", e);
        }
        SmsVo smsVo = new SmsVo(logVo.getFrom(), logVo.getContent(), date, logVo.getSimInfo());
        Log.d(TAG, "resendMsgByLog smsVo:" + smsVo);

        //从simInfo判断接收的是SIM1还是SIM2，获取不到时默认走ALL
        String simInfo = smsVo.getSimInfo();
        String key = null;
        if (simInfo.startsWith("SIM1")) {
            key = "SIM1";
        } else if (simInfo.startsWith("SIM2")) {
            key = "SIM2";
        }

        RuleUtil.init(context);
        List<RuleModel> ruleList = RuleUtil.getRule(null, key, logVo.getType(), "1"); //只取已启用的规则
        if (!ruleList.isEmpty()) {
            SenderUtil.init(context);
            for (RuleModel ruleModel : ruleList) {
                //规则匹配发现需要发送
                try {
                    if (ruleModel.checkMsg(smsVo)) {
                        List<SenderModel> senderModels = SenderUtil.getSender(ruleModel.getSenderId(), null);
                        for (SenderModel senderModel : senderModels) {
                            String smsTemplate = ruleModel.getSwitchSmsTemplate() ? ruleModel.getSmsTemplate() : "";
                            String regexReplace = ruleModel.getSwitchRegexReplace() ? ruleModel.getRegexReplace() : "";
                            SendUtil.senderSendMsg(handler, null, smsVo, senderModel, logVo.getId(), smsTemplate, regexReplace);
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "resendMsgByLog: fail checkMsg:", e);
                }
            }
        }
    }

    public static void sendMsgByRuleModelSenderId(final Handler handError, RuleModel ruleModel, SmsVo smsVo, Long senderId) throws Exception {
        if (senderId == null) {
            throw new Exception("先新建并选择发送通道");
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
            throw new Exception("未找到发送通道");
        }

        for (SenderModel senderModel : senderModels) {
            String smsTemplate = ruleModel.getSwitchSmsTemplate() ? ruleModel.getSmsTemplate() : "";
            String regexReplace = ruleModel.getSwitchRegexReplace() ? ruleModel.getRegexReplace() : "";
            SendUtil.senderSendMsg(handError, null, smsVo, senderModel, 0, smsTemplate, regexReplace);
        }
    }

    public static void senderSendMsgNoHandError(SmsVo smsVo, SenderModel senderModel, long logId, String smsTemplate, String regexReplace) {
        //网络请求+延时重试比较耗时，创建子线程处理
        new Thread(() -> {
            try {
                int retryTimes = SettingUtil.getRetryTimes();
                int delayTime = SettingUtil.getDelayTime();
                RetryIntercepter retryInterceptor = retryTimes < 1 ? null : new RetryIntercepter.Builder().executionCount(retryTimes).retryInterval(delayTime).logId(logId).build();
                SendUtil.senderSendMsg(null, retryInterceptor, smsVo, senderModel, logId, smsTemplate, regexReplace);
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
            }
        }).start();
    }

    public static void senderSendMsg(Handler handError, RetryIntercepter retryInterceptor, SmsVo smsVo, SenderModel senderModel, long logId, String smsTemplate, String regexReplace) {

        Log.i(TAG, "senderSendMsg smsVo:" + smsVo.toString() + "senderModel:" + senderModel.toString());

        if (senderModel.getStatus() == STATUS_OFF) {
            LogUtil.updateLog(logId, 0, "发送通道已被禁用！");
            Log.i(TAG, "发送通道已被禁用！");
            return;
        }

        switch (senderModel.getType()) {
            case TYPE_DINGDING:
                //try phrase json setting
                if (senderModel.getJsonSetting() != null) {
                    DingDingSettingVo dingDingSettingVo = JSON.parseObject(senderModel.getJsonSetting(), DingDingSettingVo.class);
                    if (dingDingSettingVo != null) {
                        try {
                            SenderDingdingMsg.sendMsg(logId, handError, retryInterceptor, dingDingSettingVo.getToken(), dingDingSettingVo.getSecret(), dingDingSettingVo.getAtMobiles(), dingDingSettingVo.getAtAll(), smsVo.getSmsVoForSend(smsTemplate, regexReplace));
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
                            SenderMailMsg.sendEmail(logId, handError, emailSettingVo.getProtocol(), emailSettingVo.getHost(), emailSettingVo.getPort(), emailSettingVo.getSsl(), emailSettingVo.getFromEmail(), emailSettingVo.getNickname(),
                                    emailSettingVo.getPwd(), emailSettingVo.getToEmail(), smsVo.getTitleForSend(emailSettingVo.getTitle(), regexReplace), smsVo.getSmsVoForSend(smsTemplate, regexReplace));
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
                            SenderBarkMsg.sendMsg(logId, handError, retryInterceptor, barkSettingVo, smsVo.getTitleForSend(barkSettingVo.getTitle(), regexReplace), smsVo.getSmsVoForSend(smsTemplate, regexReplace), senderModel.getName());
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
                            SenderWebNotifyMsg.sendMsg(logId, handError, retryInterceptor, webNotifySettingVo.getWebServer(), webNotifySettingVo.getWebParams(), webNotifySettingVo.getSecret(), webNotifySettingVo.getMethod(), smsVo, smsTemplate, regexReplace);
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
                            SenderQyWxGroupRobotMsg.sendMsg(logId, handError, retryInterceptor, qywxGroupRobotSettingVo.getWebHook(), smsVo.getMobile(), smsVo.getSmsVoForSend(smsTemplate, regexReplace));
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
                            SenderQyWxAppMsg.sendMsg(logId, handError, retryInterceptor, senderModel, qYWXAppSettingVo, smsVo.getSmsVoForSend(smsTemplate, regexReplace));
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
                            SenderServerChanMsg.sendMsg(logId, handError, retryInterceptor, serverChanSettingVo.getSendKey(), smsVo.getMobile(), smsVo.getSmsVoForSend(smsTemplate, regexReplace));
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
                            SenderTelegramMsg.sendMsg(logId, handError, retryInterceptor, telegramSettingVo, smsVo.getMobile(), smsVo.getSmsVoForSend(smsTemplate, regexReplace), telegramSettingVo.getMethod());
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
                            SenderSmsMsg.sendMsg(logId, handError, simSlot, smsSettingVo.getMobiles(), smsSettingVo.getOnlyNoNetwork(), smsVo.getMobile(), smsVo.getSmsVoForSend(smsTemplate, regexReplace));
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
                            SenderFeishuMsg.sendMsg(logId, handError, retryInterceptor, feiShuSettingVo.getWebhook(), feiShuSettingVo.getSecret(), feiShuSettingVo.getMsgType(), smsVo.getMobile(), smsVo.getDate(), smsVo.getTitleForSend(feiShuSettingVo.getTitleTemplate()), smsVo.getSmsVoForSend(smsTemplate, regexReplace));
                        } catch (Exception e) {
                            LogUtil.updateLog(logId, 0, e.getMessage());
                            Log.e(TAG, "senderSendMsg: feishu error " + e.getMessage());
                        }
                    }
                }
                break;

            case TYPE_PUSHPLUS:
                //try phrase json setting
                if (senderModel.getJsonSetting() != null) {
                    PushPlusSettingVo pushPlusSettingVo = JSON.parseObject(senderModel.getJsonSetting(), PushPlusSettingVo.class);
                    if (pushPlusSettingVo != null) {
                        try {
                            SenderPushPlusMsg.sendMsg(logId, handError, retryInterceptor, pushPlusSettingVo, smsVo.getTitleForSend(pushPlusSettingVo.getTitleTemplate()), smsVo.getSmsVoForSend(smsTemplate, regexReplace));
                        } catch (Exception e) {
                            LogUtil.updateLog(logId, 0, e.getMessage());
                            Log.e(TAG, "senderSendMsg: feishu error " + e.getMessage());
                        }
                    }
                }
                break;

            case TYPE_GOTIFY:
                //try phrase json setting
                if (senderModel.getJsonSetting() != null) {
                    GotifySettingVo gotifySettingVo = JSON.parseObject(senderModel.getJsonSetting(), GotifySettingVo.class);
                    if (gotifySettingVo != null) {
                        try {
                            SenderGotifyMsg.sendMsg(logId, handError, retryInterceptor, gotifySettingVo, smsVo.getMobile(), smsVo.getSmsVoForSend(smsTemplate, regexReplace));
                        } catch (Exception e) {
                            LogUtil.updateLog(logId, 0, e.getMessage());
                            Log.e(TAG, "senderSendMsg: gotify error " + e.getMessage());
                        }
                    }
                }
                break;

            default:
                break;
        }
    }

}
