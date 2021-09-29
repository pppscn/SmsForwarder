package com.idormy.sms.forwarder.model.vo;

import com.idormy.sms.forwarder.R;

import lombok.Data;

@Data
public class LogVo {
    private Long id;
    private String from;
    private String content;
    private String simInfo;
    private String rule;
    private int senderImageId;
    private String time;
    private int forwardStatus;
    private String forwardResponse;

    public LogVo() {
    }

    public LogVo(Long id, String from, String content, String simInfo, String time, String rule, int senderImageId, int forwardStatus, String forwardResponse) {
        this.id = id;
        this.from = from;
        this.content = content;
        this.simInfo = simInfo;
        this.time = time;
        this.rule = rule;
        this.senderImageId = senderImageId;
        this.forwardStatus = forwardStatus;
        this.forwardResponse = forwardResponse;
    }

    public int getSimImageId() {
        if (this.simInfo != null && !this.simInfo.isEmpty()
                && this.simInfo.replace("-", "").startsWith("SIM2")) {
            return R.mipmap.sim2;
        }

        return R.mipmap.sim1;
    }

    public int getStatusImageId() {
        if (this.forwardStatus == 1) {
            return R.drawable.ic_round_check;
        }

        return R.drawable.ic_round_cancel;
    }
}
