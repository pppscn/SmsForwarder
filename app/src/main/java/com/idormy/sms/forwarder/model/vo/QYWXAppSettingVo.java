package com.idormy.sms.forwarder.model.vo;

import android.text.TextUtils;

import java.io.Serializable;

import lombok.Data;

@Data
public class QYWXAppSettingVo implements Serializable {
    private String corpID;
    private String agentID;
    private String secret;
    private String toUser;
    private Boolean atAll;
    private String accessToken;
    private Long expiresIn;

    public QYWXAppSettingVo() {
    }

    public QYWXAppSettingVo(String corpID, String agentID, String secret, String toUser, Boolean atAll) {
        this.corpID = corpID;
        this.agentID = agentID;
        this.secret = secret;
        this.toUser = toUser;
        this.atAll = atAll;
    }

    public String getAccessToken() {
        if (accessToken == null || accessToken.isEmpty() || expiresIn == null || System.currentTimeMillis() > expiresIn) {
            return null;
        }
        return accessToken;
    }

    public boolean checkParms() {
        if (TextUtils.isEmpty(corpID)) return false;
        if (TextUtils.isEmpty(agentID)) return false;
        return !TextUtils.isEmpty(secret);
    }

}
