package com.idormy.sms.forwarder.model.vo;

import java.io.Serializable;

public class TelegramSettingVo implements Serializable {
    private String apiToken;
    private String chatId;

    public TelegramSettingVo() {
    }

    public TelegramSettingVo(String apiToken, String chatId) {
        this.apiToken = apiToken;
        this.chatId = chatId;
    }

    public String getApiToken() {
        return apiToken;
    }

    public void setApiToken(String apiToken) {
        this.apiToken = apiToken;
    }

    public String getChatId() {
        return chatId;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }
}
