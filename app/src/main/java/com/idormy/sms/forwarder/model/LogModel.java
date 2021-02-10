package com.idormy.sms.forwarder.model;

public class LogModel {
    private String from;
    private String content;
    private Long ruleId;
    private Long time;

    public LogModel(String from, String content, Long ruleId) {
        this.from = from;
        this.content = content;
        this.ruleId = ruleId;
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

    public Long getRuleId() {
        return ruleId;
    }

    public void setRuleId(Long ruleId) {
        this.ruleId = ruleId;
    }

    public Long getTime() {
        return time;
    }

    @Override
    public String toString() {
        return "LogModel{" +
                "from='" + from + '\'' +
                ", content='" + content + '\'' +
                ", ruleId=" + ruleId +
                ", time=" + time +
                '}';
    }
}
