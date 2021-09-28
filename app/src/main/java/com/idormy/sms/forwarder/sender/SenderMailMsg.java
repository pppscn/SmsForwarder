package com.idormy.sms.forwarder.sender;

import android.os.Handler;
import android.util.Log;

import com.idormy.sms.forwarder.utils.LogUtil;
import com.idormy.sms.forwarder.utils.SettingUtil;
import com.smailnet.emailkit.Draft;
import com.smailnet.emailkit.EmailKit;

import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableEmitter;


@SuppressWarnings("ResultOfMethodCallIgnored")
public class SenderMailMsg extends SenderBaseMsg {
    private static final String TAG = "SenderMailMsg";

    public static void sendEmail(final long logId, final Handler handError, final String host, final String port, final boolean ssl, final String fromEmail, final String nickname, final String pwd, final String toAdd, final String title, final String content) {

        Log.d(TAG, "sendEmail: host:" + host + " port:" + port + " ssl:" + ssl + " fromEmail:" + fromEmail + " nickname:" + nickname + " pwd:" + pwd + " toAdd:" + toAdd);

        Observable
                .create((ObservableEmitter<Object> emitter) -> {
                    Toast(handError, TAG, "开始请求接口...");
                    try {
                        //初始化框架
                        //EmailKit.initialize(this);

                        //配置发件人邮件服务器参数
                        EmailKit.Config config = new EmailKit.Config()
                                .setSMTP(host, Integer.parseInt(port), ssl)  //设置SMTP服务器主机地址、端口和是否开启ssl
                                .setAccount(fromEmail)             //发件人邮箱
                                .setPassword(pwd);                 //密码或授权码

                        //设置一封草稿邮件
                        Draft draft = new Draft()
                                .setNickname(nickname)   //发件人昵称
                                .setTo(toAdd)                  //收件人邮箱
                                .setSubject(title)             //邮件主题
                                .setText(content);             //邮件正文

                        //使用SMTP服务发送邮件
                        EmailKit.useSMTPService(config)
                                .send(draft, new EmailKit.GetSendCallback() {
                                    @Override
                                    public void onSuccess() {
                                        LogUtil.updateLog(logId, 1, "发送成功");
                                        Toast(handError, TAG, "发送成功");
                                    }

                                    @Override
                                    public void onFailure(String errMsg) {
                                        LogUtil.updateLog(logId, 0, errMsg);
                                        Toast(handError, TAG, "发送失败，错误：" + errMsg);
                                    }
                                });

                        //销毁框架
                        EmailKit.destroy();

                    } catch (Exception e) {
                        LogUtil.updateLog(logId, 0, e.getMessage());
                        Log.e(TAG, e.getMessage(), e);
                        Toast(handError, TAG, "发送失败：" + e.getMessage());
                        emitter.onError(new RuntimeException("请求接口异常..."));
                    }
                }).retryWhen((Observable<Throwable> errorObservable) -> errorObservable
                .zipWith(Observable.just(
                        SettingUtil.getRetryDelayTime(1),
                        SettingUtil.getRetryDelayTime(2),
                        SettingUtil.getRetryDelayTime(3),
                        SettingUtil.getRetryDelayTime(4),
                        SettingUtil.getRetryDelayTime(5)
                ), (Throwable e, Integer time) -> time)
                .flatMap((Integer delay) -> {
                    Toast(handError, TAG, "请求接口异常，" + delay + "秒后重试");
                    return Observable.timer(delay, TimeUnit.SECONDS);
                }))
                .subscribe(System.out::println);
    }
}