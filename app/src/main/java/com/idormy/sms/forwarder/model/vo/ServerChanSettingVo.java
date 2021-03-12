package com.idormy.sms.forwarder.model.vo;

import java.io.Serializable;

public class ServerChanSettingVo implements Serializable {
    private String send_key;

    public ServerChanSettingVo() {
    }

    public ServerChanSettingVo(String send_key) {
        this.send_key = send_key;
    }

    public String getSendKey() {
        return send_key;
    }

    public void setSendKey(String send_key) {
        this.send_key = send_key;
    }
}
