package com.idormy.sms.forwarder.model.vo;

import android.annotation.SuppressLint;
import android.util.Log;

import androidx.annotation.NonNull;

import com.idormy.sms.forwarder.MyApplication;
import com.idormy.sms.forwarder.R;
import com.idormy.sms.forwarder.utils.SettingUtils;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

import lombok.Data;

@Data
public class SmsVo implements Serializable {
    String mobile;
    String content;
    Date date;
    String simInfo;

    public SmsVo() {
    }

    public SmsVo(String mobile, String content, Date date, String simInfo) {
        this.mobile = mobile;
        this.content = content;
        this.date = date;
        this.simInfo = simInfo;
    }

    @SuppressLint("SimpleDateFormat")
    public String getTitleForSend() {
        return getTitleForSend("", "");
    }


    @SuppressLint("SimpleDateFormat")
    public String getTitleForSend(String titleTemplate) {
        return getTitleForSend(titleTemplate, "");
    }

    @SuppressLint("SimpleDateFormat")
    public String getTitleForSend(String titleTemplate, String regexReplace) {
        if (titleTemplate == null || titleTemplate.isEmpty()) titleTemplate = getString(R.string.tag_from);

        String deviceMark = SettingUtils.getAddExtraDeviceMark().trim();
        String versionName = SettingUtils.getVersionName();
        String titleForSend = titleTemplate.replace(getString(R.string.tag_from), mobile)
                .replace(getString(R.string.tag_package_name), mobile)
                .replace(getString(R.string.tag_sms), content)
                .replace(getString(R.string.tag_msg), content)
                .replace(getString(R.string.tag_card_slot), simInfo)
                .replace(getString(R.string.tag_title), simInfo)
                .replace(getString(R.string.tag_receive_time), new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date))
                .replace(getString(R.string.tag_device_name), deviceMark)
                .replace(getString(R.string.tag_app_version), versionName)
                .trim();

        return RegexReplace(regexReplace, titleForSend);
    }

    @SuppressLint("SimpleDateFormat")
    public String getSmsVoForSend() {
        return getSmsVoForSend("", "");
    }

    @SuppressLint("SimpleDateFormat")
    public String getSmsVoForSend(String ruleSmsTemplate) {
        return getSmsVoForSend(ruleSmsTemplate, "");
    }

    @SuppressLint("SimpleDateFormat")
    public String getSmsVoForSend(String ruleSmsTemplate, String regexReplace) {
        String deviceMark = SettingUtils.getAddExtraDeviceMark().trim();
        String customSmsTemplate = getString(R.string.tag_from) + "\n" +
                getString(R.string.tag_sms) + "\n" +
                getString(R.string.tag_card_slot) + "\n" +
                getString(R.string.tag_receive_time) + "\n" +
                getString(R.string.tag_device_name);

        //优先取转发规则的自定义模板，留空则取全局设置
        if (!ruleSmsTemplate.isEmpty()) {
            customSmsTemplate = ruleSmsTemplate;
        } else {
            boolean switchSmsTemplate = SettingUtils.getSwitchSmsTemplate();
            String smsTemplate = SettingUtils.getSmsTemplate().trim();
            if (switchSmsTemplate && !smsTemplate.isEmpty()) {
                customSmsTemplate = smsTemplate;
            }
        }

        String versionName = SettingUtils.getVersionName();
        String smsVoForSend = customSmsTemplate.replace(getString(R.string.tag_from), mobile)
                .replace(getString(R.string.tag_package_name), mobile)
                .replace(getString(R.string.tag_sms), content)
                .replace(getString(R.string.tag_msg), content)
                .replace(getString(R.string.tag_card_slot), simInfo)
                .replace(getString(R.string.tag_title), simInfo)
                .replace(getString(R.string.tag_receive_time), new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date))
                .replace(getString(R.string.tag_device_name), deviceMark)
                .replace(getString(R.string.tag_app_version), versionName)
                .trim();

        return RegexReplace(regexReplace, smsVoForSend);
    }

    //正则替换内容
    private String RegexReplace(String regexReplace, String Content) {
        if (regexReplace == null || regexReplace.isEmpty()) return Content;

        try {
            String newContent = Content;
            String[] lineArray = regexReplace.split("\\n");
            for (String line : lineArray) {
                String[] lineSplit = line.split("===");
                if (lineSplit.length >= 1) {
                    String regex = lineSplit[0];
                    String replacement = lineSplit.length >= 2 ? lineSplit[1] : "";
                    newContent = newContent.replaceAll(regex, replacement);
                }
            }
            return newContent;
        } catch (Exception e) {
            Log.e("RegexReplace", "Failed to get the receiving phone number:" + e.getMessage());
            return Content;
        }
    }

    private static String getString(int resId) {
        return MyApplication.getContext().getString(resId);
    }

    @NonNull
    @Override
    public String toString() {
        return "SmsVo{" +
                "mobile='" + mobile + '\'' +
                ", content='" + content + '\'' +
                ", date=" + date +
                ", simInfo=" + simInfo +
                '}';
    }

}
