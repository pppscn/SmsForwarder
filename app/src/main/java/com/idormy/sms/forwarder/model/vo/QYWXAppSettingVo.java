package com.idormy.sms.forwarder.model.vo;

import java.io.Serializable;

public class QYWXAppSettingVo implements Serializable {
    private String corpID;
    private String agentID;
    private String secret;
    private String toUser;
    private Boolean atAll;

    public QYWXAppSettingVo() {
    }

    public QYWXAppSettingVo(String corpID, String agentID, String secret, String toUser, Boolean atAll) {
        this.corpID = corpID;
        this.agentID = agentID;
        this.secret = secret;
        this.toUser = toUser;
        this.atAll = atAll;
    }

    public String getCorpID() {
        return corpID;
    }

    public void setCorpID(String corpID) {
        this.corpID = corpID;
    }

    public String getAgentID() {
        return agentID;
    }

    public void setAgentID(String agentID) {
        this.agentID = agentID;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getToUser() {
        return toUser;
    }

    public void setToUser(String toUser) {
        this.toUser = toUser;
    }

    public Boolean getAtAll() {
        return atAll;
    }

    public void setAtAll(Boolean atAll) {
        this.atAll = atAll;
    }
}
