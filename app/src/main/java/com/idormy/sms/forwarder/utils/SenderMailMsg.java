package com.idormy.sms.forwarder.utils;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import java.util.Date;
import java.util.Properties;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import static com.idormy.sms.forwarder.SenderActivity.NOTIFY;


public class SenderMailMsg {
    private static String TAG = "SenderMailMsg";
    //qq
//    private static final String HOST = "smtp.qq.com";
//    private static final String PORT = "587";
//    private static final String FROM_ADD = "teprinciple@foxmail.com"; //发送方邮箱
//    private static final String FROM_PSW = "lfrlpganzjrwbeci";//发送方邮箱授权码

//    //163
//    private static final String HOST = "smtp.163.com";
//    private static final String PORT = "465"; //nossl 25或者ssl465  994
//    private static final String FROM_ADD = "xxxxxx@163.com";
//    private static final String FROM_PSW = "xx";

    public static void sendEmail(final Handler handError, final String host, final String port, final boolean ssl, final String fromemail, final String pwd, final String toAdd, final String title, final String content) {

        Log.d(TAG, "sendEmail: host:" + host + " port:" + port + " ssl:" + ssl + " fromemail:" + fromemail + " pwd:" + pwd + " toAdd:" + toAdd);
        new Thread(new Runnable() {
            @Override
            public void run() {

                try {
                    final MailSenderInfo mailInfo = new MailSenderInfo();
                    mailInfo.setMailServerHost(host);
                    mailInfo.setMailServerPort(port);
                    mailInfo.setValidate(true);
                    mailInfo.setUserName(fromemail);  //你的邮箱地址
                    mailInfo.setPassword(pwd);//您的邮箱密码
                    mailInfo.setFromAddress(fromemail);//和上面username的邮箱地址一致
                    mailInfo.setToAddress(toAdd);
                    mailInfo.setSubject(title);
                    mailInfo.setContent(content);
                    mailInfo.setSsl(ssl);

                    //这个类主要来发送邮件
                    // 判断是否需要身份认证
                    MyAuthenticator authenticator = null;
                    Properties pro = mailInfo.getProperties();
                    if (mailInfo.isValidate()) {
                        // 如果需要身份认证，则创建一个密码验证器
                        authenticator = new MyAuthenticator(mailInfo.getUserName(), mailInfo.getPassword());
                    }
                    // 根据邮件会话属性和密码验证器构造一个发送邮件的session
                    Session sendMailSession = Session.getDefaultInstance(pro, authenticator);
                    try {
                        // 根据session创建一个邮件消息
                        final Message mailMessage = new MimeMessage(sendMailSession);
                        // 创建邮件发送者地址
                        Address from = new InternetAddress(mailInfo.getFromAddress());
                        // 设置邮件消息的发送者
                        mailMessage.setFrom(from);
                        // 创建邮件的接收者地址，并设置到邮件消息中
                        Address to = new InternetAddress(mailInfo.getToAddress());
                        mailMessage.setRecipient(Message.RecipientType.TO, to);
                        // 设置邮件消息的主题
                        mailMessage.setSubject(mailInfo.getSubject());
                        // 设置邮件消息发送的时间
                        mailMessage.setSentDate(new Date());
                        // 设置邮件消息的主要内容
                        String mailContent = mailInfo.getContent();
                        mailMessage.setText(mailContent);
                        // 发送邮件
                        Transport.send(mailMessage);

                    } catch (MessagingException ex) {
                        ex.printStackTrace();
                        Log.e(TAG, "error" + ex.getMessage());
                        if (handError != null) {
                            android.os.Message msg = new android.os.Message();
                            msg.what = NOTIFY;
                            Bundle bundle = new Bundle();
                            bundle.putString("DATA", ex.getMessage());
                            msg.setData(bundle);
                            handError.sendMessage(msg);
                        }

                    }
                    if (handError != null) {
                        android.os.Message msg = new android.os.Message();
                        msg.what = NOTIFY;
                        Bundle bundle = new Bundle();
                        bundle.putString("DATA", "发送成功");
                        msg.setData(bundle);
                        handError.sendMessage(msg);
                    }

                    Log.e(TAG, "sendEmail success");//sms.sendHtmlMail(mailInfo);//发送html格式

                } catch (Exception e) {
                    Log.e(TAG, e.getMessage(), e);
                    if (handError != null) {
                        android.os.Message msg = new android.os.Message();
                        msg.what = NOTIFY;
                        Bundle bundle = new Bundle();
                        bundle.putString("DATA", e.getMessage());
                        msg.setData(bundle);
                        handError.sendMessage(msg);
                    }

                }
            }
        }).start();
    }
}

/**
 * public void sendFileMail(View view) {
 * <p>
 * File file = new File(Environment.getExternalStorageDirectory()+File.separator+"test.txt");
 * OutputStream os = null;
 * try {
 * os = new FileOutputStream(file);
 * String str = "hello world";
 * byte[] data = str.getBytes();
 * os.write(data);
 * } catch (FileNotFoundException e) {
 * e.printStackTrace();
 * } catch (IOException e) {
 * e.printStackTrace();
 * }finally{
 * try {
 * if (os != null)os.close();
 * } catch (IOException e) {
 * }
 * }
 * SenderMailMsg.send(file,editText.getText().toString());
 * }
 */