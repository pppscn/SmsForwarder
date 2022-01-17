package com.idormy.sms.forwarder.model.vo;

import java.io.Serializable;

import lombok.Data;

@Data
public class GotifySettingVo implements Serializable {
    private String webServer;
    private String title;
    private String priority;

    public GotifySettingVo() {
    }

    public GotifySettingVo(String webServer, String title, String priority) {
        this.webServer = webServer;
        this.title = title;
        this.priority = priority;
    }

}
