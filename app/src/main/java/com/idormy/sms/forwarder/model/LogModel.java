package com.idormy.sms.forwarder.model;

import androidx.annotation.NonNull;

import lombok.Data;

@Data
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

    @NonNull
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
