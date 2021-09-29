package com.idormy.sms.forwarder.model.vo;

import java.io.Serializable;

import lombok.Data;

@Data
public class BarkSettingVo implements Serializable {
    private String server;

    public BarkSettingVo() {
    }

    public BarkSettingVo(String server) {
        this.server = server;
    }

}
