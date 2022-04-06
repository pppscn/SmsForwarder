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
            cloneInfo.setVersionCode(SettingUtils.getVersionCode());
            cloneInfo.setVersionName(SettingUtils.getVersionName());
            cloneInfo.setEnableSms(SettingUtils.getSwitchEnableSms());
            cloneInfo.setEnablePhone(SettingUtils.getSwitchEnablePhone());
            cloneInfo.setCallType1(SettingUtils.getSwitchCallType1());
            cloneInfo.setCallType2(SettingUtils.getSwitchCallType2());
            cloneInfo.setCallType3(SettingUtils.getSwitchCallType3());
            cloneInfo.setEnableAppNotify(SettingUtils.getSwitchEnableAppNotify());
            cloneInfo.setCancelAppNotify(SettingUtils.getSwitchCancelAppNotify());
            cloneInfo.setBatteryLevelAlarmMin(SettingUtils.getBatteryLevelAlarmMin());
            cloneInfo.setBatteryLevelAlarmMax(SettingUtils.getBatteryLevelAlarmMax());
            cloneInfo.setBatteryLevelAlarmOnce(SettingUtils.getBatteryLevelAlarmOnce());
            cloneInfo.setRetryTimes(SettingUtils.getRetryTimes());
            cloneInfo.setDelayTime(SettingUtils.getDelayTime());
            cloneInfo.setEnableSmsTemplate(SettingUtils.getSwitchSmsTemplate());
            cloneInfo.setSmsTemplate(SettingUtils.getSmsTemplate());
            cloneInfo.setSenderList(SenderUtil.getSender(null, null));
            cloneInfo.setRuleList(RuleUtils.getRule(null, null));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return JSON.toJSONString(cloneInfo);
    }

    //还原设置
    public static boolean restoreSettings(CloneInfoVo cloneInfoVo) {

        try {
            //应用配置
            //SettingUtils.init(context);
            SettingUtils.switchEnableSms(cloneInfoVo.isEnableSms());
            SettingUtils.switchEnablePhone(cloneInfoVo.isEnablePhone());
            SettingUtils.switchCallType1(cloneInfoVo.isCallType1());
            SettingUtils.switchCallType2(cloneInfoVo.isCallType2());
            SettingUtils.switchCallType3(cloneInfoVo.isCallType3());
            SettingUtils.switchEnableAppNotify(cloneInfoVo.isEnableAppNotify());
            SettingUtils.switchCancelAppNotify(cloneInfoVo.isCancelAppNotify());
            SettingUtils.setBatteryLevelAlarmMin(cloneInfoVo.getBatteryLevelAlarmMin());
            SettingUtils.setBatteryLevelAlarmMax(cloneInfoVo.getBatteryLevelAlarmMax());
            SettingUtils.switchBatteryLevelAlarmOnce(cloneInfoVo.isBatteryLevelAlarmOnce());
            SettingUtils.setRetryTimes(cloneInfoVo.getRetryTimes());
            SettingUtils.setDelayTime(cloneInfoVo.getDelayTime());
            SettingUtils.switchSmsTemplate(cloneInfoVo.isEnableSmsTemplate());
            SettingUtils.setSmsTemplate(cloneInfoVo.getSmsTemplate());

            SenderUtil.delSender(null);
            List<SenderModel> senderList = cloneInfoVo.getSenderList();
            for (SenderModel senderModel : senderList) {
                SenderUtil.addSender(senderModel);
            }

            RuleUtils.delRule(null);
            List<RuleModel> ruleList = cloneInfoVo.getRuleList();
            for (RuleModel ruleModel : ruleList) {
                RuleUtils.addRule(ruleModel);
            }

            LogUtils.delLog(null, null);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
