package com.idormy.sms.forwarder.model.vo;

import com.idormy.sms.forwarder.R;

import java.io.Serializable;

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

    public int getSimSlot() {
        return simSlot;
    }

    public void setSimSlot(int simSlot) {
        this.simSlot = simSlot;
    }

    public String getMobiles() {
        return mobiles;
    }

    public void setMobiles(String mobiles) {
        this.mobiles = mobiles;
    }

    public Boolean getOnlyNoNetwork() {
        return onlyNoNetwork;
    }

    public void setOnlyNoNetwork(Boolean onlyNoNetwork) {
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
