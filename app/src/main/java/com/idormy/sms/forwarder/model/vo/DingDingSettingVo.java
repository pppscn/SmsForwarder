package com.idormy.sms.forwarder.model.vo;

import java.io.Serializable;

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

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getAtMobils() {
        return atMobils;
    }

    public void setAtMobils(String atMobils) {
        this.atMobils = atMobils;
    }

    public Boolean getAtAll() {
        return atAll;
    }

    public void setAtAll(Boolean atAll) {
        this.atAll = atAll;
    }
}
