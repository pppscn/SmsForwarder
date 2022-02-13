package com.idormy.sms.forwarder.model.vo;

import androidx.annotation.NonNull;

import com.alibaba.fastjson.JSON;
import com.idormy.sms.forwarder.sender.SmsHubApiTask;
import com.idormy.sms.forwarder.utils.PhoneUtils;
import com.idormy.sms.forwarder.utils.SettingUtil;
import com.idormy.sms.forwarder.utils.SimUtil;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import lombok.Data;

@SuppressWarnings("unchecked")
@Data
public class SmsHubVo implements Serializable {
    private static final long serialVersionUID = 1L;

    public SmsHubVo() {
    }

    public SmsHubVo(Type type, Integer simId, String content, String target) {
        this.msgId = UUID.randomUUID().toString();
        if (simId != null) {
            String simInfo = simId == 2 ? SettingUtil.getAddExtraSim2() : SettingUtil.getAddExtraSim1(); //自定义备注优先
            simInfo = "SIM" + simId + ":" + simInfo;
            this.channel = simInfo;
        }
        this.content = content;
        this.target = target;
        this.action = Action.receive.code();
        this.type = type.code;
        this.ts = System.currentTimeMillis() + "";
    }

    //唯一id
    private String msgId;
    //心跳数据时发送的设备名
    private String deviceInfo;
    //卡槽信息
    private String channel;
    //消息内容
    private String content;
    //错误消息
    private String errMsg;
    //手机号(;分隔)或包名
    private String target;
    //状态或操作 0:发送短信, 1:接收到的消息, 2:操作处理成功, 3:操作处理失败, -1:心跳包 (包含deviceInfo字段,children里带有两次心跳间收到的消息)
    private String action;
    //消息类型 app:通知 phone:来电, sms:短信, battery:电池信息
    private String type;
    //时间戳
    private String ts;
    //两次交互之间接收到的消息
    private List<SmsHubVo> children;

    public static SmsHubVo heartbeatInstance(List<SmsHubVo> data) {
        SmsHubVo smsHubVo = new SmsHubVo();
        HashMap<String, String> deviInfoMap = getDevInfoMap(false);
        smsHubVo.setDeviceInfo(JSON.toJSONString(deviInfoMap));
        smsHubVo.setChannel("SIM1:" + SimUtil.getSimInfo(1) + SettingUtil.getAddExtraSim1() + ";SIM2:" + SimUtil.getSimInfo(2) + SettingUtil.getAddExtraSim2());
        smsHubVo.setTs(Long.toString(System.currentTimeMillis()));
        smsHubVo.setAction(SmsHubVo.Action.heartbeat.code());
        if (data != null && data.size() > 0) {
            smsHubVo.setChildren(data);
        }
        return smsHubVo;
    }

    private static Map<String, Object> cache = new ConcurrentHashMap<>();

    @NonNull
    public static HashMap<String, String> getDevInfoMap(boolean reflush) {
        String key = "deviceInfo";
        if (reflush || !cache.containsKey(key)) {
            HashMap<String, String> deviInfoMap = new HashMap<>();
            deviInfoMap.put("mark", SettingUtil.getAddExtraDeviceMark());
            deviInfoMap.put("simOperatorName", PhoneUtils.getSimOperatorName());
            deviInfoMap.put("phoneNumber", PhoneUtils.getPhoneNumber());
            deviInfoMap.put("imei", PhoneUtils.getIMEI());
            deviInfoMap.put("SDKVersion", PhoneUtils.getSDKVersion() + "");
            deviInfoMap.put("Version", SettingUtil.getVersionName());
            deviInfoMap.put("heartbeat", SmsHubApiTask.DELAY_SECONDS + "");
            cache.put(key, deviInfoMap);
            return deviInfoMap;
        }
        return (HashMap<String, String>) Objects.requireNonNull(cache.get(key));
    }

    public enum Action {
        send("0"), receive("1"), suessces("2"), failure("3"), heartbeat("-1");

        Action(String code) {
            this.code = code;
        }

        private final String code;

        public String code() {
            return code;
        }
    }

    public enum Type {
        app("app"), phone("phone"), sms("sms"), battery("battery");

        Type(String code) {
            this.code = code;
        }

        private final String code;

        public String code() {
            return code;
        }
    }
}
