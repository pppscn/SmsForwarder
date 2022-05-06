![SmsForwarder](pic/SmsForwarder.png)

# SmsForwarder

[中文版](README.md)

[![GitHub release](https://img.shields.io/github/release/pppscn/SmsForwarder.svg)](https://github.com/pppscn/SmsForwarder/releases) [![GitHub stars](https://img.shields.io/github/stars/pppscn/SmsForwarder)](https://github.com/pppscn/SmsForwarder/stargazers) [![GitHub forks](https://img.shields.io/github/forks/pppscn/SmsForwarder)](https://github.com/pppscn/SmsForwarder/network/members) [![GitHub issues](https://img.shields.io/github/issues/pppscn/SmsForwarder)](https://github.com/pppscn/SmsForwarder/issues) [![GitHub license](https://img.shields.io/github/license/pppscn/SmsForwarder)](https://github.com/pppscn/SmsForwarder/blob/main/LICENSE)

SmsForwarder - listens to SMS, incoming calls, and App notifications on Android mobile devices, and forward according to user defined rules to another App/device, including DingTalk, WeCom and WeCom Group Bot, Feishi Bot, E-mail, Bark, Webhook, Telegram Bot, ServerChan, PushPlus, SMS, etc.

### Download

> ⚠ Repo address: https://github.com/pppscn/SmsForwarder/releases

> ⚠ Repo mirror in China: https://gitee.com/pp/SmsForwarder/releases

> ⚠ Internet storage: https://wws.lanzoui.com/b025yl86h, access password: `pppscn`

> ⚠ CoolAPK.com: https://www.coolapk.com/apk/com.idormy.sms.forwarder

### Manual

> ⚠ GitHub: https://github.com/pppscn/SmsForwarder/wiki

> ⚠ Gitee: https://gitee.com/pp/SmsForwarder/wikis/pages

--------

## NOTE

* Any code/APK of `SmsForwarder` related to the this repository is for test, study, and research only, commercial use is **prohibited**. Legality, accuracy, completeness and validity of any code/APK of this repo is guaranteed by **NOBODY**, and shall only be determined by User.

* `pppscn` and/or any other Contributor to this repo is **NOT** responsible for any consequences (including but not limited to privacy leakage) arising from any user's direct or indirect use or dissemination of any code or APK of `SmsForwarder`, regardless of whether such use is in accordance with the laws of the country or territory where such user locates or such use or dissemination occurs.

* Should any entity finds the code/APK of this repo infringing their rights, please provide notice and identity and proprietorship document, and we will delete relating code/APK after examining such document.

* Privacy: `SmsForwarder` collects absolutely **NO** any of your personal data!! Except 1) version information to umeng.com for stats as the App starts, and 2) version number when manually check for update, `SmsForwarder` is **NOT** sending any data without users' knowledge.

--------

## Features and standards

**Simplicity** - `SmsForwarder` does two things only: Listen to "SMS service/Incoming calls/App notifications", and forward according to rules specified by user.

Benefit by simplicity:

* **E**fficient: (It's inconvenient to read the security codes such as OTP on a mobile phone, when you are using another device; and no solution satisfices our needs)

  > + AirDroid: Too many functionalities, power consuming, requiring to many permissions, data relayed by a 3rd party, paid premium service...
  > + IFTTT: Too many functionalities, power consuming, requiring to many permissions, data relayed by a 3rd party, paid premium service...
  > + And other Apps (e.g. Tasker) with similar features.

* **E**nergy friendly: listens to broadcast only when running, and forwards message only when texts are received and logs recent forwarding contents and status.
* **E**ndurance: "Simplicity is the Ultimate Sophistication." The simpler the code is, the less it errs or crashes; that is what make the app runs longer.

### Workflow:

![Workflow](pic/working_principle_en.png "Workflow")

### Features:

- [x] Listen to SMS service, and forward according to user-defined rules (SMS contents to destination);
- [x] Forward to DingTalk Bot (to a group chat and @SOMBODY);
- [x] Forward to E-mail (SMTP with SSL encryption);
- [x] Forward to Bark;
- [x] Forward to webhook (a single web page [sending POST/GET requests to a designated URL](doc/POST_WEB.md));
- [x] Forward to WeCom Bots;
- [x] Forward to WeCom enterprise channels;
- [x] Forward to ServerChan·Turbo;
- [x] Forward to Telegram Bots (Proxy support ready);
- [x] Forward to another mobile phone via SMS [Note: Paid service, carriers may charge for SMS forwarding. SMS forwarding should apply with filtered rules when device has no Internet access.]
- [x] Check for new version and upgrade;
- [x] Cache purge;
- [x] Compatible with Android 5.xx, 6.xx, 7.xx, 8.xx, 9.xx, and 10.xx;
- [x] Support for dual SIM slots smartphones and label different slots/carrier/phone number (if available);
- [x] Support for multi-level rules;
- [x] Support for customized labeling of SIM slots and device, and customized forwarding templates;
- [x] Support for rules with regular expression
- [x] Support for rules for different SIM slots;
- [x] Forward missed call information (forwarded by SIM1 slot by default);
- [x] Retry 5 times after a failed request (customized interval time, stop retrying once successfully request);
- [x] Forward to FeiShu Bot;
- [x] Customized scheme (forwarder://main) wake up other Apps;
- [x] Monitor of battery status changes;
- [x] I18n support (Chinese and English currently);
- [x] Support for setting import and export functions (One-key cloning);
- [x] Listen to notifications of other Apps and forward;
- [x] Forward to PushPlus;
- [x] Support for customized template of forwarding rules (default template overrides if left blank);
- [x] Support for variables in regular expression of forwarding rules;
- [x] 转发到 Gotify发送通道（自主推送通知服务）
- [x] 被动接收本地 HttpServer
- [x] 主动轮询远程 SmsHub Api（v2.5.0+已删除）
- [x] 适配暗夜模式

--------

### Screenshots :

| 前台服务常驻状态栏 | 应用主界面 | 发送通道 | 转发规则 |
|  :--:  | :--:  |  :--:  | :--:  |
| ![前台服务常驻状态栏](pic/taskbar.jpg "前台服务常驻状态栏") | ![应用主界面](pic/main.jpg "应用主界面") | ![发送通道](pic/sender.png "发送通道") | ![转发规则](pic/rule.jpg "转发规则") |
| 转发规则--短信转发 | 转发规则--通话记录 | 转发规则--APP通知 | 转发日志详情 |
| ![短信转发](pic/rule_sms.jpg "短信转发") | ![通话转发](pic/rule_call.jpg "通话转发") | ![通知转发](pic/rule_app.jpg "通知转发") | ![转发日志详情](pic/maindetail.jpg "转发日志详情") |
| 设置界面--总开关 | 设置界面--电量监控&保活措施 | 设置界面--个性设置 | 一键克隆（配置导出导入） |
| ![设置界面--总开关](pic/setting_1.jpg "设置界面--总开关") | ![设置界面--电量监控&保活措施](pic/setting_2.jpg "设置界面--电量监控&保活措施") | ![设置界面--个性设置](pic/setting_3.jpg "设置界面--个性设置") | ![配置导出导入功能（一键克隆）](pic/clone.jpg "配置导出导入功能（一键克隆）") |

更多截图参见 https://github.com/pppscn/SmsForwarder/wiki

--------

## Feedback and suggestions:

+ Submit an issue or Pull Request.
+ Join group chat (only Chinese groups/channels available currently)

| DingTalk | QQ user group #1: 562854376 | QQ user group #2: 31330492 | WeCom |
|  ----  |  ----  | ----  | ----  |
| ![钉钉客户群](pic/dingtalk.png "钉钉客户群") | ![QQ交流群：562854376](pic/qqgroup_1.jpg "QQ交流群：562854376") | ![QQ交流群：31330492](pic/qqgroup_2.jpg "QQ交流群：31330492") | ![企业微信群](pic/qywechat.png "企业微信群") |

## Acknowledgements

> Thanks to the projects below, `SmsForwarder` won't exists without them!

+ https://github.com/xiaoyuanhost/TranspondSms (Foundation of `SmsForwarder`)
+ https://github.com/square/okhttp (http communications)
+ https://github.com/xuexiangjys/XUpdateAPI (online update)
+ https://github.com/mailhu/emailkit (email sending)
+ https://github.com/alibaba/fastjson (json parsing)
+ https://github.com/getActivity/XXPermissions (permission requiring)
+ https://github.com/Xcreen/RestSMS（被动接收本地API方案）
+ ~~https://github.com/juancrescente/SMSHub（主动轮询远程API方案，v2.5.0+删除）~~
+ https://github.com/mainfunx/frpc_android (内网穿透)
+ [<img src="https://resources.jetbrains.com/storage/products/company/brand/logos/jb_beam.svg?_ga=2.126618957.1361252949.1638261367-1417196221.1635638144&_gl=1*1pfl3dq*_ga*MTQxNzE5NjIyMS4xNjM1NjM4MTQ0*_ga_V0XZL7QHEB*MTYzODMzMjA4OC43LjAuMTYzODMzMjA5Ny4w" alt="GitHub license" style="zoom:50%;" />](https://jb.gg/OpenSourceSupport)  (License Certificate for JetBrains All Products Pack)

--------

## Star this repo if you find this application useful!

[![starcharts stargazers over time](https://starchart.cc/pppscn/SmsForwarder.svg)](https://github.com/pppscn/SmsForwarder)

--------

## LICENSE

BSD
