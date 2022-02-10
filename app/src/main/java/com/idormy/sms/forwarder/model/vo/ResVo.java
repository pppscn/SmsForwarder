package com.idormy.sms.forwarder.model.vo;

import lombok.Data;

@Data
public class ResVo<T> {
    public static final int ERROR_CODE = 500;
    public static final int Suess_CODE = 200;
    private int code;
    private String msg;
    private String auth;
    private SmsHubVo heartbeat;
    private T data;

    public ResVo<T> setSuessces(T data) {
        this.setCode(Suess_CODE);
        this.setData(data);
        this.setMsg("");
        return this;
    }

    public ResVo<T> setError(Throwable e) {
        this.setCode(ERROR_CODE);
        this.setMsg(e.getMessage());
        return this;
    }

    public ResVo<T> setError(String msg) {
        this.setCode(ERROR_CODE);
        this.setMsg(msg);
        return this;
    }

    public static <T> ResVo<T> suessces(T data) {
        ResVo<T> resVo = new ResVo<>();
        resVo.setCode(Suess_CODE);
        resVo.setData(data);
        resVo.setMsg("");
        return resVo;
    }

    public static <T> ResVo<T> error(Throwable e) {
        ResVo<T> resVo = new ResVo<>();
        resVo.setCode(ERROR_CODE);
        resVo.setMsg(e.getMessage());
        return resVo;
    }

    public static ResVo<String> error(String msg) {
        ResVo<String> resVo = new ResVo<>();
        resVo.setCode(ERROR_CODE);
        resVo.setMsg(msg);
        return resVo;
    }
}
