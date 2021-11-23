package com.idormy.sms.forwarder.model.vo;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;

import com.idormy.sms.forwarder.utils.SettingUtil;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

import lombok.Data;

@Data
public class SmsVo implements Serializable {
    String mobile;
    String content;
    Date date;
    String simInfo;

    public SmsVo() {
    }

    public SmsVo(String mobile, String content, Date date, String simInfo) {
        this.mobile = mobile;
        this.content = content;
        this.date = date;
        this.simInfo = simInfo;
    }

    @SuppressLint("SimpleDateFormat")
    public String getTitleForSend(String titleTemplate) {
        if (titleTemplate == null || titleTemplate.isEmpty()) titleTemplate = "{{来源号码}}";

        String deviceMark = SettingUtil.getAddExtraDeviceMark().trim();
        String versionName = SettingUtil.getVersionName();
        return titleTemplate.replace("{{来源号码}}", mobile).replace("{{APP包名}}", mobile)
                .replace("{{短信内容}}", content).replace("{{通知内容}}", content)
                .replace("{{卡槽信息}}", simInfo)
                .replace("{{接收时间}}", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date))
                .replace("{{设备名称}}", deviceMark)
                .replace("{{当前应用版本号}}", versionName)
                .trim();
    }

    @SuppressLint("SimpleDateFormat")
    public String getSmsVoForSend(String ruleSmsTemplate) {
        String deviceMark = SettingUtil.getAddExtraDeviceMark().trim();
        String customSmsTemplate = "{{来源号码}}\n{{短信内容}}\n{{卡槽信息}}\n{{接收时间}}\n{{设备名称}}";

        //优先取转发规则的自定义模板，留空则取全局设置
        if (!ruleSmsTemplate.isEmpty()) {
            customSmsTemplate = ruleSmsTemplate;
        } else {
            boolean switchSmsTemplate = SettingUtil.getSwitchSmsTemplate();
            String smsTemplate = SettingUtil.getSmsTemplate().trim();
            if (switchSmsTemplate && !smsTemplate.isEmpty()) {
                customSmsTemplate = smsTemplate;
            }
        }

        String versionName = SettingUtil.getVersionName();
        return customSmsTemplate.replace("{{来源号码}}", mobile).replace("{{APP包名}}", mobile)
                .replace("{{短信内容}}", content).replace("{{通知内容}}", content)
                .replace("{{卡槽信息}}", simInfo)
                .replace("{{接收时间}}", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date))
                .replace("{{设备名称}}", deviceMark)
                .replace("{{当前应用版本号}}", versionName)
                .trim();
    }

    @NonNull
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
