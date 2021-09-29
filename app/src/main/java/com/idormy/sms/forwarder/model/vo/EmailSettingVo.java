package com.idormy.sms.forwarder.model.vo;

import java.io.Serializable;

import lombok.Data;

@Data
public class EmailSettingVo implements Serializable {
    private String host;
    private String port;
    private Boolean ssl;
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

}
