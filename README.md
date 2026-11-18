# PluEssentials 2.0

面向 Minecraft Java 1.21.2（Bukkit / Spigot / Paper）的一体化服务端插件：玩家功能、管理员工具、经济系统、玩家市场、领地圈地、登录系统、服务器公告/规则（书本界面）、自定义 TAB，以及**内嵌 Web 管理后台**（无需额外 Web 服务器，全部打包进单个 jar）。

| 项目 | 说明 |
|------|------|
| 版本 | 2.0.0 |
| Java | 21 |
| MC API | 1.21（`api-version`），目标 1.21.2 |
| 软依赖 | Vault（经济）、LuckPerms（权限组），缺失时自动降级 |
| 产物 | `PluEssentials-2.0.0.jar`（约 4.9 MB，内含 MySQL 驱动与 Vue 前端） |
| 作者 / 开发者 / 仓库维护者 | **stonestsy** |

---

## 目录

1. [功能总览](#一功能总览)
2. [安装与依赖](#二安装与依赖)
3. [配置文件](#三配置文件)
4. [命令手册](#四命令手册)
5. [权限节点](#五权限节点)
6. [Web 管理后台](#六web-管理后台)
7. [REST API 参考](#七rest-api-参考)
8. [数据存储](#八数据存储)
9. [开发与部署](#九开发与部署)
10. [常见问题](#十常见问题)

---

## 一、功能总览

### 玩家功能
| 模块 | 命令 | 说明 |
|------|------|------|
| 家 | `/home` `/sethome` `/delhome` `/homes` | 多家支持，数量上限按权限分组 |
| 传送请求 | `/tpa` `/tpaccept` `/tpdeny` | 带超时（默认 60 秒） |
| 死亡回传 | `/back` | 回到上次死亡位置 |
| 礼包 | `/kit` | 支持冷却 |
| 出生点 | `/spawn` | 管理员用 `/setspawn` 设置 |
| 地标 | `/warp` | 管理员用 `/setwarp` `/delwarp` 维护 |
| **经济** | `/money` `/pay` `/baltop` | 余额查询、转账、财富排行 |
| **玩家市场** | `/market` | GUI 商店，玩家上架/购买/下架 |
| **领地** | `/claim` | 圈地 + 方块/容器保护 |
| **公告** | `/news` | 以成书界面展示 |
| **规则** | `/rules` | 以成书界面展示 |
| **登录** | `/register` `/login` | 注册与登录，未登录时禁止移动/交互/聊天 |
| **兑换码** | `/redeem` | 兑换码换货币，支持限量、限时、一码一人一次 |

### 管理员功能
| 模块 | 命令 |
|------|------|
| 玩家状态 | `/fly` `/gamemode` `/heal` `/feed` `/god` `/vanish` `/speed` `/clear` |
| 背包查看 | `/invsee`（可编辑，带会话锁）、`/enderchest` |
| 封禁（UUID） | `/ban` `/tempban` `/unban` `/banlist` |
| 封禁（IP） | `/ipban` `/ipunban` `/ipbanlist`，登录时拦截 |
| 白名单 | `/whitelist on\|off\|add\|remove\|list`（别名 `/wl`） |
| 审计统计 | `/chatlog`、`/seen` |
| 强制操作 | `/sudo`、`/healall`、`/feedall` |
| 消息与维护 | `/broadcast`、`/plureload`、`/setwarp`、`/delwarp` |

### Web 管理后台
- 仪表盘（TPS、内存、在线人数、Vault/LuckPerms 状态）
- 玩家管理（踢出/治疗/喂食/击杀/无敌/飞行/清包/改模式/私聊/传送/查看背包/封禁）
- 控制台（日志查看 + 执行命令）
- 插件与世界管理
- 经济系统、**玩家市场**、**领地管理**、**兑换码**、**公告与规则编辑**、**登录账号**、封禁、IP 封禁、白名单、聊天记录
- Warp 管理、定时任务（Cron）、服务器控制（关服/重载/广播）

---

## 二、安装与依赖

1. 服务端使用 **Java 21**，MC 1.21.2。
2. 将 `PluEssentials-2.0.0.jar` 放入 `plugins/` 目录，启动服务端。
3. 首次启动自动生成 `plugins/PluEssentials/config.yml`、`announcements.yml` 及各类数据文件。
4. 可选依赖：
   - **Vault + 一个经济插件**（如 EssentialsX、CMI）→ 启用经济、市场、余额显示
   - **LuckPerms** → 启用权限组管理

> Vault 本体只是对接层，**必须再安装一个提供经济的插件**，否则仪表盘会显示"Vault 已安装（缺少经济插件）"。

---

## 三、配置文件

`plugins/PluEssentials/config.yml`：

```yaml
prefix: "&7[&bPluEssentials&7] "

# Web 管理后台
web:
  enabled: true
  host: "0.0.0.0"          # 监听地址
  port: 8080               # 端口
  password: "admin123"     # 登录密码，生产环境务必修改
  token-expiry: 60         # 会话有效期（分钟）
  console-buffer: 500      # 控制台日志缓存行数
  external-web-dir: ""     # 外部前端目录（留空用 jar 内嵌页面）

# 数据存储（MySQL 关闭时使用 YAML 文件）
database:
  enabled: false
  host: "localhost"
  port: 3306
  database: "pluessentials"
  username: "root"
  password: ""
  table-prefix: "plu_"     # 启动时自动建表

spawn:
  use-world-spawn: true    # 未设置出生点时使用世界出生点

whitelist:
  enabled: false           # /whitelist on|off 会同步修改此项

home:
  default-limit: 5
  limits:                  # 权限 plu.home.limit.<名称> 对应上限
    vip: 20
    admin: 50

tpa:
  timeout: 60              # 传送请求超时（秒）

back:
  enabled: true
  death-message: "&cYou died! Use &e/back &cto return to your death point."

kits:
  starter:
    cooldown: 0
    items:
      - {material: STONE_SWORD, amount: 1}
      - {material: STONE_PICKAXE, amount: 1}
      - {material: COOKED_BEEF, amount: 16}
  vip:
    cooldown: 3600
    items:
      - {material: DIAMOND_SWORD, amount: 1}
      - {material: GOLDEN_APPLE, amount: 8}

# 玩家市场
market:
  tax-percent: 0           # 成交税率（百分比，0 = 免税）
  max-items-per-player: 10 # 每人最多上架数量

# 领地
claims:
  max-per-player: 2        # 每人最多领地数量
  max-radius: 16           # 单块领地最大半径（边长 = 2*半径+1）

# 自定义 TAB 列表
tab:
  enabled: true
  header: "&b&lPluEssentials &7| &f在线 &a{online}&7/&a{max}"
  footer: "&7世界: &a{world} &7| 余额: &a{balance} &7| 时间: &a{time}"

# 登录系统
login:
  enabled: true
```

### TAB 占位符

| 占位符 | 含义 |
|--------|------|
| `{player}` | 当前玩家名 |
| `{online}` | 在线人数 |
| `{max}` | 最大人数 |
| `{world}` | 玩家所在世界 |
| `{ping}` | 延迟（ms） |
| `{balance}` | 余额（经济不可用时显示 `N/A`） |
| `{time}` | 现实时间 `HH:mm` |

TAB 每 5 秒自动刷新。

### 公告与规则配置

`plugins/PluEssentials/announcements.yml`（首次启动自动生成默认内容，也可在 Web 后台编辑）：

```yaml
news:
  title: "&6服务器公告"
  author: "服务器管理组"
  pages:
    - "&6欢迎来到服务器！"
    - "&7请遵守服务器规则，祝游戏愉快。"
rules:
  title: "&c服务器规则"
  author: "服务器管理组"
  pages:
    - "&c&l服务器规则\n&r1. 禁止作弊与使用外挂。"
    - "&r2. 禁止恶意 PVP 与刷屏。"
```

**每个列表元素 = 书本的一页**，支持 `&` 颜色代码与 `\n` 换行。

### 生产环境安全建议

- 修改 `web.password` 默认密码
- 用防火墙限制 8080 端口来源，或经 Nginx 反代并启用 HTTPS
- MySQL 避免空密码
- `login.enabled: true` 时，玩家需先注册/登录才能操作

---

## 四、命令手册

> 约定：`<参数>` 必填，`[参数]` 可选；消息支持 `&` 颜色代码。

### 4.1 玩家命令

默认所有玩家可用（`default: true`）。

#### 家（Home）

| 命令 | 权限 | 说明 |
|------|------|------|
| `/homes` | `plu.homes` | 列出自己的所有家 |
| `/home [name]` | `plu.home` | 传送到家，省略 name 默认 `home` |
| `/sethome [name]` | `plu.sethome` | 在当前位置设置家 |
| `/delhome <name>` | `plu.delhome` | 删除家 |

家数量上限：`plu.home.limit.<名称>` 权限对应 `config.yml` 的 `home.limits`，无匹配时用 `home.default-limit`。

#### 传送请求（TPA）

| 命令 | 权限 | 说明 |
|------|------|------|
| `/tpa <player>` | `plu.tpa` | 请求传送到目标玩家 |
| `/tpaccept` | `plu.tpa` | 接受挂起的请求 |
| `/tpdeny` | `plu.tpa` | 拒绝挂起的请求 |

超时由 `tpa.timeout` 控制；玩家退出时自动清理请求。

#### 其他

| 命令 | 权限 | 说明 |
|------|------|------|
| `/back` | `plu.back` | 回到上次死亡位置（`back.enabled` 控制） |
| `/kit [name]` | `plu.kit` | 领取礼包，省略 name 列出全部 |
| `/spawn` | `plu.spawn` | 传送到出生点 |
| `/warp [name]` | `plu.warp` | 传送到地标，省略 name 列出全部 |

### 4.2 经济系统

需要 Vault + 经济插件，否则命令提示不可用。

| 命令 | 权限 | 说明 |
|------|------|------|
| `/money` | `plu.money` | 查看自己余额（别名 `/bal` `/balance`） |
| `/money <player>` | `plu.money.others` | 查看他人余额 |
| `/pay <player> <金额>` | `plu.pay` | 转账给在线玩家，余额不足则不扣款 |
| `/baltop [数量]` | `plu.baltop` | 财富排行，默认 10 名（别名 `/balancetop`） |

排行数据缓存 5 分钟，避免频繁遍历离线玩家。

### 4.3 玩家市场

| 命令 | 权限 | 说明 |
|------|------|------|
| `/market` | `plu.market` | 打开市场 GUI（别名 `/ah`） |
| `/market sell <价格>` | `plu.market` | 上架手持物品 |
| `/market my` | `plu.market` | 查看自己的上架 |
| `/market remove <编号>` | `plu.market` | 下架并取回物品 |
| `/market <页码>` | `plu.market` | 直接打开指定页 |

**GUI 说明**：54 格，前 45 格为商品，底部 `45` 上一页、`49` 我的上架、`53` 下一页。点击商品购买。

**购买规则**：
- 不能购买自己上架的物品
- 背包满时拒绝购买
- 余额不足时拒绝
- 成交后按 `market.tax-percent` 抽税，余款存入卖家账户
- 卖家在线时会收到成交通知

### 4.4 领地圈地

| 命令 | 权限 | 说明 |
|------|------|------|
| `/claim create [半径]` | `plu.claim` | 以当前位置为中心创建方形领地，半径默认 5（别名 `/claim new`） |
| `/claim list` | `plu.claim` | 列出自己的领地 |
| `/claim remove <编号>` | `plu.claim` | 删除领地 |
| `/claim info` | `plu.claim` | 查看当前所在位置的领地归属 |
| `/claim`（无参数） | `plu.claim` | 等同于 `info` |

`/land` 为别名。

**规则**：
- 领地数量上限 `claims.max-per-player`（默认 2）
- 半径范围 `1 ~ claims.max-radius`（默认 16）
- 与他人的领地重叠时创建失败
- 保护范围：破坏方块、放置方块、操作容器（箱子/桶/熔炉/漏斗/潜影盒/附魔台/铁砧等）
- `plu.claim.bypass` 权限可无视保护

### 4.5 公告与规则（书本界面）

| 命令 | 权限 | 说明 |
|------|------|------|
| `/news` | `plu.news` | 以成书界面展示服务器公告（别名 `/announcements`） |
| `/rules` | `plu.rules` | 以成书界面展示服务器规则 |

内容在 `announcements.yml` 中维护，或通过 Web 后台"公告与规则"页面编辑。控制台执行时以纯文本逐页输出。

### 4.6 登录系统

| 命令 | 说明 |
|------|------|
| `/register <密码>` | 注册账号，密码 4-32 位，成功后自动登录 |
| `/login <密码>` | 登录已有账号 |

`login.enabled: true` 时，未注册/未登录玩家：
- 无法移动（被拉回原位）
- 无法破坏/放置方块、无法交互
- 无法聊天
- 只能执行 `/login` 与 `/register`

密码使用 SHA-256 加盐存储。`plu.login.bypass` 权限可跳过登录要求。

### 4.7 管理员命令

默认仅 OP（`default: op`）。

#### 玩家状态

| 命令 | 别名 | 权限 | 说明 |
|------|------|------|------|
| `/fly [player]` | — | `plu.fly` | 切换飞行 |
| `/gamemode <mode> [player]` | `/gm` | `plu.gamemode` | survival/creative/adventure/spectator 或 0-3 |
| `/heal [player]` | — | `plu.heal` | 满血、满食、灭火 |
| `/feed [player]` | — | `plu.feed` | 满食、满饱和 |
| `/god [player]` | — | `plu.god` | 切换无敌 |
| `/vanish [player]` | `/v` | `plu.vanish` | 切换隐身 |
| `/speed <1-10> [player]` | — | `plu.speed` | 按当前状态设置行走/飞行速度 |
| `/clear [player]` | `/ci` | `plu.clear` | 清空背包 |

省略 `[player]` 时对自己生效。

#### 背包

| 命令 | 别名 | 权限 | 说明 |
|------|------|------|------|
| `/invsee <player>` | — | `plu.invsee` | 打开并编辑目标玩家背包（带会话锁） |
| `/enderchest [player]` | `/ec` | `plu.enderchest` | 打开末影箱 |

#### 封禁（UUID）

| 命令 | 权限 | 说明 |
|------|------|------|
| `/ban <player> [reason]` | `plu.ban` | 永久封禁 |
| `/tempban <player> <duration> [reason]` | `plu.ban` | 限时封禁 |
| `/unban <player>` | `plu.ban` | 解封 |
| `/banlist` | `plu.ban` | 列出所有封禁及剩余时间 |

**时长格式**（`/tempban` 与 Web API 通用）：`数字 + 单位`

| 后缀 | 单位 | 示例 |
|------|------|------|
| `s` | 秒 | `30s` |
| `m` | 分钟 | `15m` |
| `h` | 小时 | `2h` |
| `d` | 天 | `7d` |
| `w` | 周 | `1w` |

`0` 或留空 = 永久。

#### IP 封禁

| 命令 | 权限 | 说明 |
|------|------|------|
| `/ipban <ip\|player> [reason]` | `plu.ipban` | 封禁 IP；参数为在线玩家名时自动取其 IP |
| `/ipunban <ip>` | `plu.ipban` | 解封 IP |
| `/ipbanlist` | `plu.ipban` | 列出所有 IP 封禁 |

#### 白名单

| 命令 | 权限 | 说明 |
|------|------|------|
| `/whitelist on` | `plu.whitelist` | 启用并踢出非白名单在线玩家 |
| `/whitelist off` | `plu.whitelist` | 关闭 |
| `/whitelist add <player>` | `plu.whitelist` | 添加（支持离线玩家） |
| `/whitelist remove <player>` | `plu.whitelist` | 移除 |
| `/whitelist list` | `plu.whitelist` | 列出 |

别名 `/wl`。开关状态持久化到 `config.yml` 的 `whitelist.enabled`。

#### 审计与统计

| 命令 | 权限 | 说明 |
|------|------|------|
| `/chatlog [count\|keyword]` | `plu.chatlog` | 数字 = 最近 N 条；文本 = 搜索玩家名或消息 |
| `/seen <player>` | `plu.seen` | 首次登录、最后在线、累计时长 |

#### 强制与群体

| 命令 | 权限 | 说明 |
|------|------|------|
| `/sudo <player> <command>` | `plu.sudo` | 以目标玩家身份执行命令 |
| `/healall` | `plu.healall` | 治疗所有在线玩家 |
| `/feedall` | `plu.feedall` | 喂食所有在线玩家 |

#### 其他

| 命令 | 别名 | 权限 | 说明 |
|------|------|------|------|
| `/broadcast <message>` | `/bc` | `plu.broadcast` | 全服广播 |
| `/plureload` | — | `plu.reload` | 重载配置与全部管理器 |
| `/setspawn` | — | `plu.setspawn` | 设置出生点 |
| `/setwarp <name>` | — | `plu.setwarp` | 创建地标 |
| `/delwarp <name>` | — | `plu.delwarp` | 删除地标 |

### 4.8 兑换码

| 命令 | 权限 | 说明 |
|------|------|------|
| `/redeem <兑换码>` | `plu.redeem` | 兑换货币，成功即时到账（别名 `/cdkey`） |
| `/redeem create <兑换码\|#> <金额> [次数] [时长]` | `plu.redeem.admin` | 创建兑换码，`#` 自动生成 8 位随机码 |
| `/redeem list` | `plu.redeem.admin` | 查看全部兑换码及使用情况 |
| `/redeem toggle <兑换码>` | `plu.redeem.admin` | 启用 / 停用 |
| `/redeem delete <兑换码>` | `plu.redeem.admin` | 删除 |

**参数说明**：
- **次数**：整服总可用次数，`0` = 不限；默认 `1`
- **时长**：`7d` / `12h` / `30m` / `1w`，`0` 或留空 = 永久
- **防重复**：同一玩家对同一个码只能兑换一次（与总次数限制独立）
- 兑换码不区分大小写，统一按大写存储

**示例**：

```text
/redeem create WELCOME2026 500 50 30d   # 500 货币，前 50 人，30 天内有效
/redeem create # 100 1 7d               # 随机生成一次性码
/redeem WELCOME2026
```

### 4.9 常用示例

```text
# 经济
/money
/pay Steve 100
/baltop 10

# 市场
/market sell 500        # 手持物品以 500 上架
/market my
/market remove 3

# 领地
/claim create 8         # 以自己为中心 17x17 的领地
/claim list
/claim info

# 公告与规则
/news
/rules

# 登录
/register MyPass123
/login MyPass123

# 兑换码
/redeem create WELCOME2026 500 50 30d
/redeem WELCOME2026

# 封禁
/tempban Steve 1d 使用外挂
/ipban BadGuy 恶意刷屏

# 白名单
/whitelist add Alex
/whitelist on

# 审计
/chatlog 30
/seen Steve
```

---

## 五、权限节点

| 权限 | 默认 | 用途 |
|------|------|------|
| `plu.home` / `plu.sethome` / `plu.delhome` / `plu.homes` | 所有人 | 家系统 |
| `plu.tpa` | 所有人 | 传送请求（accept/deny 同权限） |
| `plu.back` / `plu.kit` / `plu.spawn` / `plu.warp` | 所有人 | 基础玩家功能 |
| `plu.home.limit.<名称>` | — | 家数量上限 |
| `plu.money` | 所有人 | 查看自己余额 |
| `plu.money.others` | OP | 查看他人余额 |
| `plu.pay` | 所有人 | 转账 |
| `plu.baltop` | 所有人 | 财富排行 |
| `plu.market` | 所有人 | 玩家市场 |
| `plu.market.admin` | OP | 删除任意上架 |
| `plu.claim` | 所有人 | 领地功能 |
| `plu.claim.bypass` | OP | 无视领地保护 |
| `plu.claim.admin` | OP | 删除任意领地 |
| `plu.rules` / `plu.news` | 所有人 | 公告与规则 |
| `plu.login.bypass` | OP | 跳过登录要求 |
| `plu.redeem` | 所有人 | 兑换码兑换 |
| `plu.redeem.admin` | OP | 创建/停用/删除兑换码 |
| `plu.setspawn` / `plu.setwarp` / `plu.delwarp` | OP | 维护类 |
| `plu.fly` / `plu.gamemode` / `plu.heal` / `plu.feed` | OP | 玩家状态 |
| `plu.god` / `plu.vanish` / `plu.speed` / `plu.clear` | OP | 玩家状态 |
| `plu.invsee` / `plu.enderchest` | OP | 背包查看 |
| `plu.ban` | OP | 封禁（ban/tempban/unban/banlist） |
| `plu.ipban` | OP | IP 封禁 |
| `plu.whitelist` | OP | 白名单 |
| `plu.chatlog` / `plu.seen` | OP | 审计统计 |
| `plu.sudo` / `plu.healall` / `plu.feedall` | OP | 强制与群体操作 |
| `plu.broadcast` / `plu.reload` | OP | 消息与维护 |

---

## 六、Web 管理后台

浏览器访问 `http://<服务器IP>:8080/`，使用 `config.yml` 中 `web.password` 登录。

### 页面

仪表盘、玩家管理、控制台、插件管理、世界管理、经济系统、**玩家市场**、**领地管理**、**公告与规则**、**登录账号**、封禁管理、IP 封禁、白名单、聊天记录、Warp 管理、定时任务、服务器控制。

### 鉴权

除 `POST /api/login` 外，所有请求需携带：

```
Authorization: Bearer <token>
```

Token 有效期由 `web.token-expiry`（分钟）控制，默认 60。

### 错误格式

```json
{ "error": "描述" }
```

| 状态码 | 含义 |
|--------|------|
| 200 | 成功 |
| 400 | 参数错误 |
| 401 | 未登录 / Token 无效 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |
| 503 | 依赖不可用（如未装经济插件时调用经济接口） |

---

## 七、REST API 参考

### 认证

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/login` | 请求体 `{"password":"..."}` → `{"token":"..."}` |
| POST | `/api/logout` | 吊销当前 Token → `{"ok":true}` |

### 仪表盘

`GET /api/dashboard`

```json
{
  "tps": 20.0, "online": 12, "maxPlayers": 100,
  "version": "1.21.2", "bukkitVersion": "1.21.2-R0.1-SNAPSHOT",
  "motd": "A Minecraft Server",
  "memoryUsedMb": 512, "memoryMaxMb": 2048, "memoryPercent": 25,
  "uptimeMs": 3600000,
  "vaultEnabled": true, "vaultInstalled": true, "luckpermsEnabled": true,
  "pluginName": "PluEssentials", "pluginVersion": "2.0.0"
}
```

> `vaultInstalled`：Vault 本体是否安装；`vaultEnabled`：经济服务是否就绪。检测为懒加载，经济插件加载后自动恢复 true。

### 玩家

`GET /api/players` — 在线玩家列表，字段：`name/uuid/health/food/level/gamemode/flying/god/vanished/world/ip/ping/primaryGroup/balance`

`POST /api/players/{name}/{action}`

| action | value | 说明 |
|--------|-------|------|
| `kick` | — | 踢出 |
| `heal` | — | 治疗 |
| `feed` | — | 喂食 |
| `kill` | — | 击杀 |
| `god` | — | 切换无敌 |
| `fly` | — | 切换飞行 |
| `clear` | — | 清空背包 |
| `gamemode` | 模式 | survival/creative/adventure/spectator 或 0-3 |
| `message` | 内容 | 私聊（支持 `&`） |
| `teleport` | — | 传送到出生点 |
| `inventory` | — | 返回背包数据 |
| `ban` | 原因（可选） | 永久封禁 |

### 控制台

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/console?lines=200` | 最近日志 `{"lines":[...]}` |
| POST | `/api/console` | 请求体 `{"command":"say Hello"}` |

### 插件

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/plugins` | 插件列表 |
| POST | `/api/plugins/{name}/{action}` | `enable` / `disable` / `reload` |

### 世界

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/worlds` | `name/environment/players/time/weather/difficulty/seed` |
| POST | `/api/worlds/{name}/{action}` | `time`(value=刻) / `day` / `night` / `weather`(clear\|rain\|thunder) / `difficulty` |

### 经济（需 Vault + 经济插件）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/economy/{player}` | `{"player","balance","currency"}` |
| POST | `/api/economy/{player}/{set\|add\|remove}` | 请求体 `{"amount":100}` |

### 权限（需 LuckPerms）

`GET /api/permissions/{player}` → `{"primaryGroup","groups","available"}`

### 家

`GET /api/homes/{player}` → `{"homes":["home","base"],"limit":5}`

### 玩家市场

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/market` | `{"listings":[{id,seller,sellerName,type,amount,displayName,price,created}],"count","available"}` |
| DELETE | `/api/market/{id}` | 删除上架（管理员） |

### 领地

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/claims` | `{"claims":[{id,owner,ownerName,world,minX,minZ,maxX,maxZ,area,created}],"count"}` |
| DELETE | `/api/claims/{id}` | 删除领地 |

### 公告与规则

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/announcements` | `{"news":{"title","author","pages":[]},"rules":{...}}` |
| POST | `/api/announcements/{news\|rules}` | 请求体 `{"pages":["第一页","第二页"]}` |

### 登录账号

`GET /api/accounts` → `{"enabled":true,"accounts":[{uuid,name,online,authed}],"count"}`

### 兑换码

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/redeem` | `{"codes":[{code,amount,maxUses,usedCount,expires,created,creator,enabled,expired}],"count","economyAvailable"}` |
| POST | `/api/redeem` | `{"code":"WELCOME","amount":500,"maxUses":50,"duration":"30d"}`，`code` 传 `#` 自动生成 |
| POST | `/api/redeem/{code}/toggle` | 启用 / 停用 |
| DELETE | `/api/redeem/{code}` | 删除 |

### 封禁（UUID）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/bans` | `{"bans":[{uuid,name,reason,banner,created,expires,remaining,permanent}]}` |
| POST | `/api/bans` | 请求体 `{"name","reason","duration":"7d"}` |
| DELETE | `/api/bans/{uuid}` | 解封 |

`expires=0` 即永久，`remaining=-1` 表示永久。

### IP 封禁

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/ipbans` | `{"bans":[{ip,reason,banner,created,expires}]}` |
| POST | `/api/ipbans` | 请求体 `{"ip","reason"}` |
| DELETE | `/api/ipbans/{ip}` | 解封 |

### 白名单

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/whitelist` | `{"enabled","list":[{uuid,name}]}` |
| POST | `/api/whitelist` | 请求体 `{"enabled":true}` 切换开关 |
| POST | `/api/whitelist/{uuidOrName}` | 添加玩家（路径参数支持 UUID 或玩家名） |
| DELETE | `/api/whitelist/{uuidOrName}` | 移除玩家 |

### 聊天记录

`GET /api/chat?limit=50&player=Steve`

```json
{ "logs": [{ "time": 1700000000000, "name": "Steve", "uuid": "...", "world": "world", "message": "hello" }] }
```

### 玩家统计

`GET /api/stats/{player}` → `{"name","firstJoin","lastSeen","playtimeMs","online"}`

### Warp

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/warps` | `{"warps":[{name,world,x,y,z}],"count"}` |
| POST | `/api/warps` | `{"name","player"}` 或 `{"name","world","x","y","z"}` |
| DELETE | `/api/warps/{name}` | 删除 |

### 定时任务

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/tasks` | `{"tasks":[{id,name,command,cron,enabled,lastRun}]}` |
| POST | `/api/tasks` | `{"name","command","cron"}` → `{"id":N}` |
| PUT | `/api/tasks/{id}` | `{"name","command","cron","enabled"}` |
| DELETE | `/api/tasks/{id}` | 删除 |
| POST | `/api/tasks/{id}/run` | 立即执行一次 |

### 服务器控制

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/broadcast` | `{"message":"..."}` |
| POST | `/api/server/stop` | `{"delay":5}` 延迟秒数，默认 0 |
| POST | `/api/server/reload` | 执行 `Bukkit.reload()` |

---

## 八、数据存储

由 `database.enabled` 切换，管理器代码对两种后端透明：

| 后端 | 位置 |
|------|------|
| YAML | `plugins/PluEssentials/*.yml`（各模块独立文件） |
| MySQL | `plu_*` 表（启动自动建表） |

> 两种后端之间**不会自动迁移数据**，切换前请手动导出导入。

### 数据表

| 表 | 字段 | 说明 |
|----|------|------|
| `plu_bans` | uuid, name, reason, banner, created, expires | UUID 封禁 |
| `plu_warps` | name, world, x, y, z, yaw, pitch | 地标 |
| `plu_tasks` | id, name, command, cron, enabled, last_run | 定时任务 |
| `plu_homes` | uuid, name, world, x, y, z, yaw, pitch | 家（联合主键） |
| `plu_ipbans` | ip, reason, banner, created, expires | IP 封禁 |
| `plu_whitelist` | uuid, name | 白名单 |
| `plu_chatlog` | id, time, name, uuid, world, message | 聊天记录 |
| `plu_playerstats` | uuid, name, first_join, last_seen, playtime | 玩家统计 |
| `plu_market` | id, seller, seller_name, item, price, created | 市场上架（item 为 Base64 序列化） |
| `plu_claims` | id, owner, owner_name, world, min_x, min_z, max_x, max_z, created | 领地 |
| `plu_accounts` | uuid, name, salt, hash, registered | 登录账号（密码加盐哈希） |
| `plu_redeem_codes` | code, amount, max_uses, used_count, expires, created, creator, enabled | 兑换码定义 |
| `plu_redeem_uses` | code, uuid, used_at | 兑换记录（防重复，联合主键） |

---

## 九、开发与部署

### 技术栈

| 层 | 技术 | 版本 |
|----|------|------|
| 服务端 | Bukkit/Spigot API | 1.21.2-R0.1-SNAPSHOT |
| 语言 | Java | 21 |
| 构建 | Maven | 3.9+ |
| Web 服务 | JDK 内置 `com.sun.net.httpserver.HttpServer` | — |
| 数据库 | MySQL 可选 | 驱动 8.4.0（shade 打入 jar） |
| 前端 | Vue 3 + Vite 5 + Element Plus + Axios | — |

### 目录结构

```
plu/
├── pom.xml                          # Maven 配置（shade 打包 MySQL 驱动）
├── README.md                        # 本文档（单文件完整文档）
├── src/main/
│   ├── java/com/plu/plugin/
│   │   ├── Main.java                # 主类：生命周期、命令注册、管理器装配
│   │   ├── commands/
│   │   │   ├── user/                # 玩家命令（home/tpa/kit/warp/money/pay/market/claim/login/...）
│   │   │   └── admin/               # 管理员命令（fly/god/ban/ipban/whitelist/invsee/...）
│   │   ├── gui/
│   │   │   ├── MarketGui.java       # 市场界面（InventoryHolder）
│   │   │   └── BookGui.java         # 成书界面（公告/规则）
│   │   ├── listeners/
│   │   │   ├── PlayerListener.java  # 登录检查、聊天审计、统计、invsee 会话
│   │   │   ├── MarketListener.java  # 市场 GUI 点击
│   │   │   ├── ClaimListener.java   # 领地方块保护
│   │   │   └── LoginListener.java   # 未登录行为限制
│   │   ├── managers/                # 业务管理器（统一 Plugin+DatabaseManager 构造）
│   │   ├── storage/
│   │   │   └── DatabaseManager.java # MySQL 连接、自动建表
│   │   └── web/
│   │       ├── WebServer.java       # HttpServer 启动、静态资源
│   │       ├── ApiRouter.java       # /api/* 全部端点
│   │       ├── Auth.java            # Token 鉴权
│   │       ├── ConsoleBuffer.java   # 控制台日志缓冲
│   │       ├── TpsTracker.java      # TPS 采样
│   │       ├── Json.java            # 轻量 JSON 序列化
│   │       └── hooks/               # VaultHook、LuckPermsHook
│   └── resources/
│       ├── plugin.yml               # 命令、权限声明
│       ├── config.yml               # 默认配置
│       └── web/                     # Vue 构建产物（打入 jar）
└── web-frontend/                    # Vue3 前端源码
    └── src/
        ├── views/                   # 页面组件
        ├── router/index.js          # 路由（菜单由 meta 自动生成）
        └── api/index.js             # Axios 封装
```

### 构建流程

前置要求：JDK 21、Maven 3.9+、Node.js 20+（仅前端构建需要）。

```bash
# 1. 构建前端
cd web-frontend
npm install
npm run build            # 产物 → web-frontend/dist/

# 2. 复制前端产物到插件资源（Windows PowerShell）
Remove-Item -Recurse -Force src/main/resources/web
Copy-Item -Recurse web-frontend/dist src/main/resources/web

# 3. 构建插件 jar
mvn clean package        # 产物 → target/PluEssentials-2.0.0.jar
```

依赖说明：`spigot-api`、`VaultAPI` 为 `provided`（服务端提供）；`mysql-connector-j` 及其传递依赖 `protobuf-java` 由 `maven-shade-plugin` 打入 jar。

### 架构决策

**1. Web 服务使用 JDK 内置 HttpServer**
不引入 Spring Boot / Jetty，保持单 jar 交付。请求在独立线程处理，**所有修改 Bukkit 状态的操作必须通过 `Bukkit.getScheduler().runTask` 调度回主线程**。

**2. 双存储后端**
所有持久化管理器统一构造签名 `XxxManager(Main plugin, DatabaseManager db)`，内部按 `db.isEnabled()` 分流 MySQL 或 YAML。

**3. 软依赖懒检测**
- Vault：`isAvailable()` 在服务未就绪时自动重试 `ServicesManager` 查询，解决经济插件后于本插件加载导致的检测失败。
- LuckPerms：反射调用，避免编译期硬依赖。

**4. 前端资源交付**
构建产物打入 jar 的 `web/` 目录；若 `web.external-web-dir` 非空则优先读取外部目录，便于前端热更新。

**5. 市场物品序列化**
使用 `BukkitObjectOutputStream` + Base64，保留完整 ItemStack（含 NBT/附魔/自定义名称），存入 `plu_market.item` TEXT 字段。

### 事件流程

| 事件 | 处理 |
|------|------|
| `PlayerLoginEvent`（HIGHEST） | IP 封禁 → UUID 封禁 → 白名单 依次检查 |
| `PlayerJoinEvent` | 隐身状态、玩家统计、登录提示 |
| `PlayerQuitEvent` | 清理 TPA/隐身、结算在线时长 |
| `AsyncPlayerChatEvent` | 聊天审计；未登录时拦截 |
| `InventoryCloseEvent` | 释放 `/invsee` 会话锁 |
| `InventoryClickEvent` | 市场 GUI 购买/翻页 |
| `BlockBreakEvent` / `BlockPlaceEvent` / `PlayerInteractEvent` | 领地保护；未登录拦截 |
| `PlayerMoveEvent` | 未登录玩家禁止位移 |
| `PlayerCommandPreprocessEvent` | 未登录仅允许 `/login` `/register` |
| `EntityDamageEvent`（LOWEST） | 无敌模式取消伤害 |
| `PlayerDeathEvent` | 记录死亡位置、清理无敌 |

### 扩展指南

**新增命令**
1. `commands/admin|user/` 新建类实现 `CommandExecutor`
2. `plugin.yml` 的 `commands` 与 `permissions` 节声明
3. `Main.registerAdminCommands()` / `registerUserCommands()` 注册

**新增管理器**
1. `managers/` 新建类，构造 `(Main plugin, DatabaseManager db)`
2. 实现 `load()`，按 `db.isEnabled()` 分流
3. `Main` 中添加字段、`onEnable()` 实例化并 `load()`、`reload()` 中 `load()`、提供 getter

**新增 Web API**
1. `ApiRouter.handle()` 的 dispatch 块添加路由
2. 实现 `handleXxx(HttpExchange, String[] parts, String method)`
3. 主线程操作用 `runTask`；返回 `WebServer.sendJson(exchange, status, Json.stringify(map))`

**新增前端页面**
1. `web-frontend/src/views/` 新建 `.vue`
2. `router/index.js` 添加路由（`meta.title` / `meta.icon` 自动进菜单）
3. 重新构建 → 复制 dist → `mvn clean package`

---

## 十、常见问题

**Q：仪表盘显示"Vault 已安装（缺少经济插件）"**
装 Vault 只提供对接层，还需安装提供经济的插件（EssentialsX、CMI、Craftconomy 等）。PluEssentials 会自动重试检测，无需重启。

**Q：市场打不开，提示经济系统不可用**
同上的依赖问题；市场依赖经济系统做扣款与结算。

**Q：白名单添加玩家报错 Invalid UUID**
2.0 已支持路径传玩家名；若使用旧版请升级，或在前端传 UUID。

**Q：领地创建失败提示重叠**
领地是二维矩形（不限高度），与已有领地范围相交即失败。换位置或先删除其他领地。

**Q：玩家登录后仍被限制移动**
检查 `login.enabled`，确认已执行 `/register` 或 `/login`；`plu.login.bypass` 权限可跳过。

**Q：修改配置后不生效**
执行 `/plureload`，或在 Web 后台"服务器控制"点击重载。市场、领地、账号等管理器会在 reload 时重新读取数据。

**Q：切换 MySQL 后旧数据丢失**
两种后端不自动迁移。先备份 YAML 文件，手动导入数据库。
