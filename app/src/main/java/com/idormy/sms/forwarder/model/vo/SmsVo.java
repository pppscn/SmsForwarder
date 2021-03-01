package com.idormy.sms.forwarder.model.vo;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

public class SmsVo implements Serializable {
    String mobile;
    String content;
    Date date;
    String phoneNumber = "本机号码：未知";

    public SmsVo() {
    }

    public SmsVo(String mobile, String content, Date date, String phoneNumber) {
        this.mobile = mobile;
        this.content = content;
        this.date = date;
        this.phoneNumber = phoneNumber;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getSmsVoForSend() {
        return mobile + "\n" +
                content + "\n" +
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date) + "\n" +
                phoneNumber;
    }

    @Override
    public String toString() {
        return "SmsVo{" +
                "mobile='" + mobile + '\'' +
                ", content='" + content + '\'' +
                ", date=" + date +
                ", phoneNumber=" + phoneNumber +
                '}';
    }
}
