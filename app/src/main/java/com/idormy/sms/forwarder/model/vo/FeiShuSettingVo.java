package com.idormy.sms.forwarder.model.vo;

import com.idormy.sms.forwarder.R;

import java.io.Serializable;

import lombok.Data;

@Data
public class FeiShuSettingVo implements Serializable {
    private String webhook;
    private String secret;
    private String msgType;

    public FeiShuSettingVo() {
    }

    public FeiShuSettingVo(String webhook, String secret, String msgType) {
        this.webhook = webhook;
        this.secret = secret;
        this.msgType = msgType;
    }

    public int getMsgTypeCheckId() {
        if (msgType == null || msgType.equals("interactive")) {
            return R.id.radioFeishuMsgTypeInteractive;
        } else {
            return R.id.radioFeishuMsgTypeText;
        }
    }
}
