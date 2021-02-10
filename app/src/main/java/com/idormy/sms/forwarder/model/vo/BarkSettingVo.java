package com.idormy.sms.forwarder.model.vo;

import java.io.Serializable;

public class BarkSettingVo implements Serializable {
    private String server;

    public BarkSettingVo() {
    }

    public BarkSettingVo(String server) {
        this.server = server;
    }

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }
}
