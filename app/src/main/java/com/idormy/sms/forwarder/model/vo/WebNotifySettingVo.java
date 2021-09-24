package com.idormy.sms.forwarder.model.vo;

import com.idormy.sms.forwarder.R;

import java.io.Serializable;

public class WebNotifySettingVo implements Serializable {
    private String webServer;
    private String secret;
    private String method;
    private String webParams;

    public WebNotifySettingVo() {
    }

    public WebNotifySettingVo(String webServer, String secret, String method, String webParams) {
        this.webServer = webServer;
        this.secret = secret;
        this.method = method;
        this.webParams = webParams;
    }

    public String getWebServer() {
        return webServer;
    }

    public void setWebServer(String webServer) {
        this.webServer = webServer;
    }

    public String getwebParams() {
        return webParams;
    }

    public void setWebParams(String webParams) {
        this.webParams = webParams;
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
