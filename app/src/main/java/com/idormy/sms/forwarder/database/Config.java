package com.idormy.sms.forwarder.database;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.util.Objects;

@Entity

public class Config {

    @PrimaryKey
    @NonNull
    private String uid;
    private String name;
    private String cfg;
    @Ignore
    private Boolean connecting;

    @Ignore
    public Config() {
    }

    @Ignore
    public Config(String cfg) {
        this.cfg = cfg;
    }

    public Config(@NonNull String uid, String name, String cfg) {
        this.uid = uid;
        this.name = name;
        this.cfg = cfg;
    }

    @NonNull
    public String getUid() {
        return uid;
    }

    public Config setUid(@NonNull String uid) {
        this.uid = uid;
        return this;
    }

    public String getName() {
        return name;
    }

    public Config setName(String name) {
        this.name = name;
        return this;
    }

    public Boolean getConnecting() {
        return connecting;
    }

    public Config setConnecting(Boolean connecting) {
        this.connecting = connecting;
        return this;
    }

    public String getCfg() {
        return cfg;
    }

    public Config setCfg(String cfg) {
        this.cfg = cfg;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Config config = (Config) o;
        return Objects.equals(uid, config.uid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uid);
    }

    @Override
    public String toString() {
        return "Config{" +
                "uid='" + uid + '\'' +
                ", cfg='" + cfg + '\'' +
                '}';
    }
}
