package com.idormy.sms.forwarder.model.vo;

import java.io.Serializable;

import lombok.Data;

@Data
public class FeiShuSettingVo implements Serializable {
    private String webhook;
    private String secret;

    public FeiShuSettingVo() {
    }

    public FeiShuSettingVo(String webhook, String secret) {
        this.webhook = webhook;
        this.secret = secret;
    }
}
