package com.idormy.sms.forwarder.model.vo;

import com.idormy.sms.forwarder.R;

import java.io.Serializable;
import java.net.Proxy;

import lombok.Data;

@Data
public class TelegramSettingVo implements Serializable {
    private String apiToken;
    private String chatId;
    private Proxy.Type proxyType = Proxy.Type.DIRECT;
    private String proxyHost;
    private String proxyPort;
    private Boolean proxyAuthenticator = false;
    private String proxyUsername;
    private String proxyPassword;

    public TelegramSettingVo() {
    }

    public TelegramSettingVo(String apiToken, String chatId) {
        this.apiToken = apiToken;
        this.chatId = chatId;
        this.proxyType = Proxy.Type.DIRECT;
    }

    public TelegramSettingVo(String apiToken, String chatId, int proxyTypeId, String proxyHost, String proxyPort, boolean proxyAuthenticator, String proxyUsername, String proxyPassword) {
        this.apiToken = apiToken;
        this.chatId = chatId;
        if (proxyTypeId == R.id.btnProxyHttp) {
            this.proxyType = Proxy.Type.HTTP;
        } else if (proxyTypeId == R.id.btnProxySocks) {
            this.proxyType = Proxy.Type.SOCKS;
        } else {
            this.proxyType = Proxy.Type.DIRECT;
        }
        this.proxyHost = proxyHost;
        this.proxyPort = proxyPort;
        this.proxyAuthenticator = proxyAuthenticator;
        this.proxyUsername = proxyUsername;
        this.proxyPassword = proxyPassword;
    }

    public int getProxyTypeCheckId() {
        if (proxyType == Proxy.Type.HTTP) {
            return R.id.btnProxyHttp;
        } else if (proxyType == Proxy.Type.SOCKS) {
            return R.id.btnProxySocks;
        } else {
            return R.id.btnProxyNone;
        }
    }
}
