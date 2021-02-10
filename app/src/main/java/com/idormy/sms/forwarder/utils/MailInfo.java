package com.idormy.sms.forwarder.utils;

import java.util.Properties;

public class MailInfo {

    private String mailServerHost;// 发送邮件的服务器的IP
    private String mailServerPort;// 发送邮件的服务器的端口
    private String fromAddress;// 邮件发送者的地址
    private String toAddress;   // 邮件接收者的地址
    private String userName;// 登陆邮件发送服务器的用户名
    private String password;// 登陆邮件发送服务器的密码
    private boolean validate = true;// 是否需要身份验证
    private boolean ssl = true;// ssl
    private String subject;// 邮件主题
    private String content;// 邮件的文本内容
    private String[] attachFileNames;// 邮件附件的文件名


    public String toString() {
        return "mailServerHost:" + this.mailServerHost
                + "mailServerPort:" + this.mailServerPort
                + "fromAddress:" + this.fromAddress
                + "toAddress:" + this.toAddress
                + "userName:" + this.userName
                + "password:" + this.password
                + "subject:" + this.subject
                + "content:" + this.content;
    }

    /**
     * 获得邮件会话属性
     */
    public Properties getProperties() {
        Properties p = new Properties();
        p.put("mail.smtp.host", this.mailServerHost);
        p.put("mail.smtp.port", this.mailServerPort);
        p.put("mail.smtp.auth", validate ? "true" : "false");

        // 设置SSL加密(未采用SSL时，端口一般为25，可以不用设置；采用SSL时，端口为465，需要显示设置)
        if (ssl) {
            p.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            p.put("mail.smtp.socketFactory.fallback", "false");
            p.put("mail.smtp.socketFactory.port", this.mailServerPort);
        }


//        props.setProperty("mail.smtp.port", "465");
//        props.setProperty("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
//        props.setProperty("mail.smtp.socketFactory.fallback", "false");
//        props.setProperty("mail.smtp.socketFactory.port", "465");
        return p;
    }

    public String getMailServerHost() {
        return mailServerHost;
    }

    public void setMailServerHost(String mailServerHost) {
        this.mailServerHost = mailServerHost;
    }

    public String getMailServerPort() {
        return mailServerPort;
    }

    public void setMailServerPort(String mailServerPort) {
        this.mailServerPort = mailServerPort;
    }

    public boolean isValidate() {
        return validate;
    }

    public void setValidate(boolean validate) {
        this.validate = validate;
    }

    public void ssl(boolean ssl) {
        this.ssl = ssl;
    }

    public String[] getAttachFileNames() {
        return attachFileNames;
    }

    public void setAttachFileNames(String[] fileNames) {
        this.attachFileNames = fileNames;
    }

    public String getFromAddress() {
        return fromAddress;
    }

    public void setFromAddress(String fromAddress) {
        this.fromAddress = fromAddress;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getToAddress() {
        return toAddress;
    }

    public void setToAddress(String toAddress) {
        this.toAddress = toAddress;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String textContent) {
        this.content = textContent;
    }
}

