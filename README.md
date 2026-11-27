<div align="center">

# PluEssentials

**Minecraft 1.21.2 一体化服务端插件**
玩家功能 · 管理工具 · 经济系统 · 玩家市场 · 领地保护 · 充值 · 菜单 / NPC / 告示牌 · 内嵌 Web 管理后台

![Minecraft](https://img.shields.io/badge/Minecraft-1.21.2-62B47A?style=flat-square)
![Java](https://img.shields.io/badge/Java-21-orange?style=flat-square)
![Version](https://img.shields.io/badge/version-3.0.0-blue?style=flat-square)
![API](https://img.shields.io/badge/api--version-1.21-lightgrey?style=flat-square)
![Economy](https://img.shields.io/badge/economy-built--in-2ea44f?style=flat-square)
![Groups](https://img.shields.io/badge/groups-built--in-2ea44f?style=flat-square)
![Recharge](https://img.shields.io/badge/recharge-WeChat%20JSAPI-07C160?style=flat-square)
![Nickname](https://img.shields.io/badge/nickname-Chinese-ff69b4?style=flat-square)
![Deps](https://img.shields.io/badge/dependencies-none-brightgreen?style=flat-square)
![Build](https://img.shields.io/badge/build-Maven%20%7C%20Vite-success?style=flat-square)
![License](https://img.shields.io/badge/license-MIT-blue?style=flat-square)

[功能](#功能特性) · [安装](#快速开始) · [配置](#配置说明) · [经济](#经济系统) · [权限组](#权限组) · [充值](#充值系统) · [昵称](#中文昵称与中文-id) · [命令](#命令) · [Web 后台](#web-管理后台) · [API](#rest-api) · [构建](#从源码构建)

**作者 / 开发者 / 仓库维护者：[stonestsy](https://github.com/stonestsy)**

</div>

---

## 简介

PluEssentials 把开服常用的功能收进**单个 jar**：玩家基础指令、管理员工具、安全管控、经济与交易、领地保护、登录系统、公告规则，以及一个**内嵌的 Web 管理后台**（JDK 自带 HttpServer，不需要额外部署 Nginx/Spring）。

3.0.0 新增：**微信支付 JSAPI 充值**（未配置商户凭据时自动进入模拟支付）、**中文昵称与中文 ID 兼容**、**自定义菜单**、**NPC（内置 + Citizens）**、**命令告示牌**，并把所有命令的用法与权限节点补全到本文档。

不引入重量级框架，Web 前端为 Vue 3 构建产物，直接打进 jar；数据层支持 **MySQL** 与 **YAML** 两种后端，按需切换。

**经济与权限组均为内置实现**：余额、转账、财富排行、玩家市场（GUI 交易）、兑换码发币、后台余额调整形成闭环；权限组支持分组、前缀、权重、权限节点，并在登录时自动注入 Bukkit 权限体系。**无需安装 Vault、经济插件或 LuckPerms**，详见 [经济系统](#经济系统) 与 [权限组](#权限组)。

| 项目 | 说明 |
|------|------|
| 版本 | 3.0.0 |
| 服务端 | Bukkit / Spigot / Paper 1.21.2 |
| Java | 21 |
| 产物 | `PluEssentials-3.0.0.jar`（约 5.4 MB，内含 MySQL 驱动与前端资源） |
| 外部依赖 | **无**（经济、权限组、Web 后台、充值、NPC 全部内置；Citizens 可选） |

---

## 功能特性

### 玩家功能

| 模块 | 命令 | 说明 |
|------|------|------|
| 家 | `/home` `/sethome` `/delhome` `/homes` | 多家支持，数量上限按权限分组 |
| 传送请求 | `/tpa` `/tpaccept` `/tpdeny` | 带超时，离线自动清理 |
| 死亡回传 | `/back` | 回到上次死亡位置 |
| 礼包 | `/kit` | 支持冷却时间 |
| 出生点 | `/spawn` | 管理员用 `/setspawn` 设置 |
| 地标 | `/warp` | 管理员用 `/setwarp` `/delwarp` 维护 |
| 经济 | `/money` `/pay` `/baltop` | 余额、转账、财富排行 |
| 玩家市场 | `/market` | GUI 商店，上架 / 购买 / 下架 |
| 领地 | `/claim` | 圈地 + 方块与容器保护 |
| 公告 | `/news` | 以**成书界面**展示 |
| 规则 | `/rules` | 以**成书界面**展示 |
| 登录 | `/register` `/login` | 未登录禁止移动、交互、聊天 |
| 兑换码 | `/redeem` | 兑换码换货币，限量 / 限时 / 一码一人一次 |
| 中文昵称 | `/nick` | 中文显示名（聊天 / TAB），真实登录名不变 |
| 充值 | `/charge` | 获取充值绑定码与微信 JSAPI 充值页 |
| 自定义菜单 | `/menu` | YAML 配置的 GUI 菜单，点击执行命令 / 打开子菜单 |

### 管理员功能

| 模块 | 命令 |
|------|------|
| 玩家状态 | `/fly` `/gamemode` `/heal` `/feed` `/god` `/vanish` `/speed` `/clear` |
| 背包查看 | `/invsee`（可编辑，带会话锁）、`/enderchest` |
| 封禁（UUID） | `/ban` `/tempban` `/unban` `/banlist` |
| 封禁（IP） | `/ipban` `/ipunban` `/ipbanlist`，登录阶段拦截 |
| 白名单 | `/whitelist on\|off\|add\|remove\|list`（别名 `/wl`） |
| 审计统计 | `/chatlog`（聊天记录）、`/seen`（首次登录 / 在线时长） |
| 强制操作 | `/sudo` `/healall` `/feedall` |
| 权限组 | `/group`（分组、前缀、权重、权限节点，替代 LuckPerms） |
| NPC | `/npc`（内置实体 / Citizens 玩家皮肤 NPC，可执行命令或打开菜单） |
| 命令告示牌 | `/cmdsign`（右键告示牌执行命令，可设权限节点） |
| 消息维护 | `/broadcast` `/plureload` `/setwarp` `/delwarp` |

### Web 管理后台

浏览器打开 `http://<服务器IP>:8080/` 即可管理：

- **仪表盘** — TPS、内存、在线人数、运行时长、内置经济与权限组状态、全服货币总量
- **玩家管理** — 踢出 / 治疗 / 喂食 / 击杀 / 无敌 / 飞行 / 清包 / 改模式 / 私聊 / 传送 / 查看背包 / 封禁
- **控制台** — 实时日志 + 执行任意命令
- **插件与世界** — 启停重载插件；时间 / 天气 / 难度
- **经济 · 玩家市场 · 权限组 · 领地 · 兑换码** — 全图形化管理
- **充值管理** — 礼包增删改、订单列表、人工确认到账、模拟支付状态
- **中文昵称** — 网页设置 / 清除玩家昵称
- **自定义菜单** — 可视化编辑标题、行数、槽位、材料、描述、命令、子菜单
- **NPC 管理** — 创建、编辑动作、删除、Citizens 状态提示
- **命令告示牌** — 列表、添加、删除、权限节点
- **公告与规则** — 网页编辑，游戏内书本渲染
- **登录账号 · 封禁 · IP 封禁 · 白名单 · 聊天记录**
- **Warp · 定时任务（Cron）· 服务器控制**（关服 / 重载 / 广播）

---

## 快速开始

### 1. 环境要求

- 服务端 **Java 21**
- Minecraft **1.21.2**（Bukkit / Spigot / Paper）

### 2. 安装

```text
1. 下载 PluEssentials-3.0.0.jar
2. 放入服务端 plugins/ 目录
3. 启动服务端，自动生成 plugins/PluEssentials/ 配置与数据文件
4. 完成 —— 经济、权限组、Web 后台开箱即用，无需安装任何前置插件
```

> 经济、权限组、登录、市场、领地全部由本插件自身实现，不依赖 Vault / LuckPerms。若你原本装了它们，可以保留（互不冲突），也可以移除。

### 3. 登录后台

编辑 `plugins/PluEssentials/config.yml` 设置密码后重启（或 `/plureload`）：

```yaml
web:
  enabled: true
  host: "0.0.0.0"
  port: 8080
  password: "改成你的强密码"
```

浏览器访问 `http://<服务器IP>:8080/`。

---

## 配置说明

配置文件位于 `plugins/PluEssentials/config.yml`：

```yaml
prefix: "&7[&bPluEssentials&7] "

# ---------- Web 管理后台 ----------
web:
  enabled: true
  host: "0.0.0.0"          # 监听地址
  port: 8080               # 监听端口
  password: "admin123"     # 登录密码，生产环境务必修改
  token-expiry: 60         # 会话有效期（分钟）
  console-buffer: 500      # 控制台日志缓存行数
  external-web-dir: ""     # 外部前端目录，留空使用 jar 内嵌页面

# ---------- 数据存储 ----------
database:
  enabled: false           # true 使用 MySQL，false 使用 YAML 文件
  host: "localhost"
  port: 3306
  database: "pluessentials"
  username: "root"
  password: ""
  table-prefix: "plu_"     # 启动时自动建表

# ---------- 出生点 ----------
spawn:
  use-world-spawn: true    # 未设置出生点时使用世界出生点

# ---------- 白名单 ----------
whitelist:
  enabled: false           # /whitelist on|off 会同步修改此项

# ---------- 家 ----------
home:
  default-limit: 5
  limits:                  # 权限 plu.home.limit.<名称> 对应上限
    vip: 20
    admin: 50

# ---------- 传送请求 ----------
tpa:
  timeout: 60              # 秒

# ---------- 死亡回传 ----------
back:
  enabled: true
  death-message: "&cYou died! Use &e/back &cto return to your death point."

# ---------- 礼包 ----------
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

# ---------- 玩家市场 ----------
market:
  tax-percent: 0           # 成交税率（百分比，0 = 免税）
  max-items-per-player: 10 # 每人最多上架数量

# ---------- 内置经济 ----------
economy:
  enabled: true
  currency-name: "金币"     # 货币名称，用于消息与网页展示
  currency-symbol: ""      # 货币符号，如 "$" 会显示为 $1,234.56
  decimals: 2              # 小数位数（0-4）
  starting-balance: 100    # 首次进服发放的初始余额（0 = 不发放）

# ---------- 领地 ----------
claims:
  max-per-player: 2        # 每人最多领地数量
  max-radius: 16           # 单块最大半径（边长 = 2*半径+1）

# ---------- 自定义 TAB ----------
tab:
  enabled: true
  header: "&b&lPluEssentials &7| &f在线 &a{online}&7/&a{max}"
  footer: "&7世界: &a{world} &7| 余额: &a{balance} &7| 时间: &a{time}"

# ---------- 登录系统 ----------
login:
  enabled: true

# ---------- 中文昵称 ----------
nickname:
  enabled: true
  max-length: 16           # 按字符数，中文算 1 个

# ---------- 中文 ID 兼容 ----------
compat:
  # 原版客户端仍只允许英文名；开启后本插件不会主动拒绝非 ASCII 名字
  allow-non-ascii-names: true

# ---------- 自定义菜单 / NPC / 命令告示牌 ----------
menus:
  enabled: true
npcs:
  enabled: true
  use-citizens: false      # 已装 Citizens 时是否全部交给 Citizens（player 类型始终尝试）
cmdsigns:
  enabled: true
  write-label: true        # 创建时自动改写告示牌前三行

# ---------- 充值（微信支付 APIv3 JSAPI 公众号支付） ----------
wechatpay:
  enabled: false           # 充值总开关
  mock: true               # true = 模拟支付（无需商户凭据）
  app-id: ""               # 公众号 AppID
  app-secret: ""           # 公众号 AppSecret（OAuth 换 openid）
  mch-id: ""               # 商户号
  cert-serial-no: ""       # 商户 API 证书序列号
  private-key-path: "apiclient_key.pem"  # 商户私钥（PKCS#8 PEM）
  api-v3-key: ""           # 32 位 APIv3 密钥
  notify-url: ""           # https://你的域名/api/pay/notify
  public-base-url: ""      # https://你的域名，留空则用 127.0.0.1:端口
  platform-cert-path: ""   # 微信支付平台证书，配置后校验回调签名
  description: "Minecraft 服务器充值"
recharge:
  min-fen: 1               # 单笔最低金额（分）
  max-fen: 1000000         # 单笔最高金额（分）
```

### 公告与规则

`plugins/PluEssentials/announcements.yml`（首次启动自动生成，也可在 Web 后台「公告与规则」编辑）：

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

> **每个列表项 = 书本的一页**，支持 `&` 颜色代码与 `\n` 换行。

### TAB 占位符

| 占位符 | 含义 | 占位符 | 含义 |
|--------|------|--------|------|
| `{player}` | 玩家名 | `{balance}` | 余额（经济不可用显示 `N/A`） |
| `{online}` | 在线人数 | `{time}` | 现实时间 `HH:mm` |
| `{max}` | 最大人数 | `{world}` | 所在世界 |
| `{ping}` | 延迟（ms） | | |

TAB 每 5 秒刷新一次。

### 生产环境建议

- 修改 `web.password` 默认密码
- 用防火墙限制 8080 端口来源，或经 Nginx 反代并启用 HTTPS
- MySQL 避免空密码
- 切换存储后端**不会自动迁移数据**，请先手动导出导入

---

## 经济系统

经济是本项目的核心模块之一，由 **货币操作 + 玩家市场 + 兑换码 + 财富排行 + 后台管理** 五部分组成。

> 整套经济**完全内置**：余额存在本插件自己的数据表（`plu_balances` / `balances.yml`），**不需要 Vault，也不需要任何经济插件**。

```mermaid
flowchart LR
    P[玩家] -->|/money /pay /baltop| EM[内置经济管理器]
    P -->|/market 上架·购买·下架| MK[玩家市场]
    P -->|/redeem 兑换| RD[兑换码]
    MK -->|扣款 · 结算 · 抽税| EM
    RD -->|兑换入账| EM
    EM --> ST[(plu_balances / balances.yml)]
    WEB[Web 管理后台] -->|REST API| EM
    WEB --> MK
    WEB --> RD
```

### 1. 启用与配置

无需任何前置插件，开箱即用。相关配置：

```yaml
economy:
  enabled: true            # false 则关闭全部经济功能（市场、兑换码、排行一并停用）
  currency-name: "金币"     # 货币名称，显示在消息与网页
  currency-symbol: ""      # 货币符号，如 "$"，非空时显示为 $1,234.56
  decimals: 2              # 小数位数（0-4），余额按此精度四舍五入
  starting-balance: 100    # 玩家首次进服自动发放的初始余额（0 = 不发放）
```

| 场景 | 表现 |
|------|------|
| 正常 | `/money` 等命令可用，仪表盘显示「内置经济」绿色 |
| `enabled: false` | 经济相关命令提示「经济系统已关闭」，市场、兑换码同步不可用，经济接口返回 503 |
| 玩家首次进服 | 自动建档并按 `starting-balance` 发放初始余额 |

> 若从旧版本（Vault 方案）升级：旧余额存在原经济插件里，不会自动导入。需要时可用后台「经济系统」页按玩家名逐个 `set`，或先用 `/money` 记录后手工迁移。


### 2. 货币操作

| 命令 | 别名 | 权限 | 说明 |
|------|------|------|------|
| `/money` | `/bal` `/balance` | `plu.money` | 查看自己的余额，数值经经济插件 `Economy#format` 格式化（含货币单位） |
| `/money <player>` | 同上 | `plu.money.others` | 查看他人余额（默认仅 OP） |
| `/pay <player> <金额>` | — | `plu.pay` | 向在线玩家转账 |
| `/baltop [数量]` | `/balancetop` | `plu.baltop` | 财富排行榜 |

**行为细节**

- `/pay` 仅玩家可用；目标必须在线；**不能给自己转账**；金额必须 > 0
- 转账先扣款再入账，**余额不足直接失败且不扣款**，并回显当前余额
- 转账成功双方都会收到到账 / 支出提示
- `/baltop` 只统计余额 > 0 的玩家，按余额降序；默认 10 条，最多 50 条
- 排行榜结果**缓存 5 分钟**，避免每次都遍历全部离线玩家（首次查询在玩家量大时会有短暂耗时）

### 3. 玩家市场

玩家之间自由交易的 GUI 商店，货币结算走内置经济。

#### 使用流程

```text
上架：手持物品 → /market sell <价格> → 物品从手上扣除并进入市场
浏览：/market            → 打开 GUI，点击商品即可购买
管理：/market my         → 列出自己的上架（含编号）
下架：/market remove <编号> → 物品退回背包
```

#### GUI 布局

```text
┌──────────────────────────────────────────────────┐
│  0   1   2   3   4   5   6   7   8  ...   44     │  45 个商品位
├──────────────────────────────────────────────────┤
│ 45[上一页]   ·   ·   49[我的上架]   ·   ·  53[下一页] │  灰色玻璃板填充
└──────────────────────────────────────────────────┘
```

每个商品图标保留原物品外观，lore 追加卖家、价格、编号与「点击购买」。

#### 交易规则

| 场景 | 行为 |
|------|------|
| 购买自己上架的物品 | 拒绝 |
| 背包已满（无空位） | 拒绝，提示腾出空间 |
| 余额不足 | 拒绝，提示所需金额 |
| 正常成交 | 买家扣款 → 按税率抽税 → 卖家入账 → 物品进背包 → 刷新界面 |
| 卖家在线 | 收到「已售出 + 收入（含税额说明）」通知 |
| 卖家离线 | 收入直接入账，下次登录可查余额 |
| 下架 | 校验归属（管理员可用 `plu.market.admin` 越权），背包满则拒绝取回 |

#### 配置

```yaml
market:
  tax-percent: 0           # 成交税率（百分比）。5 表示每笔交易抽 5%，0 为免税
  max-items-per-player: 10 # 每人同时上架的上限
```

#### 物品保真

上架物品使用 `BukkitObjectOutputStream` 序列化后 Base64 存入 `plu_market.item`，**完整保留**附魔、耐久、自定义名称、lore、NBT，不会退化为白板物品。

### 4. 兑换码

用于活动发币、补偿、推广，一个码可限制总次数与有效期。

| 命令 | 权限 | 说明 |
|------|------|------|
| `/redeem <兑换码>` | `plu.redeem` | 兑换货币，成功即时到账（别名 `/cdkey`） |
| `/redeem create <兑换码\|#> <金额> [次数] [时长]` | `plu.redeem.admin` | 创建；`#` 表示自动生成 8 位随机码 |
| `/redeem list` | `plu.redeem.admin` | 查看全部码、使用次数与到期时间 |
| `/redeem toggle <兑换码>` | `plu.redeem.admin` | 启用 / 停用（不删除，可随时恢复） |
| `/redeem delete <兑换码>` | `plu.redeem.admin` | 删除，同时清理兑换记录 |

**参数含义**

| 参数 | 说明 |
|------|------|
| 金额 | 必填，必须 > 0 |
| 次数 | 整服总可用次数，`0` = 不限；默认 `1` |
| 时长 | `7d` / `12h` / `30m` / `1w`；`0` 或留空 = 永久 |
| 兑换码 | 自动转大写存储，输入不区分大小写 |

**规则与边界**

- **一码一人一次**：同一玩家对同一个码只能兑换一次，与总次数限制相互独立（`plu_redeem_uses` 联合主键 `code + uuid` 兜底）
- 随机码字符集为 `A-HJ-NP-Z2-9`（剔除 `0/O/1/I`），避免口头传播时认错
- 兑换失败不会产生任何扣减，提示明确区分原因：

| 提示 | 触发条件 |
|------|----------|
| 兑换码不存在 | 码未创建 |
| 该兑换码已被停用 | `enabled = false` |
| 该兑换码已过期 | 已过到期时间 |
| 该兑换码已被用完 | 达到总次数上限 |
| 你已经兑换过这个码了 | 同一玩家重复兑换 |
| 经济系统不可用 | `economy.enabled` 为 `false` |

**示例**

```text
# 前 100 名玩家各领 500，30 天内有效
/redeem create WELCOME2026 500 100 30d

# 生成一次性随机码，7 天有效
/redeem create # 1000 1 7d

# 停用但仍保留记录（便于追溯）
/redeem toggle WELCOME2026
```

### 5. 后台与接口

**Web 后台页面**

| 页面 | 能力 |
|------|------|
| 经济系统 | 按玩家名查询余额，执行 `set` / `add` / `remove` |
| 玩家市场 | 查看全部上架（卖家、物品、价格、时间），删除违规上架 |
| 兑换码 | 新建（含随机码）、启用 / 停用、删除，状态用标签区分可用 / 已停用 / 已过期 |

**REST API**

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/economy/{player}` | 余额、货币名与格式化文本 |
| POST | `/api/economy/{player}/{set\|add\|remove}` | 请求体 `{"amount":100}`，返回最新余额 |
| GET | `/api/permissions/{player}` | 主分组、分组列表、前缀、生效权限（支持离线玩家） |
| GET | `/api/groups` | 分组列表（含权限节点与成员数） |
| POST | `/api/groups` | `{"name":"vip","prefix":"&6[VIP]","weight":10}` |
| PUT | `/api/groups/{name}` | 更新 `prefix` / `weight` |
| DELETE | `/api/groups/{name}` | 删除分组 |
| POST | `/api/groups/{name}/permissions` | `{"permission":"plu.fly","action":"add"}` |
| POST DELETE | `/api/groups/{name}/players/{player}` | 玩家入组 / 离组 |
| GET | `/api/homes/{player}` | 家列表与上限 |
| GET | `/api/market` | 上架列表（含 `available` 表示经济是否可用） |
| DELETE | `/api/market/{id}` | 删除上架 |
| GET | `/api/redeem` | 兑换码列表（含 `expired` 标记） |
| POST | `/api/redeem` | `{"code":"WELCOME","amount":500,"maxUses":50,"duration":"30d"}` |
| POST | `/api/redeem/{code}/toggle` | 启用 / 停用 |
| DELETE | `/api/redeem/{code}` | 删除 |

经济接口在经济系统被关闭时返回 `503`，便于调用方区分「功能已关闭」与「参数错误」。

### 6. 数据落点

| 数据 | 存放位置 |
|------|----------|
| 玩家余额 | `plu_balances`：`uuid / name / balance / updated` |
| 市场挂单 | `plu_market`：`id / seller / seller_name / item / price / created` |
| 兑换码定义 | `plu_redeem_codes`：`code / amount / max_uses / used_count / expires / created / creator / enabled` |
| 兑换记录 | `plu_redeem_uses`：`code / uuid / used_at`（联合主键防重复） |

YAML 模式下分别落在 `balances.yml`、`market.yml`、`redeem.yml`。余额每笔交易即时改内存，MySQL 模式直写数据库，YAML 模式每 60 秒落盘并在关服时强制保存。**切换存储后端不会自动迁移**。

### 7. 运维建议

- **税率**：`tax-percent` 建议设 3~10，作为服务器回收货币的水槽；设为 0 则市场纯流通
- **上架上限**：`max-items-per-player` 建议 5~15，过高会让 GUI 翻页过多、审核困难
- **兑换码**：批量发币时优先用「总次数 + 有效期」双限制，避免链接泄露后被无限领取；活动结束后用 `toggle` 停用而不是删除，保留审计痕迹
- **余额调整**：后台的 `add` / `set` 会直接写入本插件余额表，属于强操作，建议操作前先 `/money <玩家>` 记录原值
- **备份**：余额、市场挂单、兑换码同在本插件的数据表（或 YAML 文件）中，一起备份即可完整还原

---

## 权限组

内置权限组系统用于替代 LuckPerms：支持分组、前缀、权重与权限节点，并在玩家登录时自动注入 Bukkit 权限体系，**无需安装任何权限插件**。

```mermaid
flowchart LR
    G[分组<br/>前缀 · 权重 · 权限节点] -->|玩家入组| P[玩家]
    P -->|登录| A[PermissionAttachment 注入]
    G -->|权限集合| A
    A --> B[Bukkit 权限判定<br/>hasPermission]
    W[Web 后台 / /group] --> G
```

### 规则

- 一个玩家可属于多个组，**权重最高**的组为主分组（决定展示前缀）
- 组权限节点为字符串，前缀 `-` 表示否定节点（如 `-plu.god`）
- 权限通过 `PermissionAttachment` 注入；修改分组或权限会**立即对在线成员重新应用**
- `default` 组不可删除，未显式分组的玩家默认属于 `default`
- 分组名限制：小写字母、数字、下划线、连字符，最长 32 位

### 命令

| 命令 | 权限 | 说明 |
|------|------|------|
| `/group list` | `plu.group` | 列出所有分组（权重、前缀、节点数） |
| `/group info <组名>` | `plu.group` | 查看某分组的全部权限节点 |
| `/group check [玩家]` | `plu.group` | 查看玩家所属分组、主分组、生效权限数 |
| `/group create <组名> [前缀] [权重]` | `plu.group.admin` | 创建分组 |
| `/group delete <组名>` | `plu.group.admin` | 删除分组（default 受保护） |
| `/group prefix <组名> [前缀]` | `plu.group.admin` | 设置前缀，留空清除 |
| `/group weight <组名> <权重>` | `plu.group.admin` | 设置权重 |
| `/group perm <组名> <add\|remove> <节点>` | `plu.group.admin` | 增删权限节点 |
| `/group add <玩家> <组名>` | `plu.group.admin` | 玩家加入分组 |
| `/group remove <玩家> <组名>` | `plu.group.admin` | 玩家移出分组 |

别名：`/groups`、`/rank`。

### 示例

```text
# 创建 VIP 组：金色前缀 + 权重 10
/group create vip &6[VIP] 10
/group perm vip add plu.fly
/group perm vip add plu.home.limit.vip
/group add Steve vip

# 查看结果
/group info vip
/group check Steve
```

### 与 LuckPerms 共存

两者可以同时安装，但会形成两个权限来源：LuckPerms 直接判权，本插件通过权限附件追加。建议只保留一套作为权威来源，避免同一节点在不同处配置不一致。

---

## 充值系统

玩家充值 → 微信支付 → 自动发放游戏货币。默认使用**微信支付 APIv3 JSAPI（公众号支付）**；未配置商户凭据或 `wechatpay.mock: true` 时自动使用**模拟支付**，方便先跑通流程。

### 充值流程

1. 玩家在游戏内执行 `/charge`，获得 **6 位绑定码**（10 分钟有效）和充值页地址。
2. 在微信中打开 `https://你的域名/pay`，输入绑定码。
3. 首次进入会跳转微信网页授权（`snsapi_base`），服务器换取玩家 `openid`。
4. 选择礼包 → 服务端按礼包价格创建 JSAPI 订单 → 微信收银台支付。
5. 微信回调 `/api/pay/notify` → 验签、解密、幂等发货 → 玩家在线时收到到账提示。

```text
游戏内 /charge         微信打开 /pay           微信支付            回调发货
   │                       │                      │                   │
   ├─ 绑定码 + 网址 ──────▶ ├─ 输入绑定码 ────────▶ ├─ 收银台 ─────────▶ ├─ 解密/验签
   │                       ├─ OAuth 获取 openid   │                   ├─ 订单置 PAID
   │                       ├─ 选择礼包/下单       │                   └─ 增加余额
```

### 微信支付配置步骤

#### 0. 前置条件

| 项目 | 要求 |
|------|------|
| 公众号 | **已认证服务号**（订阅号不能开通 JSAPI 支付） |
| 商户号 | 普通商户，已完成主体认证与结算账户 |
| 域名 | 已 ICP 备案，可公网访问，并配置 HTTPS 证书 |
| 服务器 | 有公网 IP，443 端口可访问 |
| 插件 | Web 后台已启动（默认 `0.0.0.0:8080`） |

#### 1. 公众号侧：获取 AppID / AppSecret

1. 登录 [微信公众平台](https://mp.weixin.qq.com)。
2. **设置与开发 → 基本配置**：
   - 记录 **AppID**（形如 `wx1234567890abcdef`）。
   - 生成 / 重置 **AppSecret**（只显示一次，立即保存）。
3. 同一页面的 **IP 白名单**：把服务器公网 IP 加进去（OAuth 换取 openid 时需要）。

#### 2. 公众号侧：配置网页授权域名

1. **设置与开发 → 公众号设置 → 功能设置 → 网页授权域名**。
2. 填写域名，**不要带 `https://`、端口和路径**：

   ```text
   mc.example.com
   ```

3. 下载 `MP_verify_xxxxxx.txt`，确保以下地址能访问到文件内容：

   ```text
   https://mc.example.com/MP_verify_xxxxxx.txt
   ```

4. 点击保存。

提供验证文件有两种方式：

- **Nginx**：单独 `location` 指向文件目录；
- **插件外部目录**：把文件放进某个目录，配置 `web.external-web-dir` 指向该目录，由插件直接提供。

#### 3. 商户平台：绑定 AppID、开通 JSAPI

登录 [微信支付商户平台](https://pay.weixin.qq.com)。

1. **产品中心 → AppID 账号管理 → 关联 AppID**：
   - 填写第 1 步的公众号 AppID；
   - 到公众号后台确认关联；
   - 确认状态为**已生效**。
2. **产品中心 → 我的产品 → JSAPI 支付**：确认已开通。
3. **产品中心 → 开发配置 → JSAPI 支付授权目录**：
   - 推荐填根目录：`https://mc.example.com/`
   - 或填支付页目录：`https://mc.example.com/pay/`
   - 必须是 HTTPS、不能带参数、最多 5 个，且与玩家实际打开的 URL 匹配。

#### 4. 商户平台：mchid、APIv3 密钥、商户证书

**商户号 mchid**：**账户中心 → 商户信息**，记录商户号（形如 `1900000000`）。

**APIv3 密钥**：**账户中心 → API 安全 → APIv3 密钥 → 设置**：

- 必须是 **32 位**字母 + 数字；
- 设置后立即保存，插件里填 `api-v3-key`。

**商户 API 证书**：**账户中心 → API 安全 → API 证书 → 申请证书**：

1. 用「微信支付证书工具」生成证书请求；
2. 下载得到 `apiclient_cert.pem`、`apiclient_key.pem`；
3. 记录**证书序列号**（商户平台页面会显示，也可用命令读取）：

   ```powershell
   openssl x509 -in apiclient_cert.pem -noout -serial
   ```

   输出类似 `serial=3A7B9C...`，配置时**只填等号后的十六进制**，去掉 `serial=`。

> 插件要求私钥为 **PKCS#8 PEM**（文件头 `-----BEGIN PRIVATE KEY-----`）。
> 如果文件头是 `-----BEGIN RSA PRIVATE KEY-----`，先转换：
>
> ```powershell
> openssl pkcs8 -topk8 -inform PEM -in apiclient_key.pem -outform PEM -nocrypt -out apiclient_key_pkcs8.pem
> ```
>
> 然后把 `private-key-path` 指向转换后的文件。

**平台证书 / 微信支付公钥（可选，用于回调验签）**：

- 老商户：**账户中心 → API 安全 → 平台证书**，下载 `wechatpay_xxx.pem`，配置到 `platform-cert-path`。
- 新商户如果只提供「微信支付公钥 + 公钥 ID」，当前版本暂不支持公钥验签，可以先把 `platform-cert-path` 留空：
  - 回调仍会用 APIv3 密钥做 AES-GCM 解密；
  - 但不会校验微信侧签名。

#### 5. 放置凭据文件

把文件放到：

```text
plugins/PluEssentials/apiclient_key.pem
plugins/PluEssentials/wechatpay_platform.pem   # 可选
```

Linux 下收紧权限：

```bash
chmod 600 plugins/PluEssentials/apiclient_key.pem
```

不要提交到 Git，也不要放到 Web 可访问目录。

#### 6. 修改 config.yml

```yaml
wechatpay:
  enabled: true
  mock: false
  app-id: "wx1234567890abcdef"
  app-secret: "公众号 AppSecret"
  mch-id: "1900000000"
  cert-serial-no: "3A7B9C..."            # 不带 serial=
  private-key-path: "apiclient_key.pem"  # 相对插件数据目录
  api-v3-key: "32位APIv3密钥"
  notify-url: "https://mc.example.com/api/pay/notify"
  public-base-url: "https://mc.example.com"
  platform-cert-path: ""                 # 有平台证书就填 wechatpay_platform.pem
  description: "Minecraft 服务器充值"
```

| 配置 | 要求 |
|------|------|
| `notify-url` | 公网 HTTPS、443、**不能带参数**，路径固定 `/api/pay/notify` |
| `public-base-url` | 公网 HTTPS 根地址，**结尾不要带 `/`** |
| `private-key-path` | 相对 `plugins/PluEssentials/`，也可写绝对路径 |
| `cert-serial-no` | 商户 API 证书序列号，不是平台证书序列号 |
| `api-v3-key` | 必须 32 位 |
| `description` | 建议 127 字节以内 |

> 第一次配置或更换证书后请**重启服务器**；`/plureload` 不会清除已缓存的私钥。
> `public-base-url` 留空时会退化为 `http://127.0.0.1:<web.port>`，只能本机测试，微信客户端无法访问。

#### 7. 配置 HTTPS 反向代理

插件的 Web 服务是 HTTP，微信要求 HTTPS，建议用 Nginx 反向代理：

```nginx
server {
    listen 443 ssl;
    server_name mc.example.com;

    ssl_certificate     /etc/nginx/certs/mc.example.com.pem;
    ssl_certificate_key /etc/nginx/certs/mc.example.com.key;

    # 公众号网页授权域名验证文件
    location = /MP_verify_xxxxxx.txt {
        root /var/www/verify;
    }

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-Proto https;
    }
}
```

防火墙只开放 443，8080 不要对公网暴露。

#### 8. 验证流程

1. 重启服务器，控制台确认：
   - `[PluEssentials] Web admin panel running at ...`
   - 没有 `WeChat Pay: 私钥解析失败` 等警告。
2. 在微信中打开 `https://mc.example.com/pay`，页面**不应**出现「模拟支付模式」标记。
3. 游戏内执行 `/charge`，拿到 6 位绑定码。
4. 在充值页输入绑定码 → 选择礼包 → 应跳转微信授权 → 拉起微信收银台。
5. 支付成功后：
   - 页面轮询显示「充值成功」；
   - 控制台出现 `Recharge paid: ...`；
   - 游戏内收到到账提示；
   - 后台「充值管理」订单状态变为 `PAID`。

快速检查回调地址是否可达：

```powershell
curl.exe -i https://mc.example.com/pay
curl.exe -i -X POST https://mc.example.com/api/pay/notify
```

第二个请求返回 400 JSON 属正常（没有签名），说明地址可达。

#### 9. 常见错误对照

| 现象 | 原因 | 处理 |
|------|------|------|
| 充值页仍显示「模拟支付模式」 | `mock: true` 或凭据不完整 | 检查 `enabled`、`app-id`、`app-secret`、`mch-id`、`cert-serial-no`、`api-v3-key`（32 位）、`notify-url`、私钥能否解析 |
| 日志 `私钥解析失败` | 私钥不是 PKCS#8 | 用 `openssl pkcs8` 转换后重新指向 |
| 微信下单返回 401 / 签名错误 | 序列号或私钥不匹配 | `cert-serial-no` 必须是当前私钥对应的商户证书序列号 |
| 网页授权 `redirect_uri 参数错误` | 网页授权域名未配置或不一致 | 公众号后台配置 `mc.example.com`，且 `public-base-url` 使用同一域名 |
| `当前页面 URL 未注册` | JSAPI 支付授权目录不匹配 | 目录填 `https://mc.example.com/` 或 `https://mc.example.com/pay/` |
| 微信内无法打开 `/pay` | 域名未备案 / HTTPS 不通 / 反代失败 | 先用手机浏览器和微信分别访问测试 |
| 支付成功但不到账 | 回调未到达或验签失败 | 检查 `notify-url` 公网可达、HTTPS、443、无参数；后台订单若为 `PENDING` 可人工补单 |
| 回调 400 `签名校验失败` | 平台证书填错 | 换成正确的平台证书，或暂时留空 |
| 回调 400 金额不匹配 | 订单金额与实收不一致 | 检查礼包价格与订单，插件已按服务端礼包定价 |
| OAuth 换取 openid 失败 | AppSecret 错误或 IP 未加白名单 | 重置 AppSecret，加入服务器公网 IP |

#### 10. 上线前安全检查

- `mock: false`，避免玩家自助「模拟支付」白拿货币。
- 私钥、APIv3 密钥只放在服务器本地，权限 600。
- 全站 HTTPS，8080 不对公网开放。
- 配置平台证书，开启回调验签。
- `notify-url` 使用固定域名，不要带参数。
- 定期轮换 APIv3 密钥与商户证书。
- 充值货币是否可退款由运营规则决定，插件不自动退款。

### 充值礼包

礼包保存在 `plugins/PluEssentials/pay-packages.yml`，也可在 Web 后台「充值管理」增删改：

```yaml
packages:
  - id: p100
    name: "100 金币"
    price-fen: 100      # 单位：分，100 = 1.00 元
    coins: 100          # 到账游戏货币
    enabled: true
```

| 字段 | 说明 |
|------|------|
| `id` | 唯一 ID，英文数字 |
| `name` | 展示名称 |
| `price-fen` | 价格（分），受 `recharge.min-fen` / `max-fen` 限制 |
| `coins` | 到账货币数量 |
| `enabled` | 是否上架 |

### 模拟支付

`wechatpay.mock: true` 时不调用微信、不需要 OAuth：玩家输入绑定码 → 选择礼包 → 页面出现「确认支付（测试）」→ 点击即发货。用于验证订单、到账、后台记录。

### 后台与接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/pay` | 充值状态、礼包、最近 200 条订单（管理员） |
| POST / PUT | `/api/pay/packages`、`/api/pay/packages/{id}` | 新增 / 修改礼包 |
| DELETE | `/api/pay/packages/{id}` | 删除礼包 |
| POST | `/api/pay/orders/{no}/confirm` | 人工确认到账（补单） |
| GET | `/api/pay/public/packages` | 公开礼包列表（无需 Token） |
| POST | `/api/pay/public/create` | 创建订单（绑定码 + 礼包） |
| GET | `/api/pay/public/status?order=` | 查询订单状态 |
| POST | `/api/pay/public/mock-confirm` | 模拟支付确认（仅 mock 模式） |
| POST | `/api/pay/notify` | 微信支付回调（无需 Token） |

### 安全说明

- 金额、货币数量全部以**服务端礼包配置**为准，客户端只能传礼包 ID。
- 绑定码一次性使用，10 分钟过期；每次 `/charge` 会作废旧码。
- 订单发货幂等：重复回调不会重复加币。
- 回调金额会与订单金额比对，不一致拒绝发货并记录日志。
- 生产环境务必启用 HTTPS，并配置 `platform-cert-path` 校验回调签名。

---

## 中文昵称与中文 ID

### 中文昵称

昵称只影响**显示层**（聊天、TAB、`{nick}` 占位符），真实登录名不变，原版客户端即可使用。

| 命令 | 权限 | 说明 |
|------|------|------|
| `/nick <昵称>` | `plu.nick` | 设置自己的昵称 |
| `/nick` | `plu.nick` | 查看当前昵称 |
| `/nick reset` | `plu.nick` | 清除自己的昵称 |
| `/nick <玩家> <昵称>` | `plu.nick.others` | 设置他人昵称 |
| `/nick reset <玩家>` | `plu.nick.others` | 清除他人昵称 |

- 昵称长度由 `nickname.max-length` 控制（默认 16 个字符，中文算 1 个）。
- 带 `&` 颜色代码需要 `plu.nick.color`，否则会被自动移除。
- 后台「中文昵称」页可搜索玩家名并设置 / 清除。
- `tab.header` / `tab.footer` 支持 `{nick}` 占位符。

### 真正的中文游戏名

原版 Minecraft 客户端的用户名输入框只允许 `a-z 0-9 _`，**服务端插件无法单方面突破客户端限制**。要使用中文游戏 ID，需要满足以下任一条件：

1. 使用支持中文用户名的改版客户端 / 启动器；或
2. 通过支持中文 ID 的代理端接入（并关闭正版验证）。

本插件能做的是**全链路 UTF-8 兼容**：

- 数据库连接使用 `useUnicode=true&characterEncoding=UTF-8`，玩家名按 UTF-8 存储。
- 命令参数、Web API、`/seen`、封禁、经济、领地等全部按字符串处理，不假设 ASCII。
- `compat.allow-non-ascii-names: true` 时，登录阶段不会主动踢掉非 ASCII 名字。
- 玩家名解析支持“先查本插件记录，再查在线/离线玩家”，中文名也能被 `/pay`、`/ban`、`/nick` 等命令识别。

> 如果你只需要中文显示，**直接用 `/nick` 是最稳妥的方案**，不受客户端与服务端限制。

---

## 自定义菜单

菜单保存在 `plugins/PluEssentials/menus.yml`，也可在 Web 后台「自定义菜单」可视化编辑。

```yaml
menus:
  main:
    title: "&8&l服务器菜单"
    size: 27              # 9-54，必须是 9 的倍数
    items:
      - slot: 11
        material: COMPASS
        name: "&a传送"
        lore:
          - "&7点击打开传送菜单"
        amount: 1
        permission: ""
        commands:
          - "spawn"
        open: ""          # 填写菜单 id 可打开子菜单
        close: false
      - slot: 15
        material: BOOK
        name: "&e服务器规则"
        commands:
          - "rules"
```

| 字段 | 说明 |
|------|------|
| `slot` | 槽位 0-53 |
| `material` | Bukkit 材料名，如 `COMPASS`、`DIAMOND` |
| `name` / `lore` | 显示名与描述，支持 `&` 颜色 |
| `amount` | 物品数量 1-64 |
| `permission` | 为空表示所有人可见 / 可点 |
| `commands` | 点击执行，每行一条；`console:` 前缀以控制台执行 |
| `open` | 点击后打开的菜单 id（可做多级菜单） |
| `close` | 点击后是否关闭界面 |

- 命令支持占位符 `{player}`、`{nick}`。
- 玩家命令：`/menu [id]`（默认 `main`）、`/menu list`。
- 管理员命令：`/menu reload`、`/menu create <id> [size]`、`/menu delete <id>`（权限 `plu.menu.admin`）。

---

## NPC 系统

支持**内置实体 NPC**（零依赖）与 **Citizens 玩家皮肤 NPC**（可选）。

| 类型 | 说明 |
|------|------|
| `villager` | 村民（默认，内置） |
| `armor_stand` | 盔甲架（内置） |
| `zombie` / `skeleton` / `blaze` | 其他内置生物 |
| `player` | 玩家皮肤 NPC，需要安装 Citizens；未安装时回退为村民 |

### 命令（`plu.npc.admin`）

| 命令 | 说明 |
|------|------|
| `/npc create <名称> [类型]` | 在当前位置创建 NPC |
| `/npc remove <id>` | 删除 NPC |
| `/npc list` | 列出全部 NPC |
| `/npc tp <id>` | 传送到 NPC |
| `/npc action <id> <command\|console\|menu\|message> <内容>` | 设置右键动作 |
| `/npc rename <id> <名称>` | 重命名并重生 |
| `/npc skin <id> <正版玩家名>` | 设置皮肤（仅 Citizens） |

### 动作

- `command`：以玩家身份执行命令，支持 `{player}`、`{nick}`。
- `console`：以控制台身份执行命令。
- `menu`：打开自定义菜单，内容填菜单 id。
- `message`：直接发送消息，支持 `&` 颜色。

NPC 数据保存在 `plugins/PluEssentials/npcs.yml`（或 MySQL `plu_npcs`）；插件关闭时会移除实体，启动时按记录重新生成，避免重复。后台「NPC 管理」页可创建、改动作、删除，并显示 Citizens 是否已安装。

---

## 命令告示牌

把右键告示牌变成命令按钮：`/cmdsign`（权限 `plu.cmdsign.admin`）。

| 命令 | 说明 |
|------|------|
| `/cmdsign add <命令...>` | 看着告示牌（6 格内）创建 |
| `/cmdsign remove` | 移除看着的命令告示牌 |
| `/cmdsign perm <权限节点\|none>` | 设置 / 清除使用权限 |
| `/cmdsign info` | 查看看着的告示牌配置 |
| `/cmdsign list` | 列出全部命令告示牌 |

- 命令支持 `{player}`、`{nick}` 占位符。
- `console:` 前缀表示以控制台身份执行，例如 `/cmdsign add console:say 欢迎 {player}`。
- `cmdsigns.write-label: true` 时，创建后会把告示牌前三行改写为 `[命令] /<命令> 右键执行`。
- 未设置权限时所有人可用；设置后需要对应权限节点。
- 数据保存在 `signs.yml`（或 MySQL `plu_signs`），后台「命令告示牌」页可查看 / 添加 / 删除。

---

## 命令

> 约定：`<参数>` 必填，`[参数]` 可选；消息支持 `&` 颜色代码。
> 详细用法与权限见下方折叠区块。

<details open>
<summary><b>玩家命令</b></summary>

| 命令 | 权限 | 说明 |
|------|------|------|
| `/homes` | `plu.homes` | 列出自己的家 |
| `/home [name]` | `plu.home` | 传送到家，默认 `home` |
| `/sethome [name]` | `plu.sethome` | 设置家 |
| `/delhome <name>` | `plu.delhome` | 删除家 |
| `/tpa <player>` | `plu.tpa` | 请求传送到目标玩家 |
| `/tpaccept` / `/tpdeny` | `plu.tpa` | 接受 / 拒绝请求 |
| `/back` | `plu.back` | 回到死亡地点 |
| `/kit [name]` | `plu.kit` | 领取礼包，省略则列出 |
| `/spawn` | `plu.spawn` | 传送到出生点 |
| `/warp [name]` | `plu.warp` | 传送到地标，省略则列出 |
| `/money [player]` | `plu.money` / `plu.money.others` | 查询余额（别名 `/bal`） |
| `/pay <player> <金额>` | `plu.pay` | 转账 |
| `/baltop [数量]` | `plu.baltop` | 财富排行（默认 10，缓存 5 分钟） |
| `/market` | `plu.market` | 打开市场 GUI（别名 `/ah`） |
| `/market sell <价格>` | `plu.market` | 上架手持物品 |
| `/market my` | `plu.market` | 我的上架 |
| `/market remove <编号>` | `plu.market` | 下架并取回 |
| `/claim create [半径]` | `plu.claim` | 创建领地（默认半径 5） |
| `/claim list` | `plu.claim` | 我的领地 |
| `/claim remove <编号>` | `plu.claim` | 删除领地 |
| `/claim info` | `plu.claim` | 查看所在位置归属 |
| `/news` | `plu.news` | 公告（书本界面，别名 `/announcements`） |
| `/rules` | `plu.rules` | 规则（书本界面） |
| `/register <密码>` | — | 注册（4-32 位，成功自动登录） |
| `/login <密码>` | — | 登录 |
| `/redeem <兑换码>` | `plu.redeem` | 兑换货币（别名 `/cdkey`） |
| `/nick <昵称>` | `plu.nick` | 设置中文昵称；`/nick reset` 清除 |
| `/charge` | `plu.charge` | 获取充值绑定码；`/charge list` 查看礼包 |
| `/menu [id]` | `plu.menu` | 打开自定义菜单；`/menu list` 列出菜单 |

</details>

<details>
<summary><b>管理员命令</b></summary>

| 命令 | 别名 | 权限 | 说明 |
|------|------|------|------|
| `/fly [player]` | — | `plu.fly` | 切换飞行 |
| `/gamemode <mode> [player]` | `/gm` | `plu.gamemode` | survival / creative / adventure / spectator 或 0-3 |
| `/heal [player]` | — | `plu.heal` | 满血满食灭火 |
| `/feed [player]` | — | `plu.feed` | 满食满饱和 |
| `/god [player]` | — | `plu.god` | 切换无敌 |
| `/vanish [player]` | `/v` | `plu.vanish` | 切换隐身 |
| `/speed <1-10> [player]` | — | `plu.speed` | 设置行走 / 飞行速度 |
| `/clear [player]` | `/ci` | `plu.clear` | 清空背包 |
| `/invsee <player>` | — | `plu.invsee` | 查看并编辑背包（会话锁） |
| `/enderchest [player]` | `/ec` | `plu.enderchest` | 查看末影箱 |
| `/ban <player> [reason]` | — | `plu.ban` | 永久封禁 |
| `/tempban <player> <duration> [reason]` | — | `plu.ban` | 限时封禁 |
| `/unban <player>` | — | `plu.ban` | 解封 |
| `/banlist` | — | `plu.ban` | 封禁列表 |
| `/ipban <ip\|player> [reason]` | — | `plu.ipban` | IP 封禁（传在线玩家名自动取 IP） |
| `/ipunban <ip>` | — | `plu.ipban` | 解封 IP |
| `/ipbanlist` | — | `plu.ipban` | IP 封禁列表 |
| `/whitelist <on\|off\|add\|remove\|list> [player]` | `/wl` | `plu.whitelist` | 白名单管理 |
| `/chatlog [count\|keyword]` | — | `plu.chatlog` | 聊天记录：数字=条数，文本=搜索 |
| `/seen <player>` | — | `plu.seen` | 首次登录 / 最后在线 / 累计时长 |
| `/sudo <player> <command>` | — | `plu.sudo` | 强制玩家执行命令 |
| `/healall` / `/feedall` | — | `plu.healall` / `plu.feedall` | 群体治疗 / 喂食 |
| `/broadcast <message>` | `/bc` | `plu.broadcast` | 全服广播 |
| `/setspawn` | — | `plu.setspawn` | 设置出生点 |
| `/setwarp <name>` | — | `plu.setwarp` | 创建地标 |
| `/delwarp <name>` | — | `plu.delwarp` | 删除地标 |
| `/plureload` | — | `plu.reload` | 重载配置与全部管理器 |
| `/group <list\|info\|create\|delete\|prefix\|weight\|perm\|add\|remove\|check>` | `/rank` | `plu.group` / `plu.group.admin` | 内置权限组管理 |
| `/redeem create <兑换码\|#> <金额> [次数] [时长]` | — | `plu.redeem.admin` | 创建兑换码 |
| `/redeem list` / `toggle` / `delete` | — | `plu.redeem.admin` | 兑换码管理 |
| `/npc create <名称> [类型]` | — | `plu.npc.admin` | 在当前位置创建 NPC |
| `/npc remove <id>` / `list` / `tp <id>` | — | `plu.npc.admin` | NPC 删除 / 列表 / 传送 |
| `/npc action <id> <command\|console\|menu\|message> <内容>` | — | `plu.npc.admin` | 设置 NPC 点击动作 |
| `/npc rename <id> <名称>` | — | `plu.npc.admin` | 重命名 |
| `/npc skin <id> <玩家名>` | — | `plu.npc.admin` | 设置皮肤（Citizens） |
| `/cmdsign add <命令...>` | `/commandsign` | `plu.cmdsign.admin` | 看着告示牌创建命令牌 |
| `/cmdsign remove` / `info` / `list` | `/commandsign` | `plu.cmdsign.admin` | 命令告示牌管理 |
| `/cmdsign perm <节点\|none>` | `/commandsign` | `plu.cmdsign.admin` | 设置告示牌使用权限 |
| `/menu reload` / `create <id> [size]` / `delete <id>` | — | `plu.menu.admin` | 菜单维护 |
| `/nick <玩家> <昵称>` / `nick reset <玩家>` | — | `plu.nick.others` | 管理他人昵称 |

</details>

### 时长格式

`/tempban`、`/redeem` 与 Web API 通用：`数字 + 单位`

| 后缀 | 单位 | 示例 |
|------|------|------|
| `s` | 秒 | `30s` |
| `m` | 分钟 | `15m` |
| `h` | 小时 | `2h` |
| `d` | 天 | `7d` |
| `w` | 周 | `1w` |

`0` 或留空 = 永久。

### 常用示例

```text
# 经济与市场
/pay Steve 100
/market sell 500            # 手持物品以 500 上架
/claim create 8             # 以自己为中心 17x17 领地

# 兑换码：500 货币，限前 50 人，30 天有效
/redeem create WELCOME2026 500 50 30d
/redeem create # 100 1 7d   # 随机一次性码

# 安全
/tempban Steve 1d 使用外挂
/ipban BadGuy 恶意刷屏
/whitelist add Alex
/whitelist on

# 审计
/chatlog 30
/seen Steve

# 昵称与充值
/nick 小明
/nick reset
/charge

# 菜单 / NPC / 告示牌
/menu main
/npc create 商店老板 player
/npc action 3f8a2c1d menu shop
/cmdsign add console:say 欢迎 {player} 来到服务器
/cmdsign perm plu.cmdsign.use
```

### 权限节点

<details>
<summary>展开完整权限表</summary>

| 权限 | 默认 | 用途 |
|------|------|------|
| `plu.home` `plu.sethome` `plu.delhome` `plu.homes` | 所有人 | 家系统 |
| `plu.tpa` `plu.back` `plu.kit` `plu.spawn` `plu.warp` | 所有人 | 基础玩家功能 |
| `plu.home.limit.<名称>` | — | 家数量上限（配合 `home.limits`） |
| `plu.money` `plu.pay` `plu.baltop` | 所有人 | 经济 |
| `plu.money.others` | OP | 查看他人余额 |
| `plu.market` | 所有人 | 玩家市场 |
| `plu.market.admin` | OP | 删除任意上架 |
| `plu.claim` | 所有人 | 领地 |
| `plu.claim.bypass` | OP | 无视领地保护 |
| `plu.claim.admin` | OP | 删除任意领地 |
| `plu.rules` `plu.news` | 所有人 | 公告与规则 |
| `plu.redeem` | 所有人 | 兑换码兑换 |
| `plu.redeem.admin` | OP | 管理兑换码 |
| `plu.group` | 所有人 | 查看权限组 |
| `plu.group.admin` | OP | 创建 / 删除 / 改权限 / 分配玩家 |
| `plu.login.bypass` | OP | 跳过登录要求 |
| `plu.setspawn` `plu.setwarp` `plu.delwarp` | OP | 维护类 |
| `plu.fly` `plu.gamemode` `plu.heal` `plu.feed` `plu.god` `plu.vanish` `plu.speed` `plu.clear` | OP | 玩家状态 |
| `plu.invsee` `plu.enderchest` | OP | 背包查看 |
| `plu.ban` `plu.ipban` `plu.whitelist` | OP | 安全管控 |
| `plu.chatlog` `plu.seen` | OP | 审计统计 |
| `plu.sudo` `plu.healall` `plu.feedall` | OP | 强制与群体操作 |
| `plu.broadcast` `plu.reload` | OP | 消息与维护 |
| `plu.nick` | 所有人 | 设置自己的昵称 |
| `plu.nick.others` | OP | 设置他人昵称 |
| `plu.nick.color` | OP | 昵称使用颜色代码 |
| `plu.menu` | 所有人 | 打开自定义菜单 |
| `plu.menu.admin` | OP | 创建 / 删除 / 重载菜单 |
| `plu.charge` | 所有人 | 获取充值绑定码 |
| `plu.pay.admin` | OP | 充值礼包与订单管理（后台） |
| `plu.npc.admin` | OP | NPC 创建 / 删除 / 动作 / 皮肤 |
| `plu.cmdsign.admin` | OP | 命令告示牌管理 |
| `plu.cmdsign.use` | 所有人 | 命令告示牌的通用权限示例 |

</details>

---

## Web 管理后台

插件内嵌 `com.sun.net.httpserver.HttpServer`，前端为 Vue 3 + Element Plus 构建产物，随 jar 一起分发。

| 项目 | 说明 |
|------|------|
| 默认地址 | `http://<服务器IP>:8080/` |
| 登录方式 | 密码登录，换取 Bearer Token |
| Token 有效期 | `web.token-expiry` 分钟（默认 60） |
| 前端来源 | jar 内 `web/` 资源；`web.external-web-dir` 可指向外部目录做热更新 |

**页面清单**：仪表盘、玩家管理、控制台、插件管理、世界管理、经济系统、玩家市场、领地管理、兑换码、充值管理、中文昵称、自定义菜单、NPC 管理、命令告示牌、公告与规则、登录账号、封禁管理、IP 封禁、白名单、聊天记录、Warp 管理、定时任务、服务器控制。

---

## REST API

所有接口返回 UTF-8 JSON。除登录外需携带请求头：

```http
Authorization: Bearer <token>
```

错误统一为 `{ "error": "描述" }`，状态码 `400` 参数错误 / `401` 未授权 / `404` 不存在 / `503` 依赖不可用。

<details open>
<summary><b>认证与仪表盘</b></summary>

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/login` | `{"password":"..."}` → `{"token":"..."}`，失败 401 |
| POST | `/api/logout` | 吊销 Token |
| GET | `/api/dashboard` | TPS、内存、在线人数、`vaultEnabled` / `vaultInstalled` / `luckpermsEnabled` |

</details>

<details>
<summary><b>玩家 / 控制台 / 插件 / 世界</b></summary>

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/players` | 在线玩家列表（含血量、模式、IP、延迟、余额） |
| POST | `/api/players/{name}/{action}` | `kick` `heal` `feed` `kill` `god` `fly` `clear` `gamemode` `message` `teleport` `inventory` `ban` |
| GET | `/api/console?lines=200` | 控制台日志 |
| POST | `/api/console` | `{"command":"..."}` |
| GET | `/api/plugins` | 插件列表 |
| POST | `/api/plugins/{name}/{action}` | `enable` `disable` `reload` |
| GET | `/api/worlds` | 世界列表（时间、天气、难度、种子） |
| POST | `/api/worlds/{name}/{action}` | `time` `day` `night` `weather` `difficulty` |

</details>

<details>
<summary><b>经济 / 权限 / 家 / 市场 / 领地</b></summary>

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/economy/{player}` | 余额与货币名（内置经济） |
| POST | `/api/economy/{player}/{set\|add\|remove}` | `{"amount":100}` |
| GET | `/api/permissions/{player}` | 主组与组列表（内置权限组） |
| GET | `/api/homes/{player}` | 家列表与上限 |
| GET | `/api/market` | 上架列表 |
| DELETE | `/api/market/{id}` | 删除上架 |
| GET | `/api/claims` | 领地列表（范围、面积） |
| DELETE | `/api/claims/{id}` | 删除领地 |

</details>

<details>
<summary><b>公告 / 账号 / 兑换码</b></summary>

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/announcements` | 公告与规则内容 |
| POST | `/api/announcements/{news\|rules}` | `{"pages":["第一页","第二页"]}` |
| GET | `/api/accounts` | 已注册账号与登录状态 |
| GET | `/api/redeem` | 兑换码列表 |
| POST | `/api/redeem` | `{"code":"WELCOME","amount":500,"maxUses":50,"duration":"30d"}` |
| POST | `/api/redeem/{code}/toggle` | 启用 / 停用 |
| DELETE | `/api/redeem/{code}` | 删除 |

</details>

<details>
<summary><b>充值 / 昵称 / 菜单 / NPC / 告示牌</b></summary>

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/pay` | 充值状态、礼包、最近订单 |
| POST PUT DELETE | `/api/pay/packages`、`/api/pay/packages/{id}` | 礼包维护 |
| POST | `/api/pay/orders/{no}/confirm` | 人工确认到账 |
| GET POST | `/api/nicknames` | 昵称列表 / 设置 `{"name":"玩家名","nick":"昵称"}` |
| DELETE | `/api/nicknames/{uuid}` | 清除昵称 |
| GET POST PUT DELETE | `/api/menus`、`/api/menus/{id}` | 菜单维护（含 items 数组） |
| GET POST DELETE | `/api/npcs`、`/api/npcs/{id}` | NPC 列表 / 创建 / 更新 / 删除 |
| GET POST DELETE | `/api/signs`、`/api/signs/{world}/{x}/{y}/{z}` | 命令告示牌维护 |

</details>

<details>
<summary><b>封禁 / 白名单 / 审计 / Warp / 任务 / 服务器</b></summary>

| 方法 | 路径 | 说明 |
|------|------|------|
| GET POST DELETE | `/api/bans`、`/api/bans/{uuid}` | UUID 封禁 |
| GET POST DELETE | `/api/ipbans`、`/api/ipbans/{ip}` | IP 封禁 |
| GET POST DELETE | `/api/whitelist`、`/api/whitelist/{uuidOrName}` | 白名单（路径支持名字或 UUID） |
| GET | `/api/chat?limit=50&player=Steve` | 聊天记录 |
| GET | `/api/stats/{player}` | 玩家统计 |
| GET POST DELETE | `/api/warps`、`/api/warps/{name}` | 地标 |
| GET POST PUT DELETE | `/api/tasks`、`/api/tasks/{id}` | 定时任务（Cron） |
| POST | `/api/tasks/{id}/run` | 立即执行 |
| POST | `/api/broadcast` | `{"message":"..."}` |
| POST | `/api/server/stop` | `{"delay":5}` |
| POST | `/api/server/reload` | `Bukkit.reload()` |

</details>

---

## 数据存储

由 `database.enabled` 切换，代码层对两种后端透明：

| 后端 | 位置 |
|------|------|
| YAML | `plugins/PluEssentials/*.yml`（按模块拆分） |
| MySQL | `plu_*` 表，启动时自动建表，前缀可配 |

<details>
<summary>展开数据表结构</summary>

| 表 | 字段 | 说明 |
|----|------|------|
| `plu_bans` | uuid, name, reason, banner, created, expires | UUID 封禁 |
| `plu_ipbans` | ip, reason, banner, created, expires | IP 封禁 |
| `plu_whitelist` | uuid, name | 白名单 |
| `plu_homes` | uuid, name, world, x, y, z, yaw, pitch | 家（联合主键） |
| `plu_warps` | name, world, x, y, z, yaw, pitch | 地标 |
| `plu_tasks` | id, name, command, cron, enabled, last_run | 定时任务 |
| `plu_chatlog` | id, time, name, uuid, world, message | 聊天记录 |
| `plu_playerstats` | uuid, name, first_join, last_seen, playtime | 玩家统计 |
| `plu_market` | id, seller, seller_name, item, price, created | 市场上架（物品 Base64） |
| `plu_claims` | id, owner, owner_name, world, min_x, min_z, max_x, max_z, created | 领地 |
| `plu_accounts` | uuid, name, salt, hash, registered | 登录账号（加盐哈希） |
| `plu_redeem_codes` | code, amount, max_uses, used_count, expires, created, creator, enabled | 兑换码 |
| `plu_redeem_uses` | code, uuid, used_at | 兑换记录（防重复） |
| `plu_balances` | uuid, name, balance, updated | 内置经济余额 |
| `plu_groups` | name, prefix, weight, permissions | 权限组定义（权限为逗号分隔） |
| `plu_player_groups` | uuid, group_name | 玩家与分组的关联 |
| `plu_pay_orders` | no, uuid, player, package_id, amount_fen, coins, status, created, paid_at, openid, prepay_id, transaction_id | 充值订单 |
| `plu_nicknames` | uuid, name, nick, updated | 中文昵称 |
| `plu_npcs` | id, name, type, world, x, y, z, yaw, pitch, action_type, action_value, skin, citizens, citizens_id, entity_uuid | NPC |
| `plu_signs` | world, x, y, z, command, permission, creator, created | 命令告示牌 |

</details>

---

## 从源码构建

### 环境

- JDK 21
- Maven 3.9+
- Node.js 20+（仅前端构建需要）

### 步骤

```bash
# 1. 构建前端
cd web-frontend
npm install
npm run build

# 2. 将前端产物复制到插件资源（Windows PowerShell）
Remove-Item -Recurse -Force src/main/resources/web
Copy-Item -Recurse web-frontend/dist src/main/resources/web

# 3. 构建插件
cd ..
mvn clean package
```

产物：`target/PluEssentials-3.0.0.jar`

> 后端编译启用了 `-Xlint:all,-classfile`，构建输出 0 警告。
> MySQL 驱动与 Vue 资源由 `maven-shade-plugin` 打入 jar，最终约 5.4 MB。

## 项目结构

```text
plu/
├── pom.xml                     # Maven 配置（shade 打包 MySQL 驱动）
├── src/main/
│   ├── java/com/plu/plugin/
│   │   ├── Main.java           # 主类：生命周期、命令注册、管理器装配
│   │   ├── commands/
│   │   │   ├── user/           # 玩家命令
│   │   │   └── admin/          # 管理员命令
│   │   ├── gui/                # 市场 GUI、书本界面
│   │   ├── listeners/          # 登录检查、聊天审计、领地保护、市场交互
│   │   ├── managers/           # 各业务管理器（MySQL / YAML 双后端）
│   │   ├── pay/                # 微信支付 APIv3 客户端（JSAPI / 回调验签解密）
│   │   ├── storage/            # DatabaseManager：连接与自动建表
│   │   ├── util/               # 玩家解析、数值转换工具
│   │   └── web/                # HttpServer、路由、鉴权、公开充值页与回调
│   └── resources/
│       ├── plugin.yml          # 命令与权限声明
│       ├── config.yml          # 默认配置
│       ├── pay/index.html      # 玩家充值页（无需构建）
│       └── web/                # 前端构建产物（打入 jar）
└── web-frontend/               # Vue 3 + Vite + Element Plus 源码
```

### 架构要点

- **无外部 Web 框架**：仅用 JDK 内置 HttpServer，单 jar 交付；请求线程中修改 Bukkit 状态的操作通过 `Bukkit.getScheduler().runTask` 回到主线程。
- **双存储后端**：管理器统一构造签名 `XxxManager(Main plugin, DatabaseManager db)`，内部按 `db.isEnabled()` 分流。
- **无硬依赖**：Citizens 仅在存在时通过反射创建玩家皮肤 NPC；未安装则回退内置实体。微信支付仅使用 JDK 自带 `HttpClient` 与 JCA。
- **前端可热更新**：`web.external-web-dir` 指向外部目录即可绕过 jar 内资源。
- **公开支付与后台隔离**：`/pay`、`/api/pay/public/*`、`/api/pay/notify` 在鉴权路由之前处理，管理员 Token 不参与支付回调。

---

## 常见问题

<details>
<summary><b>经济系统提示「已关闭」</b></summary>

检查 `config.yml` 的 `economy.enabled` 是否为 `true`，修改后执行 `/plureload`。关闭状态下市场、兑换码、排行会一并停用。
</details>

<details>
<summary><b>市场打不开，提示经济系统已关闭</b></summary>

同上，市场依赖内置经济做扣款与结算，请确认 `economy.enabled: true`。
</details>

<details>
<summary><b><code>/baltop</code> 为空或数据不全</b></summary>

排行只统计**余额大于 0** 的玩家并缓存 5 分钟。刚调整过余额需要等缓存过期，或执行 `/plureload` 刷新；玩家量大时首次统计会有短暂延迟。
</details>

<details>
<summary><b>市场成交后卖家没收到钱</b></summary>

款项直接写入内置经济（卖家离线也会入账），先确认「经济系统」页余额是否正确。若开启了 `market.tax-percent`，卖家实际到账为「价格 − 税额」，税额部分直接销毁不上缴，可用于回收货币。
</details>

<details>
<summary><b>兑换码提示「你已经兑换过这个码了」</b></summary>

同一玩家对同一个码只能兑换一次，这是独立于总次数的防刷机制。需要给同一玩家重复发币时，请新建一个兑换码。
</details>

<details>
<summary><b>从旧版（Vault 经济）升级后余额变成 0</b></summary>

旧余额存在原来那套经济插件里，本插件不会自动读取。可在后台「经济系统」页按玩家名 `set` 目标金额，或用 `/money` 记录旧值后逐个迁移。
</details>

<details>
<summary><b>权限组改了但玩家仍是旧权限</b></summary>

在线玩家会在改动后立即重新应用；离线玩家需下次登录生效。若同时安装了 LuckPerms，注意不要在两处配置同一节点。
</details>

<details>
<summary><b>领地创建失败提示重叠</b></summary>

领地是二维矩形（不限高度），与已有领地范围相交即失败。换个位置或先删除已有领地。
</details>

<details>
<summary><b>玩家登录后仍被限制移动</b></summary>

确认已执行 `/register` 或 `/login`；`login.enabled: false` 可整体关闭登录要求，`plu.login.bypass` 权限可跳过。
</details>

<details>
<summary><b>充值页一直显示「模拟支付模式」</b></summary>

说明 `wechatpay.mock: true`，或商户凭据不完整（`isConfigured()` 返回 false）。检查 `enabled`、`app-id`、`mch-id`、`cert-serial-no`、`api-v3-key`（必须 32 位）、`notify-url`、`private-key-path` 是否齐全，且私钥为 PKCS#8 PEM。改完执行 `/plureload`。
</details>

<details>
<summary><b>微信支付调起失败 / 充值页无法访问</b></summary>

JSAPI 支付只能在微信内打开。确认 `public-base-url` 是可公网访问的 HTTPS 域名，且该域名已配置为公众号网页授权域名与 JSAPI 支付授权目录；`notify-url` 必须能被微信回调到。
</details>

<details>
<summary><b>玩家支付成功但没到账</b></summary>

先看后台「充值管理」订单状态：`PENDING` 可在确认收款后点「确认到账」补单。若订单一直是 `PENDING`，检查微信回调是否到达（`notify-url`、HTTPS、防火墙），以及日志中的验签 / 金额不匹配提示。
</details>

<details>
<summary><b>设置中文游戏名后无法进入服务器</b></summary>

原版客户端用户名只允许 `a-z0-9_`。插件无法绕过客户端限制，只能用改版客户端或支持中文 ID 的代理；若只是想要中文显示，请使用 `/nick`（[中文昵称与中文 ID](#中文昵称与中文-id)）。
</details>

<details>
<summary><b>昵称设置成功但聊天里没变化</b></summary>

本插件会设置 `displayName` 与 TAB 名称；若有其他聊天格式插件接管了 `AsyncPlayerChatEvent` 并固定使用真实名，需要在其格式里改用显示名或 `{nick}`。TAB 每 5 秒刷新一次。
</details>

<details>
<summary><b>NPC 没有玩家皮肤 / 无法创建 player 类型</b></summary>

玩家皮肤 NPC 需要安装 Citizens（`player` 类型会在 Citizens 存在时自动使用）。未安装时会回退成村民，并在控制台给出提示。
</details>

<details>
<summary><b>命令告示牌右键没反应</b></summary>

确认告示牌已用 `/cmdsign add` 创建（后台列表能看到）；若设置了权限节点，玩家需要拥有该节点；`/cmdsign remove` 与 `perm` 都需要先看着目标告示牌。
</details>

<details>
<summary><b>修改配置后不生效</b></summary>

执行 `/plureload`，或在后台「服务器控制」重载插件。
</details>

<details>
<summary><b>切换 MySQL 后旧数据丢失</b></summary>

两种后端不自动迁移，切换前请先备份 YAML 并手动导入。
</details>

---

## 作者

<table>
<tr><td><b>作者</b></td><td>stonestsy</td></tr>
<tr><td><b>开发者</b></td><td>stonestsy</td></tr>
<tr><td><b>仓库维护者</b></td><td>stonestsy</td></tr>
</table>

欢迎提交 Issue 与 Pull Request。反馈问题时请附上：服务端类型与版本、Java 版本、插件版本、完整报错堆栈。

## 许可

本项目基于 [MIT License](LICENSE) 开源，Copyright (c) 2026 stonestsy。

你可以自由使用、修改、分发与商用，只需保留原始版权声明与许可声明。

---

<div align="center">
<sub>Built with Java 21 · Bukkit API 1.21 · Vue 3</sub>
</div>
