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
    public String getSmsVoForSend() {
        boolean switchAddExtra = SettingUtil.getSwitchAddExtra();
        boolean switchAddDeviceName = SettingUtil.getSwitchAddDeviceName();
        boolean switchSmsTemplate = SettingUtil.getSwitchSmsTemplate();
        String smsTemplate = SettingUtil.getSmsTemplate().trim();
        String deviceMark = SettingUtil.getAddExtraDeviceMark().trim();
        if (!switchSmsTemplate) {
            smsTemplate = "{{来源号码}}\n{{短信内容}}\n{{卡槽信息}}\n{{接收时间}}\n{{设备名称}}";
        }

        return smsTemplate.replace("{{来源号码}}", mobile)
                .replace("{{短信内容}}", content)
                .replace("{{卡槽信息}}", simInfo)
                .replace("{{接收时间}}", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date))
                .replace("{{设备名称}}", deviceMark)
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
