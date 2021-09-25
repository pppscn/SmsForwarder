package com.idormy.sms.forwarder.model.vo;

import java.io.Serializable;

import lombok.Data;

@Data
public class DingDingSettingVo implements Serializable {
    private String token;
    private String secret;
    private String atMobils;
    private Boolean atAll;

    public DingDingSettingVo() {
    }

    public DingDingSettingVo(String token, String secret, String atMobils, Boolean atAll) {
        this.token = token;
        this.secret = secret;
        this.atMobils = atMobils;
        this.atAll = atAll;
    }
}
