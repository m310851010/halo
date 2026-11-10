# Halo 本地部署使用手册

> 面向日常使用与运维（不是开发者）。系统要求：Windows 10/11，已装 JDK 17。
> 开发者接手请看 [HANDOVER.md](./HANDOVER.md)。

---

## 目录

1. [系统组成一览](#1-系统组成一览)
2. [快速启动（每天开机的三步）](#2-快速启动)
3. [首次部署（从零开始）](#3-首次部署)
4. [后台管理入门](#4-后台管理入门)
5. [写文章与分类/标签](#5-写文章与分类标签)
6. [主题管理](#6-主题管理)
7. [数据备份与恢复](#7-数据备份与恢复)
8. [停止与重启](#8-停止与重启)
9. [重新打包升级](#9-重新打包升级)
10. [常见问题排查（FAQ）](#10-常见问题排查faq)

---

## 1. 系统组成一览

| 组件 | 位置 | 端口 | 作用 |
|---|---|---|---|
| MariaDB | `D:\mariadb\` | 3306 | 数据库（账号 root / 密码 mariadb，库 halo） |
| Halo | `H:\aiWork\halo\application\target\halo-2.28.0-SNAPSHOT.jar` | 8090 | 博客本体（前台 + 管理后台） |
| 数据目录 | `H:\halo2-maven-test\` | — | 附件、主题、日志、插件（**备份重点**） |
| 打包脚本 | `H:\aiWork\halo\build-jar.bat` / `build-war.bat` | — | 重新生成 jar/war |

访问地址：

- **前台（博客首页）**：<http://localhost:8090>
- **管理后台 Console**：<http://localhost:8090/console>
- **用户中心 UC**：<http://localhost:8090/uc>
- 健康检查：<http://localhost:8090/actuator/health>

登录账号：`admin / Admin123!`（可在 Console → 用户里修改密码）。

**重要概念**：前台样式由"主题"控制（当前启用 Bootstrap 3 主题）；后台 Console 是独立的 Vue 界面，换主题不影响后台。

---

## 2. 快速启动

每天（或重启电脑后）按顺序做两步：

**第 1 步：启动数据库**

双击 `D:\mariadb\start.bat`。弹出黑色窗口显示 MariaDB 启动日志——**这个窗口要保持开着**（关掉=数据库停止）。看到 `ready for connections` 字样即成功。

> 窗口嫌碍事可以把命令注册成 Windows 服务或用 `--console` 去掉后台化，见 FAQ 第 8 条。

**第 2 步：启动 Halo**

打开命令行（cmd 或 PowerShell），执行：

```bat
java -Djava.io.tmpdir=H:/halo2-maven-test/tmp -jar H:\aiWork\halo\application\target\halo-2.28.0-SNAPSHOT.jar --spring.profiles.active=mariadb --halo.work-dir=H:/halo2-maven-test --server.port=8090
```

等待约 15~20 秒，看到 `Started Application in X seconds` 即成功。浏览器打开 <http://localhost:8090> 验证。

> 也可以把这个命令存成 `start-halo.bat`（建议放 `H:\aiWork\halo\`），以后双击。窗口同样要保持开着。

**验证**：浏览器打开 <http://localhost:8090/actuator/health>，看到 `{"status":"UP"}` 即一切正常。

---

## 3. 首次部署（从零开始）

如果接手的是一台全新的电脑（没有上面第 1 节的任何东西）：

1. **装 JDK 17**（必须是 17，不是 21——本项目按 17 降级改造过）。验证：`java -version`。
2. **装 Maven 3.8+**（只在需要重新打包时才要）。验证：`mvn -v`。
3. **拷贝三样东西**：源码仓库 `H:\aiWork\halo`、MariaDB 目录 `D:\mariadb`、数据目录 `H:\halo2-maven-test`。
4. 按"快速启动"两步走。数据库如果没拷到 data 目录，`start.bat` 会自动初始化一个新的（密码仍是 mariadb）。
5. 如果连 jar 都没有：在源码目录跑 `build-jar.bat`（详见第 9 节）。

---

## 4. 后台管理入门

1. 打开 <http://localhost:8090/console>，输入 `admin / Admin123!` 登录。
2. 登录后左侧就是全部管理功能：

| 功能 | 位置 | 说明 |
|---|---|---|
| 写文章 | 文章 → 新建 | Markdown 编辑器，右上角"发布" |
| 管理评论 | 评论 | 审核/删除访客留言 |
| 上传附件 | 附件 | 图片等静态资源，文章里引用 |
| 外观/主题 | 外观 → 主题 | 见第 6 节 |
| 插件 | 插件 | ⚠️ 当前环境插件不可用（见 FAQ 第 5 条） |
| 站点设置 | 系统 → 设置 | 站点标题、SEO、注册开关等 |

3. **用户中心 UC**（<http://localhost:8090/uc>）是给非管理员用户的个人中心，改自己资料、看自己的文章。

---

## 5. 写文章与分类标签

1. Console → 文章 → 新建。
2. 填标题、写 Markdown 正文。
3. 右侧面板设置：**分类**（必选一个）、**标签**（可多个）、**别名 slug**（URL 路径）、封面图。
4. 点"发布"。前台 <http://localhost:8090> 首页立即出现这篇文章。
5. 文章访问地址形如 `http://localhost:8090/archives/<slug>`。

删除文章：文章列表 → 勾选 → 移到回收站 → 回收站里彻底删除。

---

## 6. 主题管理

当前内置了两个主题：`theme-earth`（官方自带）和 **`theme-bootstrap3`**（本项目制作，当前启用）。

**切换主题**：Console → 外观 → 主题 → 点击主题卡片上的"启用"。

**安装新主题**：Console → 外观 → 主题 → 右上角"安装"→ 上传 zip。
现成的安装包：`D:\tmp\theme-bootstrap3.zip`（就是 bootstrap3 主题本体）。
注意：上传的 zip 必须用 jar 或 7-Zip 打包（PowerShell 打包的会安装失败，见 FAQ 第 6 条）。

**修改主题外观设置**：Console → 外观 → 主题 → 主题卡片上的"设置"按钮。Bootstrap 3 主题提供三项：页脚文字（支持 HTML）、是否显示评论区、导航栏深/浅色。

**主题主题定制开发**（改模板/CSS/加页面）：看仓库里 `docs/theme-bootstrap3-customization.md`，含模板变量速查、11 个定制示例、8 条踩坑记录。

主题源码位置：仓库根目录 `theme-bootstrap3\`（模板 + 静态资源）；运行时副本在 `H:\halo2-maven-test\themes\theme-bootstrap3\`。改运行时副本里的模板后，到 Console 主题页点"重载配置"或调 invalidate-cache 接口即可生效（无需重启）。

---

## 7. 数据备份与恢复

**所有业务数据在两处**，都要备：

1. **数据库**（文章/评论/用户/设置都在这）：

```bat
:: 备份（导出）
D:\mariadb\bin\mariadb-dump.exe -u root -pmariadb halo > H:\backup\halo-%date:~0,10%.sql

:: 恢复（导入）
D:\mariadb\bin\mariadb.exe -u root -pmariadb halo < H:\backup\halo-某个日期.sql
```

2. **数据目录** `H:\halo2-maven-test\`：直接整目录拷贝（含上传的附件、主题、插件）。可排除 `logs\` 和 `tmp\`。

**建议**：每周备份一次数据库 + 附件；做大改版（升级、换主题、改配置）前必备份。

**灾难恢复演练**（换机部署）：装好 JDK → 拷贝源码/mariadb/数据目录 → start.bat 起数据库 → java -jar 起 Halo，完成。

---

## 8. 停止与重启

| 操作 | 做法 |
|---|---|
| 停 Halo | 关掉运行 java 命令的那个命令行窗口；或 `taskkill /F /IM java.exe`（会连别的 java 一起杀，慎用） |
| 停 MariaDB | 关掉 start.bat 的窗口 |
| 重启顺序 | 先停 Halo → 再停/启 MariaDB → 再启 Halo（Halo 依赖数据库，**开机先起数据库**） |

**优雅停机建议**：在 Halo 窗口按 `Ctrl + C` 等待几秒退出，比直接关窗口更安全（等写入落盘）。

---

## 9. 重新打包升级

改了源码后要出新 jar：

```bat
:: 1. 停掉正在运行的 Halo（jar 被占用时打包会失败！）
taskkill /F /IM java.exe    :: 或关掉 Halo 窗口

:: 2. 打包（在源码目录）
cd /d H:\aiWork\halo
build-jar.bat               :: 产物 application\target\halo-2.28.0-SNAPSHOT.jar
:: build-war.bat            :: 需要 war 时用这个

:: 3. 按"快速启动"重新启动 Halo（数据库不用动，数据都在）
```

`build-war.bat` 会临时改 application/pom.xml 的打包方式再自动还原，**打包失败也会还原**；万一脚本被强杀留下 `application\pom.xml.build-war.bak`，把它复制回 `application\pom.xml` 即可。

**升级前备份**：第 7 节的数据库备份 + 数据目录。

---

## 10. 常见问题排查（FAQ）

**1. 打开 8090 白屏/连接拒绝**
先看两件事：① MariaDB 窗口还开着吗？② Halo 窗口里有没有 `Started Application`？
数据库没起是最常见原因（占历史故障的绝大多数）。顺序永远是：先数据库、后 Halo。

**2. Halo 启动时报 `Communications link failure` / r2dbc 连接错误**
同上，MariaDB 没启动。双击 `D:\mariadb\start.bat`，等 `ready for connections`，再重启 Halo。

**3. 打包时报 `Unable to rename ... halo-xxx.jar`**
Halo 还在运行，锁着 jar。先 `taskkill /F /IM java.exe`（或关 Halo 窗口）再打包。

**4. 启动后日志里大量 `UnsupportedClassVersionError`**
正常现象。预置的官方插件是 Java 21 编译的，本环境是 JDK 17，这些插件起不来。**不影响博客本身**（文章/评论管理/附件都正常），只是应用商店等插件功能不可用。要消除它需换 JDK 21 运行（代码层面无需任何改动，但本项目按 17 定制过测试基线，换之前先跑一遍测试）。

**5. 插件页是空的 / 装插件失败**
同第 4 条。本环境插件体系不可用，评论前台组件也随之禁用。

**6. 上传主题 zip 报 "Failed to unzip theme"**
zip 是用 PowerShell `Compress-Archive` 打的（反斜杠条目路径，Halo 解压不了）。用 `jar --create --no-manifest --file=主题名.zip theme.yaml settings.yaml templates` 或 7-Zip 重新打包。

**7. 上传主题报"主题已存在"或激活 404**
主题记录和主题文件夹状态不一致了。处理：Console 删除该主题（或调 DELETE 接口），确认 `H:\halo2-maven-test\themes\` 下同名文件夹也删了，再重新安装。

**8. MariaDB 窗口太碍事 / 想开机自启**
进阶做法（需要管理员权限）：

```bat
D:\mariadb\bin\mariadbd.exe --install-service MariaDB --datadir=D:/mariadb/data
net start MariaDB
```

之后就是 Windows 服务，开机自启、后台运行。卸载：`net stop MariaDB && D:\mariadb\bin\mariadbd.exe --remove-service MariaDB`。

**9. 想把博客改成 MySQL/PostgreSQL**
仓库 `application/src/main/resources/` 下有现成的 `application-mysql.yaml` / `application-postgresql.yaml`，把启动参数 `--spring.profiles.active=mariadb` 换成对应 profile，并自行改里面的连接串/密码。**注意换库 = 换数据**，先用第 7 节的 dump 导入到新库。

**10. 前台样式乱了 / 改了模板不生效**
① 浏览器强刷（Ctrl+F5，静态资源带版本号缓存）；② 改的是运行时副本 `H:\halo2-maven-test\themes\...` 还是仓库源码 `theme-bootstrap3\`——运行时只读前者；③ 改了 theme.yaml/settings.yaml 需在 Console 主题页"重载配置"。

**11. 忘记 admin 密码**
数据库方式重置（生成新密码哈希较麻烦，最简单是重跑初始化）：停 Halo → 连库执行 `DELETE FROM extensions WHERE name='/registry/users/admin'` → 启动 Halo 会回到初始化向导重新建号（**会重新灌初始数据，文章不丢但设置重置**，慎用）。更好的办法：平时在 Console → 用户里绑定邮箱，用登录页"忘记密码"流程。

**12. 端口想换**
改启动参数里的 `--server.port=8090` 为任意空闲端口。数据库端口在 `D:\mariadb\start.bat` 和 Halo 的 mariadb profile 里成对修改。

---

## 附：一分钟健康检查

```bat
curl http://localhost:8090/actuator/health        → {"status":"UP"}
curl -s http://localhost:8090 | findstr bootstrap → 有输出 = 主题生效
D:\mariadb\bin\mariadb.exe -u root -pmariadb halo -e "SELECT COUNT(*) FROM extensions"
tasklist | findstr "java mariadbd"                → 两个进程都在
```

四条都正常 = 系统健康。更多细节（架构、构建、开发）见 `docs/HANDOVER.md`。
