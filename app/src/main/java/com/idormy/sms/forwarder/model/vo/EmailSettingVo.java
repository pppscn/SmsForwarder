package com.idormy.sms.forwarder.model.vo;

import com.idormy.sms.forwarder.R;

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
    private String title;
    private String protocol;

    public EmailSettingVo() {
    }

    public EmailSettingVo(String protocol, String host, String port, Boolean ssl, String fromEmail, String nickname, String pwd, String toEmail, String title) {
        this.protocol = protocol;
        this.host = host;
        this.port = port;
        this.ssl = ssl;
        this.fromEmail = fromEmail;
        this.nickname = nickname;
        this.pwd = pwd;
        this.toEmail = toEmail;
        this.title = title;
    }

    public int getEmailProtocolCheckId() {
        if (protocol == null || protocol.equals("SMTP")) {
            return R.id.radioEmailProtocolSmtp;
        } else {
            return R.id.radioEmailProtocolImap;
        }
    }
}
