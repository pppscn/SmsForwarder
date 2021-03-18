package com.idormy.sms.forwarder.model;

import com.idormy.sms.forwarder.R;


public class SenderModel {
    public static final int STATUS_ON = 1;
    public static final int STATUS_OFF = 0;
    public static final int TYPE_DINGDING = 0;
    public static final int TYPE_EMAIL = 1;
    public static final int TYPE_BARK = 2;
    public static final int TYPE_WEB_NOTIFY = 3;
    public static final int TYPE_QYWX_GROUP_ROBOT = 4;
    public static final int TYPE_QYWX_APP = 5;
    public static final int TYPE_SERVER_CHAN = 6;
    public static final int TYPE_TELEGRAM = 7;
    public static final int TYPE_SMS = 8;
    private Long id;
    private String name;
    private int status;
    private int type;

    private String jsonSetting;

    private long time;

    public SenderModel() {
    }

    public SenderModel(String name, int status, int type, String jsonSetting) {
        this.name = name;
        this.status = status == STATUS_ON ? STATUS_ON : STATUS_OFF;
        this.type = type;
        this.jsonSetting = jsonSetting;
    }

    public static int getImageId(int type) {
        switch (type) {
            case (TYPE_DINGDING):
                return R.mipmap.dingding;
            case (TYPE_EMAIL):
                return R.mipmap.email;
            case (TYPE_BARK):
                return R.mipmap.bark;
            case (TYPE_WEB_NOTIFY):
                return R.mipmap.webhook;
            case (TYPE_QYWX_GROUP_ROBOT):
                return R.mipmap.qywx;
            case (TYPE_QYWX_APP):
                return R.mipmap.qywxapp;
            case (TYPE_SERVER_CHAN):
                return R.mipmap.serverchan;
            case (TYPE_TELEGRAM):
                return R.mipmap.telegram;
            case (TYPE_SMS):
                return R.mipmap.sms;
            default:
                return R.mipmap.sms;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status == STATUS_ON ? STATUS_ON : STATUS_OFF;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getJsonSetting() {
        return jsonSetting;
    }

    public void setJsonSetting(String jsonSetting) {
        this.jsonSetting = jsonSetting;
    }

    public int getImageId() {
        switch (type) {
            case (TYPE_DINGDING):
                return R.mipmap.dingding;
            case (TYPE_EMAIL):
                return R.mipmap.email;
            case (TYPE_BARK):
                return R.mipmap.bark;
            case (TYPE_WEB_NOTIFY):
                return R.mipmap.webhook;
            case (TYPE_QYWX_GROUP_ROBOT):
                return R.mipmap.qywx;
            case (TYPE_QYWX_APP):
                return R.mipmap.qywxapp;
            case (TYPE_SERVER_CHAN):
                return R.mipmap.serverchan;
            case (TYPE_TELEGRAM):
                return R.mipmap.telegram;
            default:
                return R.mipmap.sms;

        }
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public int getSmsSimSlotId(int id) {
        if (id == R.id.btnSmsSimSlot1) {
            return 1;
        } else if (id == R.id.btnSmsSimSlot2) {
            return 2;
        } else {
            return 0;
        }
    }

    @Override
    public String toString() {
        return "SenderModel{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", status=" + status +
                ", type=" + type +
                ", jsonSetting='" + jsonSetting + '\'' +
                ", time=" + time +
                '}';
    }
}
