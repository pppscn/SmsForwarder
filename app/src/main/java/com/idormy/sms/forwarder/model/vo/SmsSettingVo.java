package com.idormy.sms.forwarder.model.vo;

import com.idormy.sms.forwarder.R;

import java.io.Serializable;

import lombok.Data;

@Data
public class SmsSettingVo implements Serializable {
    private int simSlot;
    private String mobiles;
    private Boolean onlyNoNetwork;

    public SmsSettingVo() {
    }

    public SmsSettingVo(int simSlot, String mobiles, Boolean onlyNoNetwork) {
        this.simSlot = simSlot;
        this.mobiles = mobiles;
        this.onlyNoNetwork = onlyNoNetwork;
    }

    public int getSmsSimSlotCheckId() {
        if (simSlot == 1) {
            return R.id.btnSmsSimSlot1;
        } else if (simSlot == 2) {
            return R.id.btnSmsSimSlot2;
        } else {
            return R.id.btnSmsSimSlotOrg;
        }
    }
}
