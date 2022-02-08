package com.idormy.sms.forwarder.model.vo;

import androidx.annotation.NonNull;

import java.io.Serializable;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class CloneInfoVo implements Serializable {

    private int versionCode;
    private String versionName;
    private boolean enableSms;
    private boolean enablePhone;
    private boolean callType1;
    private boolean callType2;
    private boolean callType3;
    private boolean enableAppNotify;
    private boolean cancelAppNotify;
    private String smsHubApiUrl;
    private int batteryLevelAlarmMin;
    private int batteryLevelAlarmMax;
    private boolean batteryLevelAlarmOnce;
    private int retryTimes;
    private int delayTime;
    private boolean enableSmsTemplate;
    private String smsTemplate;
    private String backupVersion;

    @NonNull
    @Override
    public String toString() {
        return "CloneInfoVo{" +
                "versionCode='" + versionCode + '\'' +
                ", versionName='" + versionName + '\'' +
                ", enableSms=" + enableSms +
                ", enablePhone=" + enablePhone +
                ", callType1=" + callType1 +
                ", callType2=" + callType2 +
                ", callType3=" + callType3 +
                ", enableAppNotify=" + enableAppNotify +
                ", cancelAppNotify=" + cancelAppNotify +
                ", smsHubApiUrl=" + smsHubApiUrl +
                ", batteryLevelAlarmMin=" + batteryLevelAlarmMin +
                ", batteryLevelAlarmMax=" + batteryLevelAlarmMax +
                ", batteryLevelAlarmOnce=" + batteryLevelAlarmOnce +
                ", retryTimes=" + retryTimes +
                ", delayTime=" + delayTime +
                ", enableSmsTemplate=" + enableSmsTemplate +
                ", smsTemplate=" + smsTemplate +
                ", backupVersion=" + backupVersion +
                '}';
    }
}
