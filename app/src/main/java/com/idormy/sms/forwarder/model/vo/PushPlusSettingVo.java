package com.idormy.sms.forwarder.model.vo;

import java.io.Serializable;

import lombok.Data;

@Data
public class PushPlusSettingVo implements Serializable {
    private String token;
    private String topic;
    private String template;
    private String channel;
    private String webhook;
    private String callbackUrl;
    private String validTime;
    private String titleTemplate;

    public PushPlusSettingVo() {
    }

    public PushPlusSettingVo(String token, String topic, String template, String channel, String webhook, String callbackUrl, String validTime, String titleTemplate) {
        this.token = token;
        this.topic = topic;
        this.template = template;
        this.channel = channel;
        this.webhook = webhook;
        this.callbackUrl = callbackUrl;
        this.validTime = validTime;
        this.titleTemplate = titleTemplate;
    }
}
