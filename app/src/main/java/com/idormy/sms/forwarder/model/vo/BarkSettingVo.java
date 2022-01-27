package com.idormy.sms.forwarder.model.vo;

import com.idormy.sms.forwarder.R;

import java.io.Serializable;

import lombok.Data;

@Data
public class BarkSettingVo implements Serializable {
    private String server; //推送地址
    private String icon; //消息图标
    private String title; //标题模板
    private String level; //时效性
    private String sound; //声音
    private String badge; //角标
    private String url; //链接

    public BarkSettingVo() {
    }

    public BarkSettingVo(String server, String icon, String title, int levelId, String sound, String badge, String url) {
        this.server = server;
        this.icon = icon;
        this.title = title;
        this.level = getLevelStr(levelId);
        this.sound = sound;
        this.badge = badge;
        this.url = url;
    }

    public int getLevelId() {
        if (level.equals("timeSensitive")) {
            return R.id.btnBarkLevelTimeSensitive;
        } else if (level.equals("passive")) {
            return R.id.btnBarkLevelPassive;
        } else {
            return R.id.btnBarkLevelActive;
        }
    }

    public String getLevelStr(int id) {
        if (id == R.id.btnBarkLevelTimeSensitive) {
            return "timeSensitive";
        } else if (id == R.id.btnBarkLevelPassive) {
            return "passive";
        } else {
            return "active";
        }
    }
}
