package com.idormy.sms.forwarder.model.vo;

import java.io.Serializable;

public class QYWXGroupRobotSettingVo implements Serializable {
    private String webHook;

    public QYWXGroupRobotSettingVo() {
    }

    public QYWXGroupRobotSettingVo(String webHook) {
        this.webHook = webHook;
    }

    public String getWebHook() {
        return webHook;
    }

    public void setWebHook(String webHook) {
        this.webHook = webHook;
    }
}
