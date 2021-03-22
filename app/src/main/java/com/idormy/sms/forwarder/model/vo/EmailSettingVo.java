package com.idormy.sms.forwarder.model.vo;

import java.io.Serializable;

public class EmailSettingVo implements Serializable {
    private String host;
    private String port;
    private Boolean ssl = true;
    private String fromEmail;
    private String nickname;
    private String pwd;
    private String toEmail;

    public EmailSettingVo() {
    }

    public EmailSettingVo(String host, String port, Boolean ssl, String fromEmail, String nickname, String pwd, String toEmail) {
        this.host = host;
        this.port = port;
        this.ssl = ssl;
        this.fromEmail = fromEmail;
        this.nickname = nickname;
        this.pwd = pwd;
        this.toEmail = toEmail;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public Boolean getSsl() {
        return ssl;
    }

    public void setSsl(Boolean ssl) {
        this.ssl = ssl;
    }

    public String getFromEmail() {
        return fromEmail;
    }

    public void setFromEmail(String fromEmail) {
        this.fromEmail = fromEmail;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getPwd() {
        return pwd;
    }

    public void setPwd(String pwd) {
        this.pwd = pwd;
    }

    public String getToEmail() {
        return toEmail;
    }

    public void setToEmail(String toEmail) {
        this.toEmail = toEmail;
    }
}
