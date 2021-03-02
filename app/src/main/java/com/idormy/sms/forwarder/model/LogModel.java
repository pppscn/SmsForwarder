package com.idormy.sms.forwarder.model;

public class LogModel {
    private String from;
    private String content;
    private Long ruleId;
    private Long time;
    private String simInfo;

    public LogModel(String from, String content, String simInfo, Long ruleId) {
        this.from = from;
        this.content = content;
        this.simInfo = simInfo;
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

    public String getSimInfo() {
        return simInfo;
    }

    public void setSimInfo(String simInfo) {
        this.simInfo = simInfo;
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

    public void setTime(Long time) {
        this.time = time;
    }

    @Override
    public String toString() {
        return "LogModel{" +
                "from='" + from + '\'' +
                ", content='" + content + '\'' +
                ", simInfo=" + simInfo +
                ", ruleId=" + ruleId +
                ", time=" + time +
                '}';
    }
}
