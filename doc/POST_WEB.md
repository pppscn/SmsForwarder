## 配置转发到WEB后，测试或者转发时会向配置的token即url发送POST请求

> ⚠ 有一个已经实现好的站点[消息通知](https://msg.allmything.com)

### 请求体如下

> post form 参数：

|  key   | 类型  |  说明  |
|  ----  | ----  | ----  |
| from  | string  | 来源手机号 |
| content  | string  | 短信内容 |
| timestamp  | string |  当前时间戳，单位是毫秒，（建议验证与请求调用时间误差不能超过1小时，防止重放欺骗） |
| sign  | string  | 当设置secret时，生成的sign签名，用于发送端校验，规则见下方sign校验规则 |

* get请求的时，以上节点经过urlEncode后加在url上
* sign部分参考借鉴了[阿里钉钉群机器人的sign生成](https://developers.dingtalk.com/document/app/custom-robot-access)

### sign校验规则

把timestamp+"\n"+密钥当做签名字符串，使用HmacSHA256算法计算签名，然后进行Base64 encode，最后再把签名参数再进行urlEncode，得到最终的签名（需要使用UTF-8字符集） | 参数 | 说明 | | ---- | ---- | | timestamp | 当前时间戳，单位是毫秒，（建议验证与请求调用时间误差不能超过1小时，防止重放欺骗） | | secret | 密钥，web通知设置页面，secret |

示例：

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
