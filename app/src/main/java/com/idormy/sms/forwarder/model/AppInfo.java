package com.idormy.sms.forwarder.model;

import android.content.Intent;
import android.graphics.drawable.Drawable;

import lombok.Data;

@Data
public class AppInfo {
    public String pkgName;
    public String appName;
    public Drawable appIcon;
    public Intent appIntent;
    public String verName;
    public int verCode;

    public AppInfo() {
    }

    public AppInfo(String appName) {
        this.appName = appName;
    }

    public AppInfo(String appName, String pkgName) {
        this.appName = appName;
        this.pkgName = pkgName;
    }

    public AppInfo(String appName, String pkgName, Drawable appIcon, String verName, int verCode) {
        this.appName = appName;
        this.pkgName = pkgName;
        this.appIcon = appIcon;
        this.verName = verName;
        this.verCode = verCode;
    }

    @Override
    public String toString() {
        return "AppInfo{" +
                "appName='" + appName + '\'' +
                ", pkgName='" + pkgName + '\'' +
                ", appIcon=" + appIcon +
                ", verName=" + verName +
                ", verCode=" + verCode +
                '}';
    }
}
