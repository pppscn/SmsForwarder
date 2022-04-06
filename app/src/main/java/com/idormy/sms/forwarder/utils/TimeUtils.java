package com.idormy.sms.forwarder.utils;

import android.annotation.SuppressLint;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class TimeUtils {

    //友好时间显示
    public static String friendlyTime(String utcTime) {
        @SuppressLint("SimpleDateFormat") SimpleDateFormat utcFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        utcFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));//时区定义并进行时间获取
        Date utcDate;
        try {
            utcDate = utcFormatter.parse(utcTime);
        } catch (ParseException e) {
            e.printStackTrace();
            return utcTime;
        }

        //获取utcDate距离当前的秒数
        assert utcDate != null;
        int ct = (int) ((System.currentTimeMillis() - utcDate.getTime()) / 1000);

        if (ct == 0) {
            return "刚刚";
        }

        if (ct > 0 && ct < 60) {
            return ct + "秒前";
        }

        if (ct >= 60 && ct < 3600) {
            return Math.max(ct / 60, 1) + "分钟前";
        }
        if (ct >= 3600 && ct < 86400) {
            return ct / 3600 + "小时前";
        }
        if (ct >= 86400 && ct < 2592000) { //86400 * 30
            int day = ct / 86400;
            return day + "天前";
        }
        if (ct >= 2592000 && ct < 31104000) { //86400 * 30
            return ct / 2592000 + "月前";
        }

        return ct / 31104000 + "年前";
    }

    /**
     * 函数功能描述:UTC时间转本地时间格式
     *
     * @param utcTime UTC时间
     * @return 本地时间格式的时间
     */
    public static Date utc2LocalDate(String utcTime) throws ParseException {
        String utcTimePatten = "yyyy-MM-dd HH:mm:ss";
        @SuppressLint("SimpleDateFormat") SimpleDateFormat utcFormatter = new SimpleDateFormat(utcTimePatten);
        utcFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));//时区定义并进行时间获取

        return utcFormatter.parse(utcTime);
    }

    /**
     * 函数功能描述:UTC时间转本地时间格式
     *
     * @param utcTime UTC时间
     * @return 本地时间格式的时间
     */
    public static String utc2Local(String utcTime) {
        String localTimePatten = "yyyy-MM-dd HH:mm:ss";

        Date utcDate;
        try {
            utcDate = utc2LocalDate(utcTime);
        } catch (ParseException e) {
            e.printStackTrace();
            return utcTime;
        }

        @SuppressLint("SimpleDateFormat") SimpleDateFormat localFormatter = new SimpleDateFormat(localTimePatten);
        localFormatter.setTimeZone(TimeZone.getDefault());
        assert utcDate != null;
        return localFormatter.format(utcDate.getTime());
    }

    public static String getTimeString(String pattern) {
        return new SimpleDateFormat(pattern, Locale.CHINESE).format(new Date());
    }

    public static String getTimeString(long time, String pattern) {
        SimpleDateFormat df = new SimpleDateFormat(pattern, Locale.CHINESE);
        return df.format(new Date(time));
    }

}
