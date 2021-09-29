package com.idormy.sms.forwarder.model.vo;

import java.io.Serializable;

import lombok.Data;

@Data
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

}
