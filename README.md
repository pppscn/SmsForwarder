![SmsForwarder](pic/SmsForwarder.png)

# SmsForwarder-短信转发器

[English Version](README_en.md)

[![GitHub release](https://img.shields.io/github/release/pppscn/SmsForwarder.svg)](https://github.com/pppscn/SmsForwarder/releases) [![GitHub stars](https://img.shields.io/github/stars/pppscn/SmsForwarder)](https://github.com/pppscn/SmsForwarder/stargazers) [![GitHub forks](https://img.shields.io/github/forks/pppscn/SmsForwarder)](https://github.com/pppscn/SmsForwarder/network/members) [![GitHub issues](https://img.shields.io/github/issues/pppscn/SmsForwarder)](https://github.com/pppscn/SmsForwarder/issues) [![GitHub license](https://img.shields.io/github/license/pppscn/SmsForwarder)](https://github.com/pppscn/SmsForwarder/blob/main/LICENSE)

--------

短信转发器——不仅只转发短信，备用机必备神器！

监控Android手机短信、来电、APP通知，并根据指定规则转发到其他手机：钉钉群自定义机器人、钉钉企业内机器人、企业微信群机器人、企业微信应用消息、飞书群机器人、飞书企业应用、邮箱、bark、webhook、Tele****机器人、Server酱、PushPlus、手机短信等。

包括主动控制服务端与客户端，让你轻松远程发短信、查短信、查通话、查话簿、查电量等。（V3.0 新增）

> 注意：从`2022-06-06`开始，原`Java版`的代码归档到`v2.x`分支，不再更新！

> 1、从`v2.x`到`v3.x`不是简单的功能迭代，采用`kotlin`全新重构了（不是单纯的迁移代码，起初我也是这么认为的），由于我是第一次使用`kotlin`开发（Java版也是第一次），到处踩坑，每一行代码都是度娘手把手教会我的，所以`v3.x`版本可能一开始并不稳定。另外，眼睛葡萄膜炎还没好，晚上不敢肝，中间停摆了个把月，进度缓慢，历时2个月终于让`V3.x`顺产了！

> 2、如果目前`v2.x`用的好好的没必要升级（之前也是这么建议大家的，没必要每版必跟，除非你急需新功能）

> 3、`v3.x` 适配 Android 4.4 ~ 13.0

--------

## 特别声明:

* 本仓库发布的`SmsForwarder`项目中涉及的任何代码/APK，仅用于测试和学习研究，禁止用于商业用途，不能保证其合法性，准确性，完整性和有效性，请根据情况自行判断。

* 任何用户直接或间接使用或传播`SmsForwarder`的任何代码或APK，无论该等使用是否符合其所在国家或地区，或该等使用或传播发生的国家或地区的法律，`pppscn`和/或代码仓库的任何其他贡献者均不对该等行为产生的任何后果（包括但不限于隐私泄露）负责。

* 如果任何单位或个人认为该项目的代码/APK可能涉嫌侵犯其权利，则应及时通知并提供身份证明，所有权证明，我们将在收到认证文件后删除相关代码/APK。

* 隐私声明： **SmsForwarder 不会收集任何您的隐私数据！！！** APP启动时发送版本信息发送到友盟统计；手动检查新版本时发送版本号用于检查新版本；除此之外，没有任何数据！！！

--------

## 工作流程：

![工作流程](pic/working_principle.png "working_principle.png")

--------

## 界面预览：

![界面预览](pic/界面预览.jpeg "界面预览.jpg")

更多截图参见 https://github.com/pppscn/SmsForwarder/wiki

--------

## 下载地址

> ⚠ 首发地址：https://github.com/pppscn/SmsForwarder/releases

> ⚠ 国内镜像：https://gitee.com/pp/SmsForwarder/releases

> ⚠ 网盘下载：https://wws.lanzoui.com/b025yl86h 访问密码：`pppscn`

--------

## 使用文档【新用户必看！】

> ⚠ GitHub Wiki：https://github.com/pppscn/SmsForwarder/wiki

> ⚠ Gitee Wiki：https://gitee.com/pp/SmsForwarder/wikis/pages

![使用流程与问题排查流程](pic/使用流程与问题排查流程.png "使用流程与问题排查流程.png")

--------

## 反馈与建议：

+ 提交issues 或 pr
+ 加入交流群（群内都是机油互帮互助，禁止发任何与SmsForwarder使用无关的内容）

| QQ频道 | 钉钉客户群 | Tg 群组 |
|  :--:  |  :--:  | :--:  |
| ![QQ频道号: q7oofwp13s](pic/qq_channel.png "QQ频道号: q7oofwp13s") | ![钉钉客户群](pic/dingtalk.png "钉钉客户群") | ![Tg 群组](pic/tg.png "Tg 群组") |
| QQ频道号: q7oofwp13s | 群号：29760014208 | [+QBZgnL_fxYM0NjE9](https://t.me/+QBZgnL_fxYM0NjE9) |

## 感谢

> [感谢所有赞助本项目的热心网友 --> 打赏名单](https://gitee.com/pp/SmsForwarder/wikis/pages?sort_id=4912193&doc_id=1821427)

> 本项目得到以下项目的支持与帮助，在此表示衷心的感谢！

+ https://github.com/xiaoyuanhost/TranspondSms (项目原型)
+ https://github.com/xuexiangjys/XUI （UI框架）
+ https://github.com/xuexiangjys/XUpdate （在线升级）
+ https://github.com/getActivity/XXPermissions (权限请求框架)
+ https://github.com/mainfunx/frpc_android (内网穿透)
+ https://github.com/gyf-dev/Cactus (保活措施)
+ https://github.com/yanzhenjie/AndServer (HttpServer)
+ https://github.com/jenly1314/Location (Location)
+ https://gitee.com/xuankaicat/kmnkt (socket通信)
+ [<img src="https://resources.jetbrains.com/storage/products/company/brand/logos/jb_beam.svg?_ga=2.126618957.1361252949.1638261367-1417196221.1635638144&_gl=1*1pfl3dq*_ga*MTQxNzE5NjIyMS4xNjM1NjM4MTQ0*_ga_V0XZL7QHEB*MTYzODMzMjA4OC43LjAuMTYzODMzMjA5Ny4w" alt="GitHub license" style="width：96px" width="96" />](https://jb.gg/OpenSourceSupport)  (License Certificate for JetBrains All Products Pack)

--------

## 如果您觉得本工具对您有帮助，不妨在右上角点亮一颗小星星，以示鼓励！

[![starcharts stargazers over time](https://starchart.cc/pppscn/SmsForwarder.svg)](https://github.com/pppscn/SmsForwarder)

--------

## LICENSE

BSD
