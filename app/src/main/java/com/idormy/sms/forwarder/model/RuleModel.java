package com.idormy.sms.forwarder.model;

import android.annotation.SuppressLint;
import android.util.Log;

import androidx.annotation.NonNull;

import com.idormy.sms.forwarder.MyApplication;
import com.idormy.sms.forwarder.R;
import com.idormy.sms.forwarder.model.vo.SmsVo;
import com.idormy.sms.forwarder.utils.RuleLineUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import lombok.Data;

@SuppressWarnings({"unused"})
@Data
public class RuleModel {
    public static final int STATUS_ON = 1;
    public static final int STATUS_OFF = 0;
    public static final String FILED_TRANSPOND_ALL = "transpond_all";
    public static final String FILED_PHONE_NUM = "phone_num";
    public static final String FILED_PACKAGE_NAME = "package_name";
    public static final String FILED_MSG_CONTENT = "msg_content";
    public static final String FILED_INFORM_CONTENT = "inform_content";
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
    public static final Map<String, String> TYPE_MAP = new HashMap<>();

    static {
        TYPE_MAP.put("sms", getString(R.string.rule_sms));
        TYPE_MAP.put("call", getString(R.string.rule_call));
        TYPE_MAP.put("app", getString(R.string.rule_app));
    }

    static {
        FILED_MAP.put("transpond_all", getString(R.string.rule_transpond_all));
        FILED_MAP.put("phone_num", getString(R.string.rule_phone_num));
        FILED_MAP.put("msg_content", getString(R.string.rule_msg_content));
        FILED_MAP.put("multi_match", getString(R.string.rule_multi_match));
        FILED_MAP.put("package_name", getString(R.string.rule_package_name));
        FILED_MAP.put("inform_content", getString(R.string.rule_inform_content));
    }

    static {
        CHECK_MAP.put("is", getString(R.string.rule_is));
        CHECK_MAP.put("notis", getString(R.string.rule_notis));
        CHECK_MAP.put("contain", getString(R.string.rule_contain));
        CHECK_MAP.put("startwith", getString(R.string.rule_startwith));
        CHECK_MAP.put("endwith", getString(R.string.rule_endwith));
        CHECK_MAP.put("notcontain", getString(R.string.rule_notcontain));
        CHECK_MAP.put("regex", getString(R.string.rule_regex));
    }

    static {
        SIM_SLOT_MAP.put("ALL", getString(R.string.rule_all));
        SIM_SLOT_MAP.put("SIM1", "SIM1");
        SIM_SLOT_MAP.put("SIM2", "SIM2");
    }

    private String TAG = "RuleModel";
    private Long id;
    private String type;
    private String filed;
    private String check;
    private String value;
    private Long senderId;
    private Long time;
    private String simSlot;
    private boolean switchSmsTemplate;
    private String smsTemplate;
    private boolean switchRegexReplace;
    private String regexReplace;
    private int status;

    public static String getRuleMatch(String filed, String check, String value, String simSlot) {
        String SimStr = SIM_SLOT_MAP.get(simSlot) + getString(R.string.rule_card);
        if (filed == null || filed.equals(FILED_TRANSPOND_ALL)) {
            return SimStr + getString(R.string.rule_all_fw_to);
        } else {
            return SimStr + getString(R.string.rule_when) + FILED_MAP.get(filed) + " " + CHECK_MAP.get(check) + " " + value + getString(R.string.rule_fw_to);
        }
    }

    @SuppressLint("NonConstantResourceId")
    public static String getRuleFiledFromCheckId(int id) {
        switch (id) {
            case R.id.btnContent:
                return FILED_MSG_CONTENT;
            case R.id.btnPhone:
                return FILED_PHONE_NUM;
            case R.id.btnPackageName:
                return FILED_PACKAGE_NAME;
            case R.id.btnInformContent:
                return FILED_INFORM_CONTENT;
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
                case FILED_PACKAGE_NAME:
                    mixChecked = checkValue(msg.getMobile());
                    break;
                case FILED_MSG_CONTENT:
                case FILED_INFORM_CONTENT:
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
                            //noinspection LoopStatementThatDoesntLoop
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
        String SimStr = "app".equals(type) ? "" : SIM_SLOT_MAP.get(simSlot) + getString(R.string.rule_card);
        if (filed == null || filed.equals(FILED_TRANSPOND_ALL)) {
            return SimStr + getString(R.string.rule_all_fw_to);
        } else {
            return SimStr + getString(R.string.rule_when) + FILED_MAP.get(filed) + " " + CHECK_MAP.get(check) + " " + value + getString(R.string.rule_fw_to);
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
            case FILED_PACKAGE_NAME:
                return R.id.btnPackageName;
            case FILED_INFORM_CONTENT:
                return R.id.btnInformContent;
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

    public boolean getSwitchSmsTemplate() {
        return switchSmsTemplate;
    }

    public boolean getSwitchRegexReplace() {
        return switchRegexReplace;
    }

    public boolean getStatusChecked() {
        return !(status == STATUS_OFF);
    }


    public int getImageId() {
        switch (simSlot) {
            case (CHECK_SIM_SLOT_1):
                return R.drawable.sim1;
            case (CHECK_SIM_SLOT_2):
                return R.drawable.sim2;
            case (CHECK_SIM_SLOT_ALL):
            default:
                return type.equals("app") ? R.drawable.ic_app : R.drawable.ic_sim;
        }
    }

    public int getStatusImageId() {
        switch (status) {
            case (STATUS_OFF):
                return R.drawable.ic_round_pause;
            case (STATUS_ON):
            default:
                return R.drawable.ic_round_play;
        }
    }

    private static String getString(int resId) {
        return MyApplication.getContext().getString(resId);
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
                ", status=" + status +
                '}';
    }
}
