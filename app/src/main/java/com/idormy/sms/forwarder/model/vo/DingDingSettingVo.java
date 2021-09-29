package com.idormy.sms.forwarder.model.vo;

import java.io.Serializable;

import lombok.Data;

@Data
public class DingDingSettingVo implements Serializable {
    private String token;
    private String secret;
    private String atMobiles;
    private Boolean atAll;

    public DingDingSettingVo() {
    }

    public DingDingSettingVo(String token, String secret, String atMobiles, Boolean atAll) {
        this.token = token;
        this.secret = secret;
        this.atMobiles = atMobiles;
        this.atAll = atAll;
    }
}
