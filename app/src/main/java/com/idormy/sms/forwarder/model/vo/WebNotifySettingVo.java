package com.idormy.sms.forwarder.model.vo;

import java.io.Serializable;

public class WebNotifySettingVo implements Serializable {
    private String token;
    private String secret;

    public WebNotifySettingVo() {
    }

    public WebNotifySettingVo(String token, String secret) {
        this.token = token;
        this.secret = secret;
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
}
