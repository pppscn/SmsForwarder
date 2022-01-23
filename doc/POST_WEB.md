# 1、请求方式： GET

## 1.1 `webParams` 为空

将在 `WebServer` 的基础上，追加 `3、post form 参数列表` 所列的节点经过 `urlEncode` 的值

例如：

`WebServer`： `https://ppps.cn/demo`

`最终请求地址`：`https://ppps.cn/demo?from=15888888888&content=123456`

## 1.2 `webParams` 非空

将在 `WebServer` 的基础上，追加经过处理后的 `webParams`

处理方式： 替换 `3、post form 参数列表` 所列的节点经过 `urlEncode` 的值（例如：将 `短信内容(content)` 替换报文中的 `[msg]` 标签）

注意事项： `webParams` 中如果有特殊字符自行 `urlEncode`，程序只会替换列表中的key对应的标签

例如：

`WebServer`： `https://api2.pushdeer.com/message/push?pushkey=1234567890`

`webParams`： `text=[msg]`

`最终请求地址`：`https://api2.pushdeer.com/message/push?pushkey=1234567890&text=123456`

***

# 2、请求方式： POST

## 2.1 `webParams` 非空，包含 `[msg]` 标签、并且以 `{` 开头的 `json` 报文

将 `短信内容(content)` 替换报文中的 `[msg]` 标签，然后以 `application/json;charset=utf-8` 形式 `POST` 提交

## 2.2 `webParams` 非空，包含 `[msg]` 标签、不以 `{` 开头

将 `短信内容(content)` 经过 `URLEncoder.encode(content, "UTF-8")` 处理后，替换报文中的 `[msg]` 标签，然后以 `application/x-www-form-urlencoded` 形式 `POST` 提交

## 2.3 `webParams` 为空

将以 `application/x-www-form-urlencoded` 形式 `POST` 提交 `2、参数列表` 所列节点的表单（PS. 此形式适用于 `附录1`）

***

# 3、post form 参数列表

|  key   | 类型  |  说明  |
|  ----  | ----  | ----  |
| from  | string  | 来源手机号 / App包名 |
| content  | string  | 短信内容 / 通知内容 |
| timestamp  | string |  当前时间戳，单位是毫秒，（建议验证与请求调用时间误差不能超过1小时，防止重放欺骗） |
| sign  | string  | 当设置`secret`时，生成的`sign`签名，用于发送端校验，规则见下方`sign`校验规则 |

* 节点对应的标签就是 `key` 的值加上中括号（例如： `[msg]` ）
* `sign` 部分参考借鉴了 [阿里钉钉群机器人的sign生成](https://developers.dingtalk.com/document/app/custom-robot-access)
* `sign` 校验规则：

把 `timestamp+"\n"+密钥` 当做签名字符串，使用 `HmacSHA256` 算法计算签名，然后进行 Base64 encode，最后再把签名参数再进行urlEncode，得到最终的签名（需要使用UTF-8字符集）

| 参数 | 说明 |
| ---- | ---- |
| timestamp | 当前时间戳，单位是毫秒，（建议验证与请求调用时间误差不能超过1小时，防止重放欺骗） |
| secret | 密钥，web通知设置页面，secret |

***

# 附录：

## 1、一个现成的 `webhook` 服务端站点：可以在线查看 [消息通知](https://msg.allmything.com)

来自：[TSMS](https://github.com/xiaoyuanhost/TranspondSms)

登录之后，可以获取到一个带token的链接（类似：https://api.sl.willanddo.com/api/msg/pushMsg?token=123456）

此链接填写到 `WebServer` ， `webParams` 留空 ，即可通过该站点直接查看提交的消息列表

## 2、`sign` 签名计算示例：

```Java
//java

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class Test {
    public static void main(String[] args) throws Exception {
        Long timestamp = System.currentTimeMillis();
        String secret = "this is secret";

        String stringToSign = timestamp + "\n" + secret;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] signData = mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8));
        String sign = URLEncoder.encode(new String(Base64.encodeBase64(signData)), "UTF-8");
        System.out.println(sign);
    }

}

```

```python
#python 3.8
import time
import hmac
import hashlib
import base64
import urllib.parse

timestamp = str(round(time.time() * 1000))
secret = 'this is secret'
secret_enc = secret.encode('utf-8')
string_to_sign = '{}\n{}'.format(timestamp, secret)
string_to_sign_enc = string_to_sign.encode('utf-8')
hmac_code = hmac.new(secret_enc, string_to_sign_enc, digestmod=hashlib.sha256).digest()
sign = urllib.parse.quote_plus(base64.b64encode(hmac_code))
print(timestamp)
print(sign)

```

```python
#python 2.7
import time
import hmac
import hashlib
import base64
import urllib

timestamp = long(round(time.time() * 1000))
secret = 'this is secret'
secret_enc = bytes(secret).encode('utf-8')
string_to_sign = '{}\n{}'.format(timestamp, secret)
string_to_sign_enc = bytes(string_to_sign).encode('utf-8')
hmac_code = hmac.new(secret_enc, string_to_sign_enc, digestmod=hashlib.sha256).digest()
sign = urllib.quote_plus(base64.b64encode(hmac_code))
print(timestamp)
print(sign)

```
