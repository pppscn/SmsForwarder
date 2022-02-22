package com.idormy.sms.forwarder.model.vo;

import com.idormy.sms.forwarder.R;

import java.io.Serializable;

import lombok.Data;

@Data
public class FeiShuSettingVo implements Serializable {
    private String webhook;
    private String secret;
    private String msgType;
    private String titleTemplate;

    public FeiShuSettingVo() {
    }

    public FeiShuSettingVo(String webhook, String secret, String msgType, String titleTemplate) {
        this.webhook = webhook;
        this.secret = secret;
        this.msgType = msgType;
        this.titleTemplate = titleTemplate;
    }

    public int getMsgTypeCheckId() {
        if (msgType == null || msgType.equals("interactive")) {
            return R.id.radioFeishuMsgTypeInteractive;
        } else {
            return R.id.radioFeishuMsgTypeText;
        }
    }
}
