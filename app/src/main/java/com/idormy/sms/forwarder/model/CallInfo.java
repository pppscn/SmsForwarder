package com.idormy.sms.forwarder.model;

import androidx.annotation.NonNull;

import lombok.Data;

@Data
public class CallInfo {
    public String name;  //姓名
    public String number;  //号码
    public Long dateLong; //获取通话日期
    public int duration;//获取通话时长，值为多少秒
    public int type; //获取通话类型：1.呼入 2.呼出 3.未接
    public String viaNumber; //来源号码
    public int subscriptionId; //卡槽id

    public CallInfo() {
    }

    public CallInfo(String name, String number, Long dateLong, int duration, int type, String viaNumber, int subscriptionId) {
        this.name = name;
        this.number = number;
        this.dateLong = dateLong;
        this.duration = duration;
        this.type = type;
        this.viaNumber = viaNumber;
        this.subscriptionId = subscriptionId;
    }

    @NonNull
    @Override
    public String toString() {
        return "CallInfo{" +
                "name='" + name + '\'' +
                ", number='" + number + '\'' +
                ", dateLong=" + dateLong +
                ", duration=" + duration +
                ", type=" + type +
                ", viaNumber=" + viaNumber +
                ", subscriptionId=" + subscriptionId +
                '}';
    }
}
