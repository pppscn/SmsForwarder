package com.idormy.sms.forwarder.model;

import android.util.Log;

import com.idormy.sms.forwarder.R;
import com.idormy.sms.forwarder.model.vo.SmsVo;
import com.idormy.sms.forwarder.utils.RuleLineUtils;

import java.util.HashMap;
import java.util.Map;

public class RuleModel {
    private String TAG = "RuleModel";
    private Long id;
    public static final String FILED_TRANSPOND_ALL = "transpond_all";
    public static final String FILED_PHONE_NUM = "phone_num";
    public static final String FILED_MSG_CONTENT = "msg_content";
    public static final String FILED_MULTI_MATCH="multi_match";
    public static final Map<String, String> FILED_MAP = new HashMap<String, String>();
    static{
        FILED_MAP.put("transpond_all", "转发全部");
        FILED_MAP.put("phone_num", "手机号");
        FILED_MAP.put("msg_content", "内容");
        FILED_MAP.put("multi_match", "多重匹配");
    }
    private String filed;

    public static final String CHECK_IS = "is";
    public static final String CHECK_CONTAIN = "contain";
    public static final String CHECK_START_WITH = "startwith";
    public static final String CHECK_END_WITH = "endwith";
    public static final String CHECK_NOT_IS = "notis";
    public static final Map<String, String> CHECK_MAP = new HashMap<String, String>();

    static {
        CHECK_MAP.put("is", "是");
        CHECK_MAP.put("contain", "包含");
        CHECK_MAP.put("startwith", "开头是");
        CHECK_MAP.put("endwith", "结尾是");
        CHECK_MAP.put("notis", "不是");
    }
    private String check;

    private String value;

    private Long senderId;
    private Long time;

    //字段分支
    public boolean checkMsg(SmsVo msg) throws Exception {

        //检查这一行和上一行合并的结果是否命中
        boolean mixChecked=false;
        if(msg!=null){
            //先检查规则是否命中
            switch (this.filed){
                case FILED_TRANSPOND_ALL:
                    mixChecked= true;
                    break;
                case FILED_PHONE_NUM:
                    mixChecked= checkValue(msg.getMobile());
                    break;
                case FILED_MSG_CONTENT:
                    mixChecked= checkValue(msg.getContent());
                    break;
                case FILED_MULTI_MATCH:
                    mixChecked= RuleLineUtils.checkRuleLines(msg,this.value);
                    break;
                default:
                    break;

            }
        }


        Log.i(TAG, "rule:"+this+" checkMsg:"+msg+" checked:"+mixChecked);
        return mixChecked;

    }

    //内容分支
    public boolean checkValue(String msgValue){
        boolean checked=false;

        if(this.value!=null){
            switch (this.check){
                case CHECK_IS:
                    checked=this.value.equals(msgValue);
                    break;
                case CHECK_CONTAIN:
                    if(msgValue!=null){
                        checked=msgValue.contains(this.value);
                    }
                    break;
                case CHECK_START_WITH:
                    if(msgValue!=null){
                        checked=msgValue.startsWith(this.value);
                    }
                    break;
                case CHECK_END_WITH:
                    if(msgValue!=null){
                        checked=msgValue.endsWith(this.value);
                    }
                    break;
                default:
                    break;
            }
        }

        Log.i(TAG, "checkValue "+msgValue+" "+this.check+" "+this.value+" checked:"+checked);

        return checked;

    }


    public String getRuleMatch() {
        switch (filed){
            case FILED_TRANSPOND_ALL:
                return "全部转发到 ";
            default:
                return "当 "+FILED_MAP.get(filed)+" "+CHECK_MAP.get(check)+" "+value+" 转发到 ";
        }

    }
    public static String getRuleMatch(String filed, String check, String value) {
        switch (filed) {
            case FILED_TRANSPOND_ALL:
                return "全部转发到 ";
            default:
                return "当 " + FILED_MAP.get(filed) + " " + CHECK_MAP.get(check) + " " + value + " 转发到 ";

        }

    }

    public Long getRuleSenderId() {
        return senderId;
    }

    public int getRuleFiledCheckId(){
        switch (filed){
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

    public static String getRuleFiledFromCheckId(int id){
        switch (id){
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

    public int getRuleCheckCheckId(){
        switch (check){
            case CHECK_CONTAIN:
                return R.id.btnContain;
            case CHECK_START_WITH:
                return R.id.btnStartWith;
            case CHECK_END_WITH:
                return R.id.btnEndWith;
            case CHECK_NOT_IS:
                return R.id.btnNotIs;
            default:
                return R.id.btnIs;
        }
    }

    public static String getRuleCheckFromCheckId(int id){
        switch (id){
            case R.id.btnContain:
                return CHECK_CONTAIN;
            case R.id.btnStartWith:
                return CHECK_START_WITH;
            case R.id.btnEndWith:
                return CHECK_END_WITH;
            case R.id.btnNotIs:
                return CHECK_NOT_IS;
            default:
                return CHECK_IS;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSenderId() {
        return senderId;
    }

    public void setSenderId(Long senderId) {
        this.senderId = senderId;
    }

    public Long getTime() {
        return time;
    }

    public void setTime(Long time) {
        this.time = time;
    }

    public String getFiled() {
        return filed;
    }

    public void setFiled(String filed) {
        this.filed = filed;
    }

    public String getCheck() {
        return check;
    }

    public void setCheck(String check) {
        this.check = check;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }


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
