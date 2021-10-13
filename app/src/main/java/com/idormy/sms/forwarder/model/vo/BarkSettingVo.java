package com.idormy.sms.forwarder.model.vo;

import java.io.Serializable;

import lombok.Data;

@Data
public class BarkSettingVo implements Serializable {
    private String server;
    private String icon;

    public BarkSettingVo() {
    }

    public BarkSettingVo(String server, String icon) {
        this.server = server;
        this.icon = icon;
    }

}
