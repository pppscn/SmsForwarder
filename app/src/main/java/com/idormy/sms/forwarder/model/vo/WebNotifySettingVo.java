package com.idormy.sms.forwarder.model.vo;

import com.idormy.sms.forwarder.R;

import java.io.Serializable;

public class WebNotifySettingVo implements Serializable {
    private String webServer;
    private String secret;
    private String method;

    public WebNotifySettingVo() {
    }

    public WebNotifySettingVo(String webServer, String secret, String method) {
        this.webServer = webServer;
        this.secret = secret;
        this.method = method;
    }

    public String getWebServer() {
        return webServer;
    }

    public void setWebServer(String webServer) {
        this.webServer = webServer;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public int getWebNotifyMethodCheckId() {
        if (method == null || method.equals("POST")) {
            return R.id.radioWebNotifyMethodPost;
        } else {
            return R.id.radioWebNotifyMethodGet;
        }
    }
}
