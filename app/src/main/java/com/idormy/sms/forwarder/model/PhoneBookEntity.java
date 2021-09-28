package com.idormy.sms.forwarder.model;

import lombok.Data;

@Data
public class PhoneBookEntity {
    private String name;
    private String phoneNumber;

    public PhoneBookEntity(String name, String phoneNumber) {
        this.name = name;
        this.phoneNumber = phoneNumber;
    }
}
