package com.idormy.sms.forwarder.model.vo;

import com.idormy.sms.forwarder.R;

import java.io.Serializable;
import java.util.Map;

import lombok.Data;

@Data
public class WebNotifySettingVo implements Serializable {
    private String webServer;
    private String secret;
    private String method;
    private String webParams;
    private Map<String, String> headers;

    public WebNotifySettingVo() {
    }

    public WebNotifySettingVo(String webServer, String secret, String method, String webParams, Map<String, String> headers) {
        this.webServer = webServer;
        this.secret = secret;
        this.method = method;
        this.webParams = webParams;
        this.headers = headers;
    }

    public int getWebNotifyMethodCheckId() {
        if (method == null || method.equals("POST")) {
            return R.id.radioWebNotifyMethodPost;
        } else {
            return R.id.radioWebNotifyMethodGet;
        }
    }


}
