package com.idormy.sms.forwarder.utils;

import android.util.Log;

import java.io.File;


public class SendMailUtil {
    private static String TAG = "SendMailUtil";
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

    public static void send(final File file, String toAdd, String title, String content) {
        Log.d(TAG, "send file to " + toAdd);
        final MailInfo mailInfo = creatMail(toAdd, title, content);
        final MailSender sms = new MailSender();
        new Thread(new Runnable() {
            @Override
            public void run() {
                sms.sendFileMail(mailInfo, file);
            }
        }).start();
    }

    public static void send(String toAdd, String title, String content) {
        Log.d(TAG, "send to " + toAdd);
        final MailInfo mailInfo = creatMail(toAdd, title, content);
        final MailSender sms = new MailSender();
        new Thread(new Runnable() {
            @Override
            public void run() {
                sms.sendTextMail(mailInfo);
            }
        }).start();
    }

    private static MailInfo creatMail(String toAdd, String title, String content) {
        Log.d(TAG, "creatMail to " + toAdd);
        final MailInfo mailInfo = new MailInfo();
        mailInfo.setMailServerHost(SettingUtil.get_send_util_email(Define.SP_MSG_SEND_UTIL_EMAIL_HOST_KEY));
        mailInfo.setMailServerPort(SettingUtil.get_send_util_email(Define.SP_MSG_SEND_UTIL_EMAIL_PORT_KEY));
        mailInfo.setValidate(true);
        mailInfo.ssl(true);
        mailInfo.setUserName(SettingUtil.get_send_util_email(Define.SP_MSG_SEND_UTIL_EMAIL_FROMADD_KEY)); // 你的邮箱地址
        mailInfo.setPassword(SettingUtil.get_send_util_email(Define.SP_MSG_SEND_UTIL_EMAIL_PSW_KEY));// 您的邮箱密码
        mailInfo.setFromAddress(SettingUtil.get_send_util_email(Define.SP_MSG_SEND_UTIL_EMAIL_FROMADD_KEY)); // 发送的邮箱
        mailInfo.setToAddress(SettingUtil.get_send_util_email(Define.SP_MSG_SEND_UTIL_EMAIL_TOADD_KEY)); // 发到哪个邮件去
        mailInfo.setSubject(title); // 邮件主题
        mailInfo.setContent(content); // 邮件文本
        return mailInfo;
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
 * SendMailUtil.send(file,editText.getText().toString());
 * }
 */