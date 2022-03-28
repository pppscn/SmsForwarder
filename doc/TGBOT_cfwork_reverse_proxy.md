### Cloudflare Work 反向代理代理 **TG_BOT_API**

​	准备：cloudflare账号

## 1.建立一个cf work 复制粘贴以下代码

```js
const whitelist = ["/bot你的botID:"];
//示例const whitelist = ["/bot123456:"];
const tg_host = "api.telegram.org";

addEventListener('fetch', event => {
    event.respondWith(handleRequest(event.request))
})

function validate(path) {
    for (var i = 0; i < whitelist.length; i++) {
        if (path.startsWith(whitelist[i]))
            return true;
    }
    return false;
}

async function handleRequest(request) {
    var u = new URL(request.url);
    u.host = tg_host;
    if (!validate(u.pathname))
        return new Response('Unauthorized', {
            status: 403
        });
    var req = new Request(u, {
        method: request.method,
        headers: request.headers,
        body: request.body
    });
    const result = await fetch(req);
    return result;
}

```

然后获取workers地址如：https://xx.xxx.workers.dev

测试发送消息：{}不需要填写

https://xxx.xxx.workers.dev/bot{机器人token}/sendMessage?chat_id={消息发送人}&text=test

示例：https://xxx.xxx.workers.dev/bot1234567:abcd_abcd--abd/sendMessage?chat_id=123456&text=test

## 2.配置APP转发

<img src="https://ae03.alicdn.com/kf/Hb31257341c364a83a5844dd160667140d.png" alt="image.png" title="image.png" />

添加TGBOT

选择GET请求 

图1位置输入第一部分发送测试地址‘？’之前的url如

https://xxx.xxx.workers.dev/bot1234567:abcd_abcd--abd/sendMessage

图2位置输入通知人ID即可 点击测试发送。



