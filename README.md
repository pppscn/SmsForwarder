## 使用方法

### 设置 Token

```bash
export STAR_HISTORY_TOKEN=ghp_xxxxxxxxxxxxxxxxx
```

Windows PowerShell：

```powershell
$env:STAR_HISTORY_TOKEN="ghp_xxxxxxxxxxxxxxxxx"
```

---

### 设置仓库

```bash
export GITHUB_REPOSITORY=pppscn/SmsForwarder
```

Windows：

```powershell
$env:GITHUB_REPOSITORY="pppscn/SmsForwarder"
```

---

### 运行

```bash
node scripts/bootstrap.js
```

生成：

```text
history.json
```

内容类似：

```json
{
  "repository": "pppscn/SmsForwarder",
  "updated": "2026-07-26",
  "points": [
    {
      "date": "2021-02-10",
      "stars": 1
    },
    {
      "date": "2021-02-11",
      "stars": 3
    }
  ]
}
```

---

## 如果运行时报错

例如：

```text
GitHub 当前未返回 starred_at，无法 Bootstrap 历史数据。
```

或者返回 403/404，这不是脚本问题，而是 **GitHub 已经限制了 `starred_at` 历史数据接口**。
