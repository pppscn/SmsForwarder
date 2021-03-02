package com.idormy.sms.forwarder.model.vo;

public class LogVo {
    private String from;
    private String content;
    private String simInfo;
    private String rule;
    private int senderImageId;
    private String time;

    public LogVo(String from, String content, String simInfo, String time, String rule, int senderImageId) {
        this.from = from;
        this.content = content;
        this.simInfo = simInfo;
        this.time = time;
        this.rule = rule;
        this.senderImageId = senderImageId;
    }

    public LogVo() {

    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getSimInfo() {
        return simInfo;
    }

    public void setSimInfo(String simInfo) {
        this.simInfo = simInfo;
    }

    public String getRule() {
        return rule;
    }

    public void setRule(String rule) {
        this.rule = rule;
    }

    public String getTime() {
        return time;
    }

    public int getSenderImageId() {
        return senderImageId;
    }

    public void setSenderImageId(int senderImageId) {
        this.senderImageId = senderImageId;
    }
}
