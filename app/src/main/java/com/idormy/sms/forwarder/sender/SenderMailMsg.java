package com.idormy.sms.forwarder.sender;

import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

import com.idormy.sms.forwarder.MyApplication;
import com.idormy.sms.forwarder.R;
import com.idormy.sms.forwarder.model.vo.EmailSettingVo;
import com.idormy.sms.forwarder.utils.LogUtils;
import com.smailnet.emailkit.Draft;
import com.smailnet.emailkit.EmailKit;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;


public class SenderMailMsg extends SenderBaseMsg {
    private static final String TAG = "SenderMailMsg";

    public static void sendEmail(final long logId, final Handler handError, final EmailSettingVo emailSettingVo, final String title, final String content) {

        Log.d(TAG, "emailSettingVo: " + emailSettingVo.toString());

        try {
            //初始化框架
            //EmailKit.initialize(MyApplication.getContext());

            //配置发件人邮件服务器参数
            String fromEmail = emailSettingVo.getFromEmail();
            EmailKit.Config config = new EmailKit.Config();
            if (TextUtils.isEmpty(emailSettingVo.getMailType()) || emailSettingVo.getMailType().equals(MyApplication.getContext().getString(R.string.other_mail_type))) {
                config.setSMTP(emailSettingVo.getHost(), Integer.parseInt(emailSettingVo.getPort()), emailSettingVo.getSsl());  //设置SMTP服务器主机地址、端口和是否开启ssl
            } else {
                fromEmail += emailSettingVo.getMailType();
                config.setMailType(emailSettingVo.getMailType());//选择邮箱类型
            }

            config.setAccount(fromEmail) //发件人邮箱
                    .setPassword(emailSettingVo.getPwd());   //密码或授权码

            //多个收件人邮箱
            Set<String> toSet = new HashSet<>(Arrays.asList(emailSettingVo.getToEmail().replace("，", ",").split(",")));

            //设置一封草稿邮件
            Draft draft = new Draft()
                    .setNickname(emailSettingVo.getNickname())   //发件人昵称
                    .setTo(toSet)            //收件人邮箱
                    .setSubject(title)       //邮件主题
                    .setText(content);       //邮件正文

            //使用SMTP服务发送邮件
            EmailKit.useSMTPService(config)
                    .send(draft, new EmailKit.GetSendCallback() {
                        @Override
                        public void onSuccess() {
                            LogUtils.updateLog(logId, 2, "发送成功");
                            Toast(handError, TAG, "发送成功");
                        }

                        @Override
                        public void onFailure(String errMsg) {
                            LogUtils.updateLog(logId, 0, errMsg);
                            Toast(handError, TAG, "发送失败，错误：" + errMsg);
                        }
                    });

            //销毁框架
            EmailKit.destroy();

        } catch (Exception e) {
            LogUtils.updateLog(logId, 0, e.getMessage());
            Log.e(TAG, e.getMessage(), e);
            Toast(handError, TAG, "发送失败：" + e.getMessage());
        }

    }
}