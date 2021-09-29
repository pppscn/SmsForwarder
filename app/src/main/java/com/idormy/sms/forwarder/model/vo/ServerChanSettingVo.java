package com.idormy.sms.forwarder.model.vo;

import java.io.Serializable;

import lombok.Data;

@Data
public class ServerChanSettingVo implements Serializable {
    private String sendKey;

    public ServerChanSettingVo() {
    }

    public ServerChanSettingVo(String sendKey) {
        this.sendKey = sendKey;
    }

}
