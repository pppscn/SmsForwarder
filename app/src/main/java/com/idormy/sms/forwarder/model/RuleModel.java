package com.idormy.sms.forwarder.model;

import android.annotation.SuppressLint;
import android.util.Log;

import androidx.annotation.NonNull;

import com.idormy.sms.forwarder.R;
import com.idormy.sms.forwarder.model.vo.SmsVo;
import com.idormy.sms.forwarder.utils.RuleLineUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import lombok.Data;

@SuppressWarnings({"unused", "LoopStatementThatDoesntLoop"})
@Data
public class RuleModel {
    public static final String FILED_TRANSPOND_ALL = "transpond_all";
    public static final String FILED_PHONE_NUM = "phone_num";
    public static final String FILED_MSG_CONTENT = "msg_content";
    public static final String FILED_MULTI_MATCH = "multi_match";
    public static final Map<String, String> FILED_MAP = new HashMap<>();
    public static final String CHECK_IS = "is";
    public static final String CHECK_CONTAIN = "contain";
    public static final String CHECK_NOT_CONTAIN = "notcontain";
    public static final String CHECK_START_WITH = "startwith";
    public static final String CHECK_END_WITH = "endwith";
    public static final String CHECK_NOT_IS = "notis";
    public static final String CHECK_REGEX = "regex";
    public static final Map<String, String> CHECK_MAP = new HashMap<>();
    public static final String CHECK_SIM_SLOT_ALL = "ALL";
    public static final String CHECK_SIM_SLOT_1 = "SIM1";
    public static final String CHECK_SIM_SLOT_2 = "SIM2";
    public static final Map<String, String> SIM_SLOT_MAP = new HashMap<>();

    static {
        FILED_MAP.put("transpond_all", "全部转发");
        FILED_MAP.put("phone_num", "手机号");
        FILED_MAP.put("msg_content", "内容");
        FILED_MAP.put("multi_match", "多重匹配");
    }

    static {
        CHECK_MAP.put("is", "是");
        CHECK_MAP.put("notis", "不是");
        CHECK_MAP.put("contain", "包含");
        CHECK_MAP.put("startwith", "开头是");
        CHECK_MAP.put("endwith", "结尾是");
        CHECK_MAP.put("notcontain", "不包含");
        CHECK_MAP.put("regex", "正则匹配");
    }

    static {
        SIM_SLOT_MAP.put("ALL", "全部");
        SIM_SLOT_MAP.put("SIM1", "SIM1");
        SIM_SLOT_MAP.put("SIM2", "SIM2");
    }

    private String TAG = "RuleModel";
    private Long id;
    private String filed;
    private String check;
    private String value;
    private Long senderId;
    private Long time;
    private String simSlot;

    public static String getRuleMatch(String filed, String check, String value, String simSlot) {
        String SimStr = SIM_SLOT_MAP.get(simSlot) + "卡 ";
        if (filed == null || filed.equals(FILED_TRANSPOND_ALL)) {
            return SimStr + "全部 转发到 ";
        } else {
            return SimStr + "当 " + FILED_MAP.get(filed) + " " + CHECK_MAP.get(check) + " " + value + " 转发到 ";
        }
    }

    @SuppressLint("NonConstantResourceId")
    public static String getRuleFiledFromCheckId(int id) {
        switch (id) {
            case R.id.btnContent:
                return FILED_MSG_CONTENT;
            case R.id.btnPhone:
                return FILED_PHONE_NUM;
            case R.id.btnMultiMatch:
                return FILED_MULTI_MATCH;
            default:
                return FILED_TRANSPOND_ALL;
        }
    }

    @SuppressLint("NonConstantResourceId")
    public static String getRuleCheckFromCheckId(int id) {
        switch (id) {
            case R.id.btnContain:
                return CHECK_CONTAIN;
            case R.id.btnStartWith:
                return CHECK_START_WITH;
            case R.id.btnEndWith:
                return CHECK_END_WITH;
            case R.id.btnRegex:
                return CHECK_REGEX;
            case R.id.btnNotContain:
                return CHECK_NOT_CONTAIN;
            default:
                return CHECK_IS;
        }
    }

    @SuppressLint("NonConstantResourceId")
    public static String getRuleSimSlotFromCheckId(int id) {
        switch (id) {
            case R.id.btnSimSlot1:
                return CHECK_SIM_SLOT_1;
            case R.id.btnSimSlot2:
                return CHECK_SIM_SLOT_2;
            default:
                return CHECK_SIM_SLOT_ALL;
        }
    }

    //字段分支
    public boolean checkMsg(SmsVo msg) throws Exception {

        //检查这一行和上一行合并的结果是否命中
        boolean mixChecked = false;
        if (msg != null) {
            //先检查规则是否命中
            switch (this.filed) {
                case FILED_TRANSPOND_ALL:
                    mixChecked = true;
                    break;
                case FILED_PHONE_NUM:
                    mixChecked = checkValue(msg.getMobile());
                    break;
                case FILED_MSG_CONTENT:
                    mixChecked = checkValue(msg.getContent());
                    break;
                case FILED_MULTI_MATCH:
                    mixChecked = RuleLineUtils.checkRuleLines(msg, this.value);
                    break;
                default:
                    break;

            }
        }


        Log.i(TAG, "rule:" + this + " checkMsg:" + msg + " checked:" + mixChecked);
        return mixChecked;

    }

    //内容分支
    public boolean checkValue(String msgValue) {
        boolean checked = false;

        if (this.value != null) {
            switch (this.check) {
                case CHECK_IS:
                    checked = this.value.equals(msgValue);
                    break;
                case CHECK_NOT_IS:
                    checked = !this.value.equals(msgValue);
                    break;
                case CHECK_CONTAIN:
                    if (msgValue != null) {
                        checked = msgValue.contains(this.value);
                    }
                    break;
                case CHECK_NOT_CONTAIN:
                    if (msgValue != null) {
                        checked = !msgValue.contains(this.value);
                    }
                    break;
                case CHECK_START_WITH:
                    if (msgValue != null) {
                        checked = msgValue.startsWith(this.value);
                    }
                    break;
                case CHECK_END_WITH:
                    if (msgValue != null) {
                        checked = msgValue.endsWith(this.value);
                    }
                    break;
                case CHECK_REGEX:
                    if (msgValue != null) {
                        try {
                            //checked = Pattern.matches(this.value, msgValue);
                            Pattern pattern = Pattern.compile(this.value, Pattern.CASE_INSENSITIVE);
                            Matcher matcher = pattern.matcher(msgValue);
                            while (matcher.find()) {
                                checked = true;
                                break;
                            }
                        } catch (PatternSyntaxException e) {
                            Log.d(TAG, "PatternSyntaxException: ");
                            Log.d(TAG, "Description: " + e.getDescription());
                            Log.d(TAG, "Index: " + e.getIndex());
                            Log.d(TAG, "Message: " + e.getMessage());
                            Log.d(TAG, "Pattern: " + e.getPattern());
                        }
                    }
                    break;
                default:
                    break;
            }
        }

        Log.i(TAG, "checkValue " + msgValue + " " + this.check + " " + this.value + " checked:" + checked);

        return checked;

    }

    public String getRuleMatch() {
        String SimStr = SIM_SLOT_MAP.get(simSlot) + "卡 ";
        if (filed == null || filed.equals(FILED_TRANSPOND_ALL)) {
            return SimStr + "全部 转发到 ";
        } else {
            return SimStr + "当 " + FILED_MAP.get(filed) + " " + CHECK_MAP.get(check) + " " + value + " 转发到 ";
        }
    }

    public Long getRuleSenderId() {
        return senderId;
    }

    public int getRuleFiledCheckId() {
        switch (filed) {
            case FILED_MSG_CONTENT:
                return R.id.btnContent;
            case FILED_PHONE_NUM:
                return R.id.btnPhone;
            case FILED_MULTI_MATCH:
                return R.id.btnMultiMatch;
            default:
                return R.id.btnTranspondAll;
        }
    }

    public int getRuleCheckCheckId() {
        switch (check) {
            case CHECK_CONTAIN:
                return R.id.btnContain;
            case CHECK_START_WITH:
                return R.id.btnStartWith;
            case CHECK_END_WITH:
                return R.id.btnEndWith;
            case CHECK_REGEX:
                return R.id.btnRegex;
            case CHECK_NOT_CONTAIN:
                return R.id.btnNotContain;
            default:
                return R.id.btnIs;
        }
    }

    public int getRuleSimSlotCheckId() {
        switch (simSlot) {
            case CHECK_SIM_SLOT_1:
                return R.id.btnSimSlot1;
            case CHECK_SIM_SLOT_2:
                return R.id.btnSimSlot2;
            default:
                return R.id.btnSimSlotAll;
        }
    }

    @NonNull
    @Override
    public String toString() {
        return "RuleModel{" +
                "id=" + id +
                ", filed='" + filed + '\'' +
                ", check='" + check + '\'' +
                ", value='" + value + '\'' +
                ", senderId=" + senderId +
                ", time=" + time +
                '}';
    }
}
