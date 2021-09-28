package com.idormy.sms.forwarder.utils;

import android.os.Environment;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

@SuppressWarnings("rawtypes")
public class BuildProperties {
    private static BuildProperties ourInstance;
    private final Properties properties;

    private BuildProperties() throws IOException {
        properties = new Properties();
        properties.load(new FileInputStream(new File(Environment.getRootDirectory(), "build.prop")));
    }

    public static BuildProperties getInstance() throws IOException {
        if (ourInstance == null) ourInstance = new BuildProperties();
        return ourInstance;
    }

    public boolean containsKey(final Object key) {
        return properties.containsKey(key);
    }

    public boolean containsValue(final Object value) {
        return properties.containsValue(value);
    }

    public String getProperty(final String name) {
        return properties.getProperty(name);
    }

    public String getProperty(final String name, final String defaultValue) {
        return properties.getProperty(name, defaultValue);
    }

    public Set<Map.Entry<Object, Object>> entrySet() {
        return properties.entrySet();
    }

    public boolean isEmpty() {
        return properties.isEmpty();
    }

    public Enumeration keys() {
        return properties.keys();
    }

    public Set keySet() {
        return properties.keySet();
    }

    public int size() {
        return properties.size();
    }

    public Collection values() {
        return properties.values();
    }
}