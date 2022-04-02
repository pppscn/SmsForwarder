package com.idormy.sms.forwarder.utils;

import com.alibaba.fastjson.JSON;
import com.idormy.sms.forwarder.model.RuleModel;
import com.idormy.sms.forwarder.model.SenderModel;
import com.idormy.sms.forwarder.model.vo.CloneInfoVo;
import com.idormy.sms.forwarder.sender.SenderUtil;

import java.util.List;

/**
 * 一键克隆工具类
 */
public class CloneUtils {

    //导出设置
    public static String exportSettings() {
        CloneInfoVo cloneInfo = new CloneInfoVo();
        try {
            cloneInfo.setVersionCode(SettingUtil.getVersionCode());
            cloneInfo.setVersionName(SettingUtil.getVersionName());
            cloneInfo.setEnableSms(SettingUtil.getSwitchEnableSms());
            cloneInfo.setEnablePhone(SettingUtil.getSwitchEnablePhone());
            cloneInfo.setCallType1(SettingUtil.getSwitchCallType1());
            cloneInfo.setCallType2(SettingUtil.getSwitchCallType2());
            cloneInfo.setCallType3(SettingUtil.getSwitchCallType3());
            cloneInfo.setEnableAppNotify(SettingUtil.getSwitchEnableAppNotify());
            cloneInfo.setCancelAppNotify(SettingUtil.getSwitchCancelAppNotify());
            cloneInfo.setSmsHubApiUrl(SettingUtil.getSmsHubApiUrl());
            cloneInfo.setBatteryLevelAlarmMin(SettingUtil.getBatteryLevelAlarmMin());
            cloneInfo.setBatteryLevelAlarmMax(SettingUtil.getBatteryLevelAlarmMax());
            cloneInfo.setBatteryLevelAlarmOnce(SettingUtil.getBatteryLevelAlarmOnce());
            cloneInfo.setRetryTimes(SettingUtil.getRetryTimes());
            cloneInfo.setDelayTime(SettingUtil.getDelayTime());
            cloneInfo.setEnableSmsTemplate(SettingUtil.getSwitchSmsTemplate());
            cloneInfo.setSmsTemplate(SettingUtil.getSmsTemplate());
            cloneInfo.setSenderList(SenderUtil.getSender(null, null));
            cloneInfo.setRuleList(RuleUtil.getRule(null, null));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return JSON.toJSONString(cloneInfo);
    }

    //还原设置
    public static boolean restoreSettings(CloneInfoVo cloneInfoVo) {

        try {
            //应用配置
            //SettingUtil.init(context);
            SettingUtil.switchEnableSms(cloneInfoVo.isEnableSms());
            SettingUtil.switchEnablePhone(cloneInfoVo.isEnablePhone());
            SettingUtil.switchCallType1(cloneInfoVo.isCallType1());
            SettingUtil.switchCallType2(cloneInfoVo.isCallType2());
            SettingUtil.switchCallType3(cloneInfoVo.isCallType3());
            SettingUtil.switchEnableAppNotify(cloneInfoVo.isEnableAppNotify());
            SettingUtil.switchCancelAppNotify(cloneInfoVo.isCancelAppNotify());
            SettingUtil.smsHubApiUrl(cloneInfoVo.getSmsHubApiUrl());
            SettingUtil.setBatteryLevelAlarmMin(cloneInfoVo.getBatteryLevelAlarmMin());
            SettingUtil.setBatteryLevelAlarmMax(cloneInfoVo.getBatteryLevelAlarmMax());
            SettingUtil.switchBatteryLevelAlarmOnce(cloneInfoVo.isBatteryLevelAlarmOnce());
            SettingUtil.setRetryTimes(cloneInfoVo.getRetryTimes());
            SettingUtil.setDelayTime(cloneInfoVo.getDelayTime());
            SettingUtil.switchSmsTemplate(cloneInfoVo.isEnableSmsTemplate());
            SettingUtil.setSmsTemplate(cloneInfoVo.getSmsTemplate());

            SenderUtil.delSender(null);
            List<SenderModel> senderList = cloneInfoVo.getSenderList();
            for (SenderModel senderModel : senderList) {
                SenderUtil.addSender(senderModel);
            }

            RuleUtil.delRule(null);
            List<RuleModel> ruleList = cloneInfoVo.getRuleList();
            for (RuleModel ruleModel : ruleList) {
                RuleUtil.addRule(ruleModel);
            }

            LogUtil.delLog(null, null);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
