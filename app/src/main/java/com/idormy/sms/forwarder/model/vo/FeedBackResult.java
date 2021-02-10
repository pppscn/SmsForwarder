package com.idormy.sms.forwarder.model.vo;

import java.io.Serializable;

public class FeedBackResult implements Serializable {
    Integer code;
    String message;
    Object result;

    public FeedBackResult() {

    }

    public String getMessage() {
        return message;
    }

    public boolean isSuccess() {
        return 1 == code;
    }

    @Override
    public String toString() {
        return "FeedBackResult{" +
                "code=" + code +
                ", message='" + message + '\'' +
                ", result=" + result +
                '}';
    }
}
