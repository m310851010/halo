# Halo 2.x 主题定制指南（附 Bootstrap 3.x 实战）

> 本文档基于 Halo 2.28（本仓库源码）实测编写，配套示例主题位于仓库根目录
> [`theme-bootstrap3/`](../theme-bootstrap3/)，已在本机 MariaDB 环境下完成全部页面渲染验证。

---

## 目录

1. [Bootstrap 做主题可行吗？](#1-bootstrap-做主题可行吗)
2. [主题的目录结构](#2-主题的目录结构)
3. [theme.yaml —— 主题清单](#3-themeyaml--主题清单)
4. [settings.yaml —— 可视化设置项](#4-settingsyaml--可视化设置项)
5. [模板编写核心：layout 片段模式](#5-模板编写核心layout-片段模式)
6. [模板里能用的变量](#6-模板里能用的变量)
7. [各页面模板与变量速查](#7-各页面模板与变量速查)
8. [静态资源与 URL 重写规则](#8-静态资源与-url-重写规则)
9. [评论组件与 Halo 注入](#9-评论组件与-halo-注入)
10. [开发调试与安装启用](#10-开发调试与安装启用)
11. [常见定制示例](#11-常见定制示例)
12. [踩坑记录（必读）](#12-踩坑记录必读)

---

## 1. Bootstrap 做主题可行吗？

**完全可行。** Halo 主题本质是 [Thymeleaf](https://www.thymeleaf.org) 模板 + 静态资源，
模板只输出 HTML，任何 CSS 框架都能用：

| 框架 | 可行性 | 说明 |
|---|---|---|
| Bootstrap 3.x | ✅ | 经典栅格（`col-md-*`）+ 组件（panel/navbar/pagination），无需构建工具，直接放 `templates/assets/` 引用即可。本仓库 `theme-bootstrap3` 即为成品示例 |
| Bootstrap 4/5 | ✅ | 同理；5.x 无 jQuery 依赖更干净。注意 4/5 用 `card` 替代 3 的 `panel` |
| TailwindCSS | ✅ | 官方 starter（`theme-vite-starter`）走 Vite 构建路线 |
| 纯手写 CSS | ✅ | 最简单，零依赖 |

**Bootstrap 3 的优势**：兼容老浏览器、无构建链、社区模板丰富；**劣势**：2019 年后不再更新，
对新 CSS 特性（grid、变量）支持弱。如果你要长期维护、追求现代观感，建议 Bootstrap 5；
如果是内部系统、快速交付或怀旧风格，Bootstrap 3 依然非常合适。

> Bootstrap 3.4.1 = 3.x 最终版。国内 CDN 推荐_BOOTCDN_：
> `https://cdn.bootcdn.net/ajax/libs/twitter-bootstrap/3.4.1/css/bootstrap.min.css`。
> 但主题发布时**建议把 css/js 下载进主题目录**（本主题就是这么做的），不依赖外部 CDN。

---

## 2. 主题的目录结构

```text
theme-bootstrap3/
├── theme.yaml              # 必需：主题清单（元数据 + 设置项声明）
├── settings.yaml           # 可选：Console 可视化设置表单
└── templates/              # 所有模板（Thymeleaf，.html）
    ├── layout.html         # 公共布局片段（页头/导航/页脚）
    ├── index.html          # 首页（文章列表）
    ├── post.html           # 文章详情
    ├── page.html           # 单页面（"关于我们"这类独立页面）
    ├── archives.html       # 归档
    ├── categories.html     # 分类集合页
    ├── category.html       # 单个分类的文章列表
    ├── tags.html           # 标签集合页
    ├── tag.html            # 单个标签的文章列表
    └── assets/             # 静态资源（css/js/图片）
        ├── css/bootstrap.min.css
        ├── css/style.css
        └── js/{jquery,bootstrap}.min.js
```

要点：

- **文件夹名必须等于 `theme.yaml` 里 `metadata.name`**，否则静态资源 404。
- 模板放在 `templates/` 下，**静态资源也放在 `templates/assets/`**（不是根目录 `static/`），
  Halo 会把 `templates/` 下非模板文件当作主题资源对外服务（见第 8 节）。
- `metadata.name`、`spec.settingName`、`spec.configMapName` 全局唯一，重装时会冲突。

---

## 3. theme.yaml —— 主题清单

来自本主题的完整可用示例：

```yaml
apiVersion: theme.halo.run/v1alpha1
kind: Theme
metadata:
  name: theme-bootstrap3          # 必须与文件夹同名
spec:
  displayName: Bootstrap 3 经典风  # Console 里显示的名称
  author:
    name: your-name
    website: https://example.com
  description: 基于 Bootstrap 3.4.1 的经典博客主题
  logo: ""
  website: ""                     # 主题主页
  repo: ""                        # 源码仓库
  issues: ""                      # 反馈地址
  settingName: theme-bootstrap3-setting        # 对应 settings.yaml 的 metadata.name
  configMapName: theme-bootstrap3-configMap    # 运行时保存用户配置
  version: 1.0.0
  requires: ">=2.0.0"             # 适配的 Halo 版本范围
  license:                        # ⚠️ 当前版本是数组（见踩坑 12.1）
    - name: GPL-3.0
      url: https://www.gnu.org/licenses/gpl-3.0.html
```

---

## 4. settings.yaml —— 可视化设置项

在 Console「主题 → 设置」中渲染成表单，用户改完的值存在 `configMapName` 对应的
ConfigMap 里，模板中用 `theme.config.<group>.<name>` 读取。

```yaml
apiVersion: v1alpha1
kind: Setting
metadata:
  name: theme-bootstrap3-setting
spec:
  forms:
    - group: general              # 分组名，读取时用 theme.config.general.xxx
      label: 通用
      formSchema:
        - $formkit: text          # FormKit 语法：text/textarea/checkbox/select/color...
          name: footer_text
          label: 页脚文字
          value: "由 Halo 强力驱动"
    - group: header
      label: 顶部导航
      formSchema:
        - $formkit: select
          name: navbar_style
          label: 导航栏风格
          value: navbar-inverse
          options:
            - label: 深色（反色）
              value: navbar-inverse
            - label: 浅色（默认）
              value: navbar-default
```

模板中读取（用 `?.` 防止配置为空时 NPE，用 `?:` 给默认值）：

```html
<nav th:classappend="${theme.config.header?.navbar_style} ?: 'navbar-inverse'">
<p th:utext="${theme.config.general?.footer_text} ?: '默认页脚'"></p>
```

---

## 5. 模板编写核心：layout 片段模式

每个页面模板都要输出完整 HTML，公共部分（导航/页脚/资源引用）用 Thymeleaf 片段复用。
这是 Halo 官方 starter 的推荐模式：

**layout.html** 定义一个 `html(head, content)` 片段：

```html
<!DOCTYPE html>
<html lang="zh-CN"
      xmlns:th="https://www.thymeleaf.org"
      th:fragment="html (head, content)">
<head>
  <meta charset="UTF-8"/>
  <meta name="viewport" content="width=device-width, initial-scale=1"/>
  <title th:text="${site.title}"></title>
  <link rel="stylesheet" th:href="@{/assets/css/bootstrap.min.css}"/>
  <link rel="stylesheet" th:href="@{/assets/css/style.css?v={v}(v=${theme.spec.version})}"/>
  <!-- 页面专属 head 片段（如 RSS meta），不传则跳过 -->
  <th:block th:if="${head != null}">
    <th:block th:replace="${head}"/>
  </th:block>
</head>
<body>
  <nav>…公共导航…</nav>
  <th:block th:replace="${content}"/>
  <footer>…公共页脚…</footer>
</body>
</html>
```

**index.html 等页面**只写自己的内容块，然后整体替换为 layout：

```html
<!DOCTYPE html>
<html xmlns:th="https://www.thymeleaf.org"
      th:replace="~{layout :: html(head = null, content = ~{::content})}">
<th:block th:fragment="content">
  <!-- 页面内容 -->
</th:block>
</html>
```

这样每个页面模板极短，改版只动 layout.html。

**`<head>` 会被 Halo 自动增强**：全局 favicon、SEO meta、统计脚本等由 Halo 的
`GlobalHeadInjectionProcessor` 在渲染 `<head>` 标签时自动注入，主题无需手写。

常用 Thymeleaf 语法速查：

| 写法 | 作用 |
|---|---|
| `th:text` / `th:utext` | 输出文本 / 输出 HTML（文章正文必须用 utext） |
| `th:href="@{${post.status.permalink}}"` | 输出链接（@{} 会做上下文路径与主题资源重写） |
| `th:each="post : ${posts.items}"` | 循环 |
| `th:if` / `th:unless` | 条件 |
| `${a?.b ?: '默认值'}` | 空安全 + 默认值（Spring EL） |
| `#lists.isEmpty(x)` | 集合判空 |
| `#temporals.format(x, 'yyyy-MM-dd')` | 格式化日期（x 必须是 Temporal 类型，见踩坑 12.2） |

---

## 6. 模板里能用的变量

### 6.1 全局变量（所有模板可用）

| 变量 | 常用属性 |
|---|---|
| `site` | `site.title`、`site.subtitle`、`site.url`、`site.logo`、`site.favicon`、`site.version`、`site.post.postPageSize` |
| `site.routes` | `site.routes.categoriesUri` / `tagsUri` / `archivesUri`（导航链接直接用） |
| `theme` | `theme.spec.displayName`、`theme.spec.version`、`theme.config.<group>.<name>` |
| `sec` | Spring Security：`sec:authorize="isAuthenticated()"` 控制登录态显示 |

### 6.2 页面级变量

见下表，均由对应路由注入：

| 模板 | 变量 |
|---|---|
| index / category / tag | `posts`（分页文章）、`category` 或 `tag` |
| post | `post`（含 `post.content.content` 正文 HTML） |
| page | `singlePage` |
| archives | `archives`（按年月分组的文章） |
| categories | `categories`（全部分类列表） |
| tags | `tags`（全部标签列表） |

### 6.3 文章对象（ListedPostVo / PostVo）常用路径

```text
post.metadata.name            内部唯一名
post.spec.title               标题
post.spec.slug                别名
post.spec.publishTime         发布时间（Temporal）
post.spec.cover               封面图
post.spec.categories          分类对象列表（含 status.permalink / spec.displayName）
post.spec.tags                标签对象列表
post.status.permalink         访问链接
post.status.excerpt           摘要（纯文本）
post.stats.visit              浏览量（PostVo）
post.content.content          渲染后的正文 HTML（仅 post 模板）
```

---

## 7. 各页面模板与变量速查

### index.html（首页）

```html
<th:block th:each="post : ${posts.items}">
  <a th:href="@{${post.status.permalink}}" th:text="${post.spec.title}"></a>
  <p th:text="${post.status.excerpt}"></p>
</th:block>
<a th:if="${posts.hasPrevious()}" th:href="@{${posts.prevUrl}}">上一页</a>
<a th:if="${posts.hasNext()}" th:href="@{${posts.nextUrl}}">下一页</a>
```

### post.html（文章页）

```html
<h1 th:text="${post.spec.title}"></h1>
<time th:text="${#temporals.format(post.spec.publishTime, 'yyyy-MM-dd')}"></time>
<article th:utext="${post.content.content}"></article>
<!-- 评论组件，由评论插件渲染 -->
<halo:comment th:attr="group=${'content.halo.run'},kind=${'Post'},name=${post.metadata.name}"/>
```

### page.html（单页面）

```html
<h1 th:text="${singlePage.spec.title}"></h1>
<article th:utext="${singlePage.content.content}"></article>
```

### archives.html（归档，按年→月→文章三层）

```html
<th:block th:each="archive : ${archives.items}">
  <h3 th:text="${archive.year}"></h3>
  <th:block th:each="month : ${archive.months}">
    <!-- ⚠️ month.month 是字符串（如 "08"），不要用 #temporals.format -->
    <span th:text="${month.month} + ' 月'"></span>
    <a th:each="post : ${month.posts}"
       th:href="@{${post.status.permalink}}" th:text="${post.spec.title}"></a>
  </th:block>
</th:block>
<a th:if="${archives.hasPrevious()}" th:href="@{${archives.prevUrl}}">上一页</a>
```

### categories.html / category.html

```html
<!-- 集合页：categories 列表 -->
<a th:each="category : ${categories}"
   th:href="@{${category.status.permalink}}" th:text="${category.spec.displayName}"></a>
<!-- 文章计数是顶层字段 postCount，不是 status.visiblePostCount -->
<span class="badge" th:text="${category.postCount}"></span>

<!-- 单分类页：category + posts（同 index 分页写法） -->
<h1 th:text="${category.spec.displayName}"></h1>
```

### tags.html / tag.html

```html
<a th:each="tag : ${tags}" th:href="@{${tag.status.permalink}}"
   th:text="${tag.spec.displayName}"></a>
<!-- tag.html：tag.spec.displayName + posts.items，同 category.html -->
```

---

## 8. 静态资源与 URL 重写规则

模板里引用主题资源**统一写 `@{/assets/...}`**，Halo 的 `ThemeLinkBuilder` 会在渲染时自动重写：

```text
模板写的：  @{/assets/css/bootstrap.min.css}
实际输出：  /themes/theme-bootstrap3/assets/css/bootstrap.min.css?v=1.0.0
```

- 自动带上 `?v=<主题版本>`，升级主题后浏览器缓存自动失效。
- 只有 `@{}` 生成的链接才会重写，**手写 `<link href="/assets/...">` 不会**。
- 非 `@{}` 的站内链接在预览模式下会被追加预览参数（预览未激活主题时用）。

---

## 9. 评论组件与 Halo 注入

| 需求 | 写法 |
|---|---|
| 文章评论区 | `<halo:comment th:attr="group=${'content.halo.run'},kind=${'Post'},name=${post.metadata.name}"/>` |
| 页面评论区 | `kind=${'SinglePage'}` |
| 搜索框 | 由 plugin-search-widget 插件注入，模板无需处理 |

`xmlns:halo` 命名空间声明只是为了 IDE 提示，运行时靠标签前缀匹配，可写可不写。
评论是否显示受评论插件与站点设置控制，未启用时该标签渲染为空。

---

## 10. 开发调试与安装启用

### 开发时热更新

- 启动加 `--spring.thymeleaf.cache=false`，改模板刷新即生效。
- 改了 `settings.yaml`/`theme.yaml` 后，Console 主题页点「重载配置」，
  或调 `PUT /apis/api.console.halo.run/v1alpha1/themes/{name}/reload`。
- 改了静态资源后浏览器强刷即可（`?v=` 不变时可能缓存，可临时加参数）。

### 打包上传

```bash
# ⚠️ 用 jar/7z 打包（正斜杠条目），不要用 PowerShell Compress-Archive（反斜杠条目会导致解压失败）
jar --create --no-manifest --file=theme-bootstrap3.zip theme.yaml settings.yaml templates
```

Console → 主题 → 安装 → 上传 zip → 启用。
命令行方式（先登录拿 SESSION cookie，见 10.1）：

```bash
POST   /apis/api.console.halo.run/v1alpha1/themes/install       # multipart, file=@theme.zip
PUT    /apis/api.console.halo.run/v1alpha1/themes/{name}/activation
PUT    /apis/api.console.halo.run/v1alpha1/themes/{name}/invalidate-cache
```

### 10.1 命令行登录（脚本自动化用）

Halo 登录密码是 **RSA(PKCS1) 加密后 Base64** 传输的，公钥内嵌在 `/login` 页面 HTML 的
`const publicKey = "..."` 里（JSEncrypt 加密）。curl 自动化步骤：

```bash
# 1. 拿 session + csrf
curl -s -c jar http://localhost:8090/login > login.html
CSRF=$(grep -oE 'name="_csrf" value="[^"]*"' login.html | sed 's/.*value="//;s/"//')
# 2. 提取公钥并用 node 加密密码
PUB=$(grep -oE 'const publicKey = "[^"]*"' login.html | sed 's/.*= "//;s/"//;s#\\/#/#g')
ENC=$(node -e "const c=require('crypto');const k=c.createPublicKey({key:Buffer.from('$PUB','base64'),format:'der',type:'spki'});console.log(c.publicEncrypt({key:k,padding:c.constants.RSA_PKCS1_PADDING},Buffer.from('你的密码')).toString('base64'))")
# 3. 登录（302 到 /uc 即成功，SESSION 在 jar 里）
curl -s -b jar -c jar -X POST http://localhost:8090/login \
  -d "username=admin" --data-urlencode "password=$ENC" --data-urlencode "_csrf=$CSRF"
```

---

## 11. 常见定制示例

以下示例都可直接加进 `theme-bootstrap3`：

### 11.1 给列表页加封面大图

```html
<img th:if="${post.spec.cover}" th:src="@{${post.spec.cover}}" class="img-responsive"/>
```

### 11.2 三栏布局（主栏 + 侧栏）

Bootstrap 3 栅格：把 layout 的内容区改成两列，页面模板不用动：

```html
<div class="row">
  <div class="col-md-8 col-sm-12"><th:block th:replace="${content}"/></div>
  <div class="col-md-4 col-sm-12">…侧栏组件…</div>
</div>
```

### 11.3 侧栏加"最新评论 / 分类"

Halo 提供 Finder（模板里直接调）：

```html
<div class="panel panel-default">
  <div class="panel-heading">分类</div>
  <div class="list-group">
    <a class="list-group-item" th:each="c : ${categoryFinder.listAsTree()}"
       th:href="@{${c.status.permalink}}" th:text="${c.spec.displayName}"></a>
  </div>
</div>
```

常用 Finder：`postFinder.list(page,size)`、`categoryFinder.*`、`tagFinder.*`、
`menuFinder.getPrimary()`、`siteStatsFinder.getStats()`。

### 11.4 导航菜单用后台可配置的菜单

```html
<ul class="nav navbar-nav" th:with="menu = ${menuFinder.getByName('primary')}">
  <li th:each="item : ${menu.menuItems}">
    <a th:href="@{${item.status.href}}" th:text="${item.status.displayName}"></a>
  </li>
</ul>
```

### 11.5 配色定制

改 `templates/assets/css/style.css` 即可（在 bootstrap 之后加载，天然覆盖）。
或加一个 `$formkit: color` 设置项让用户自己挑（参考 settings.yaml 的写法）。

---

## 12. 踩坑记录（必读）

以下都是在本次实测中真实踩到的问题：

1. **`spec.license` 是数组**：`license: {name,url}` 会安装失败（500，Jackson 反序列化
   `ArrayList<License>` 报错），必须写成 `- name: ...` 列表。
2. **归档的 `month.month` 是字符串**：不能 `#temporals.format(month.month, ...)`，直接输出即可；
   `post.spec.publishTime` 才是 Temporal。
3. **分类文章数是 `category.postCount`**（顶层），不存在 `status.visiblePostCount` 路径。
4. **zip 打包别用 PowerShell Compress-Archive**：它生成反斜杠条目路径，Halo 解压报
   "Failed to unzip theme"。用 `jar` 或 7-Zip。
5. **安装 API 前缀**：主题安装/激活在 `/apis/api.console.halo.run/v1alpha1/themes/...`，
   激活和 reload/invalidate-cache 都是 **PUT**。
6. **密码登录不能明文**：`/login` 表单的 password 字段必须是 RSA-PKCS1 加密后 Base64
   （见 10.1），否则报 invalid-credential。
7. **theme.yaml 改动需重新上传**：直接改已安装主题文件夹里的 theme.yaml 不会刷新扩展记录，
   建议改完重新打包 → 重新安装（或 Console 升级）。
8. **中文文件名/路径**：主题文件夹名只允许字母数字连字符（`metadata.name` 约束）。

---

## 附：本仓库主题验证结果（2026-11-10）

| 页面 | 状态 |
|---|---|
| `/` 首页（文章列表 + 分页 + Bootstrap 导航/页脚） | ✅ 200 |
| `/archives/hello-halo` 文章页（正文 + 评论组件 + 浏览量） | ✅ 200 |
| `/archives` 归档（年-月-文章分组） | ✅ 200 |
| `/categories` + `/categories/default` 分类页 | ✅ 200 |
| `/tags` 标签云 | ✅ 200 |
| 静态资源 `?v=` 版本化重写 | ✅ 正常 |

运行环境：Windows + JDK 17 + Maven 构建 + MariaDB 11.4（D:\mariadb）。
