package com.idormy.sms.forwarder.model.vo;

import java.io.Serializable;

import lombok.Data;

@Data
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

}
