package com.idormy.sms.forwarder.model.vo;

import java.io.Serializable;

import lombok.Data;

@Data
public class QYWXGroupRobotSettingVo implements Serializable {
    private String webHook;

    public QYWXGroupRobotSettingVo() {
    }

    public QYWXGroupRobotSettingVo(String webHook) {
        this.webHook = webHook;
    }

}
