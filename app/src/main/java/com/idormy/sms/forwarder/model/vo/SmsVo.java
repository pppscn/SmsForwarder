package com.idormy.sms.forwarder.model.vo;

import com.idormy.sms.forwarder.utils.SettingUtil;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

public class SmsVo implements Serializable {
    String mobile;
    String content;
    Date date;
    String simInfo = "SIM1_unknown_unknown";

    public SmsVo() {
    }

    public SmsVo(String mobile, String content, Date date, String simInfo) {
        this.mobile = mobile;
        this.content = content;
        this.date = date;
        this.simInfo = simInfo;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getSimInfo() {
        return simInfo;
    }

    public void setSimInfo(String simInfo) {
        this.simInfo = simInfo;
    }

    public String getSmsVoForSend() {
        boolean switchAddExtra = SettingUtil.getSwitchAddExtra();
        boolean switchSmsTemplate = SettingUtil.getSwitchSmsTemplate();
        String smsTemplate = SettingUtil.getSmsTemplate().trim();
        String deviceMark = SettingUtil.getAddExtraDeviceMark().trim();
        if (!switchSmsTemplate) {
            smsTemplate = "{{来源号码}}\n{{短信内容}}\n{{卡槽信息}}\n{{接收时间}}\n{{设备名称}}";
        }

        if (!switchAddExtra) {
            smsTemplate = smsTemplate.replace("{{卡槽信息}}\n", "").replace("{{卡槽信息}}", "");
        }

        return smsTemplate.replace("{{来源号码}}", mobile)
                .replace("{{短信内容}}", content)
                .replace("{{卡槽信息}}", simInfo)
                .replace("{{接收时间}}", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date))
                .replace("{{设备名称}}", deviceMark)
                .trim();
    }

    @Override
    public String toString() {
        return "SmsVo{" +
                "mobile='" + mobile + '\'' +
                ", content='" + content + '\'' +
                ", date=" + date +
                ", simInfo=" + simInfo +
                '}';
    }
}
