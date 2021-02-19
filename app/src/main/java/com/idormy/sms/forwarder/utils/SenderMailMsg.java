package com.idormy.sms.forwarder.utils;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import com.smailnet.emailkit.Draft;
import com.smailnet.emailkit.EmailKit;

import static com.idormy.sms.forwarder.SenderActivity.NOTIFY;


public class SenderMailMsg {
    private static String TAG = "SenderMailMsg";

    public static void sendEmail(final Handler handError, final String host, final String port, final boolean ssl, final String fromemail, final String pwd, final String toAdd, final String title, final String content) {

        Log.d(TAG, "sendEmail: host:" + host + " port:" + port + " ssl:" + ssl + " fromemail:" + fromemail + " pwd:" + pwd + " toAdd:" + toAdd);

        try {
            //初始化框架
            //EmailKit.initialize(this);

            //配置发件人邮件服务器参数
            EmailKit.Config config = new EmailKit.Config()
                    .setSMTP(host, Integer.parseInt(port), ssl)  //设置SMTP服务器主机地址、端口和是否开启ssl
                    .setAccount(fromemail)             //发件人邮箱
                    .setPassword(pwd);                 //密码或授权码

            //设置一封草稿邮件
            Draft draft = new Draft()
                    .setNickname("SmsForwarder")   //发件人昵称
                    .setTo(toAdd)                  //收件人邮箱
                    .setSubject(title)             //邮件主题
                    .setText(content);             //邮件正文

            //使用SMTP服务发送邮件
            EmailKit.useSMTPService(config)
                    .send(draft, new EmailKit.GetSendCallback() {
                        @Override
                        public void onSuccess() {
                            Log.i(TAG, "发送成功！");
                            if (handError != null) {
                                android.os.Message msg = new android.os.Message();
                                msg.what = NOTIFY;
                                Bundle bundle = new Bundle();
                                bundle.putString("DATA", "发送成功");
                                msg.setData(bundle);
                                handError.sendMessage(msg);
                            }
                        }

                        @Override
                        public void onFailure(String errMsg) {
                            Log.i(TAG, "发送失败，错误：" + errMsg);
                            if (handError != null) {
                                android.os.Message msg = new android.os.Message();
                                msg.what = NOTIFY;
                                Bundle bundle = new Bundle();
                                bundle.putString("DATA", "发送失败，错误：" + errMsg);
                                msg.setData(bundle);
                                handError.sendMessage(msg);
                            }
                        }
                    });

            //销毁框架
            EmailKit.destroy();

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
}