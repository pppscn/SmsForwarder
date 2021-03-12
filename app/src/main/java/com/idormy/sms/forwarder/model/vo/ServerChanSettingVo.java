package com.idormy.sms.forwarder.model.vo;

import java.io.Serializable;

public class ServerChanSettingVo implements Serializable {
    private String sendKey;

    public ServerChanSettingVo() {
    }

    public ServerChanSettingVo(String sendKey) {
        this.sendKey = sendKey;
    }

    public String getSendKey() {
        return sendKey;
    }

    public void setSendKey(String sendKey) {
        this.sendKey = sendKey;
    }
}
