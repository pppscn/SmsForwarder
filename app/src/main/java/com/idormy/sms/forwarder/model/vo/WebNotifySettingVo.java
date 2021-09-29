package com.idormy.sms.forwarder.model.vo;

import com.idormy.sms.forwarder.R;

import java.io.Serializable;

import lombok.Data;

@Data
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

    public int getWebNotifyMethodCheckId() {
        if (method == null || method.equals("POST")) {
            return R.id.radioWebNotifyMethodPost;
        } else {
            return R.id.radioWebNotifyMethodGet;
        }
    }


}
