package com.idormy.sms.forwarder.model.vo;

import lombok.Data;

@Data
public class ResVo<T> {
    private int code;
    private String msg;
    private String auth;
    private SmsHubVo heartbeat;
    private T data;

    public static <T> ResVo<T> suessces(T data) {
        ResVo<T> resVo = new ResVo<>();
        resVo.setCode(200);
        resVo.setData(data);
        resVo.setMsg("");
        return resVo;
    }

    public static <T> ResVo<T> error(Throwable e) {
        ResVo<T> resVo = new ResVo<>();
        resVo.setCode(500);
        resVo.setMsg(e.getMessage());
        return resVo;
    }

    public static  ResVo<String> error(String msg) {
        ResVo<String> resVo = new ResVo<>();
        resVo.setCode(500);
        resVo.setMsg(msg);
        return resVo;
    }
}
