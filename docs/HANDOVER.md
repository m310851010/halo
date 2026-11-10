# Halo 本地化改造项目 — 交接文档

> 面向接手的开发者。本文档记录本仓库相对官方上游的全部改造、环境布局、构建系统细节与踩坑记录。
> 所有内容均经过实机验证（2026-11-10 ~ 11-11）。
> 面向日常使用/运维请看 [USER-GUIDE.md](./USER-GUIDE.md)。

---

## 目录

1. [项目是什么](#1-项目是什么)
2. [环境清单（这台机器上装了什么）](#2-环境清单)
3. [构建系统（Maven）](#3-构建系统maven)
4. [JDK 21 → 17 降级改动明细](#4-jdk-21--17-降级改动明细)
5. [运行拓扑](#5-运行拓扑)
6. [日常开发命令](#6-日常开发命令)
7. [Git 仓库布局与发布](#7-git-仓库布局与发布)
8. [已知限制与踩坑记录（必读）](#8-已知限制与踩坑记录必读)
9. [接手第一天检查清单](#9-接手第一天检查清单)

---

## 1. 项目是什么

官方 [halo-dev/halo](https://github.com/halo-dev/halo)（v2.28.0-SNAPSHOT，基于 Gradle + Java 21）的本地化 fork，目标：**在这台只有 JDK 17 的 Windows 机器上，不用 Docker，从源码构建并长期运行**。

核心改造（按提交顺序）：

| 提交 | 内容 |
|---|---|
| `acb91e0db` | Gradle → Maven 迁移 + JDK 17 降级 |
| `5d6c8646d` | Bootstrap 3 主题 `theme-bootstrap3/` + 主题定制文档 |
| `0deee2a31` | 测试与视图名安全检查的平台无关化修复 |
| `3db62b90f` | 临时文件迁离 C 盘（surefire tmpdir → target/tmp） |
| `f2866b52a` | `build-jar.bat` / `build-war.bat` 打包脚本 + WAR 支持 |
| `985b187c7` | 仓库指南文档从 Gradle 更新为 Maven 命令 |

**刻意不做的**（保持最小化，别顺手加回去）：
- Maven Central 发布（api/platform 模块的签名发布链路已随 Gradle 移除）
- Docker 镜像构建（原 BootBuildImage 任务）
- CI 工作流改造（`.github/workflows/` 仍引用 gradlew，**已失效**，不修）
- OpenAPI 文档生成与 UI api-client 再生成（原 Gradle 任务无 Maven 等价物，`ui/packages/api-client/` 里的代码是冻结快照，改 API 合同时手工同步）
- platform/application、platform/plugin 两个 BOM 模块（服务外部插件开发者，本地不需要）

---

## 2. 环境清单

| 组件 | 位置 | 版本/说明 |
|---|---|---|
| JDK 17 | `D:\Develop\Java\jdk-17.0.16` | 项目编译与运行（Maven 亦运行其上） |
| Maven | `D:\Develop\apache-maven-3.8.7` | 3.8.7；本地仓库 `D:\repository`；镜像 aliyun（用户 settings.xml 已配） |
| MariaDB | `D:\mariadb\` | 11.4.7 LTS，winx64 zip 免安装；root 密码 `mariadb`；库 `halo` |
| Halo 源码 | `H:\aiWork\halo` | 本仓库 |
| Halo 运行工作目录 | `H:\halo2-maven-test\` | 附件/日志/主题/H2 备选库都在这；`tmp/` 是运行时 tmpdir |
| 主题安装包 | `D:\tmp\theme-bootstrap3.zip` | 可直接从 Console 上传的可安装 zip |
| Git 远程 | `origin` = 官方 halo-dev/halo（只读）；`fork` = github.com/m310851010/halo（推送目标） | gh CLI 已登录 m310851010 |

**未安装**：JDK 21、Docker、外部 Tomcat（曾临时下载用于验证，已清理）。

---

## 3. 构建系统（Maven）

### 3.1 模块结构

```text
pom.xml                 根：aggregator + dependencyManagement（import spring-boot-dependencies 4.2.0-RC2）
├── api/pom.xml         库模块（jar）：对外 API，256 个源文件
├── ui/pom.xml          packaging=pom：驱动 pnpm 构建前端（Node 24.11.0 + pnpm 12.4.2，产物 ui/build/dist）
└── application/pom.xml 主应用（jar）：boot repackage 出可执行 halo.jar
```

Reactors 顺序 api → ui → application（application 对 ui 有个 provided 的 pom 依赖仅为排序）。

### 3.2 关键配置点（改错会炸）

- **Spring Boot 4.2.0-RC2 是里程碑版**：根 pom 的 `<repositories>` 必须保留 `repo.spring.io/milestone`，否则依赖解析失败。
- **版本钉子**（根 pom properties）：`thymeleaf.version=3.1.6.RELEASE`（修 WebFlux DataBuffer 泄漏，Boot BOM 还是 3.1.5）、`jsonassert 2.0-rc1`（不钉则 ListResultTest 编译失败，JSONException 变受检异常）、`lucene=9.12.0`（Java 17 上限）。
- **ui 模块的 pnpm 集成**（ui/pom.xml antrun）：
  - frontend-maven-plugin 只负责装 Node；
  - pnpm 从 npmmirror 拉 tarball 手工解压到 `ui/target/node/node_modules/pnpm`；
  - **pnpm 原生二进制缓存机制**：pnpm 启动器会把下载的 `pnpm-native.exe` 存在包目录内，解压新包会丢它并触发对 registry.npmjs.org 的重下载（间歇性失败）。所以 antrun 在每次重解压后把缓存副本 `ui/target/node/pnpm-native.exe.cached` 回填进包目录，构建成功后再把新的拷出去。**如果缓存丢了且网络不通**：手工执行
    `cd ui && ./target/node/node.exe ./target/node/node_modules/pnpm/bin/pnpm.mjs --version`（带重试），成功后再 `cp target/node/node_modules/pnpm/pnpm-native.exe target/node/pnpm-native.exe.cached`。
  - npmmirror 没有 `@pnpm/exe-win32-x64` 平台包，别试图从镜像拉二进制。
- **surefire tmpdir**：api/application 都配了 `-Djava.io.tmpdir=${project.build.directory}/tmp`，由 antrun 在 generate-test-resources 阶段 mkdir。作用：JUnit 临时目录（尤其 Lucene 测试的索引）落在模块 target 下而不是 C 盘系统 TEMP。**别删 antrun 的 mkdir**，tmpdir 指向不存在的目录会让所有测试直接报错。
- **maven-war-plugin 钉 3.4.0**（根 pom pluginManagement）：war 打包时若解析到 Maven 自带的 2.2 版会因 API 不兼容直接失败。
- **application 模块三个 optional 依赖**：lombok、configuration-processor、spring-context-indexer、therapi scribe + `jakarta.servlet-api`（provided，只为 HaloServletInitializer 编译）。annotationProcessor 走 classpath 发现，pom 里没有 compiler 的 annotationProcessorPaths 配置——别加，加了反而容易因 therapi 传递依赖报 NoClassDefFoundError。
- **application/resources 阶段**：`copy-ui-dist` 把 `../ui/build/dist` 拷进 `target/classes`；10 个预置插件 jar 由 download-maven-plugin 下载；`presets.json` 由 antrun 生成；`git.properties` 由 git-commit-id 插件生成；`build-info.properties` 由 spring-boot 插件生成。产物校验：jar 里应有 `BOOT-INF/classes/ui/console.html` 等 535 个前端资源。

### 3.3 打包脚本

| 脚本 | 做什么 | 产物 |
|---|---|---|
| `build-jar.bat` | `mvn -DskipTests package` | `application/target/halo-<version>.jar` |
| `build-war.bat` | 临时把 application/pom.xml 的 packaging 改成 war → 构建 → **无论成败还原 pom 并删备份** | `application/target/halo-<version>.war` |

两个脚本均为 CRLF。**运行脚本前必须先停掉正在运行的 Halo**（Windows 锁定运行中的 jar，repackage 重命名会报 Unable to rename）。`build-war.bat` 的备份文件名是 `application/pom.xml.build-war.bak`，若脚本中途被强杀，手工 `copy /y bak → pom.xml` 恢复。

### 3.4 WAR 的边界（重要）

Halo 是 WebFlux 应用。实测外部 Tomcat 11 部署失败：servlet 环境下 Spring Security 的 servlet/reactive 两套配置注册同名 bean `conversionServicePostProcessor` 冲突（`BeanDefinitionOverrideException`）。这是 Spring Boot 官方不支持的场景，**不是本仓库的 bug**。WAR 是"可执行 war"，只用于 `java -jar` 直跑（满足需要 .war 形态的流程）。`HaloServletInitializer` 仅为让 WAR 结构合规，java -jar 时不会被用到。

---

## 4. JDK 21 → 17 降级改动明细

改动原则：改写语言特性，不改行为。全部有测试覆盖（api 264 + application 2209，0 失败）。

### 语言特性改写

| 文件 | Java 21 特性 | 改成 |
|---|---|---|
| `application/.../extension/index/query/QueryVisitor.java` | switch 模式匹配 + record pattern（27 个 case） | if/else instanceof 链，访问器方法取字段 |
| `api/.../extension/controller/DefaultController.java:115` | `Thread.ofVirtual()` 虚拟线程工厂 | 普通 `new Thread` + 计数命名 + uncaughtExceptionHandler |
| `application/.../core/attachment/thumbnail/DefaultLocalThumbnailService.java` | `Thread.ofPlatform().daemon()` 工厂 + `ExecutorService.close()` | 匿名 ThreadFactory（AtomicInteger 计数）+ `shutdown()`/`awaitTermination(30s)` |
| `application/.../theme/dialect/{DefaultFaviconHeadProcessor,GlobalSeoProcessor,IndexSeoProcessor}.java` | `LinkedHashMap.newLinkedHashMap(2)` | `new LinkedHashMap<String, String>(2)`（必须显式泛型，var + 钻石会推成 Object） |

### SequencedCollection 调用替换（`getFirst/getLast/addFirst/reversed` → Java 17 API）

主代码 4 处：`RequestSynchronizer.java`（get(size-1)）、`DefaultIndicesInitializer.java`、`LogoutSecurityConfigurer.java`（get(0)）、`ThemeLinkBuilder.java`（加了 isEmpty 防护）。
测试 6 处：`CommentPermalinkIntegrationTest`（倒序 for 循环）、`UiPluginRoleTemplateTest`、`MenuItemHierarchyMigrationTest`、`DefaultIndexEngineTest`、`UiPluginBundleServiceImplTest`、api 的 `DefaultIndexEngineTest`/`LabelIndexImplTest`（descendingKeySet）。

注意保留的（**不是** Java 21 API，别误改）：Spring `MultiValueMap.getFirst()`、commons-lang3 `Pair.getFirst()`、`Deque.addFirst/removeLast`（Java 6 起）、`Comparator.reversed()`（Java 8 起）。

### 依赖降级

- **Lucene 10.5.2 → 9.12.0**（Lucene 10 要求 Java 21 运行时）。唯一代码影响：`LuceneSearchEngine.java:232` 的 `hits.totalHits.value()` 回退为字段 `hits.totalHits.value`。git 历史证明上游升 10 时只改了这一行。
- 索引引擎（extension/index/**）不依赖 Lucene，不受影响。

### 平台无关化修复（Windows + JDK 17 下发现）

1. `LuceneSearchEngineTest`：`@TempDir(cleanup = CleanupMode.NEVER)`——Lucene 9 的 mmap 段文件要等 GC 解锁（Lucene 10 靠 Java 21 FFI 显式 unmap），JUnit 删临时目录会失败。
2. `CategoryFinderImplTest`：假数据时间戳加 `AtomicLong` 严格递增——Windows 时钟粒度粗，同毫秒时间戳触发字典序兜底排序打乱断言。
3. `DefaultViewNameResolver.isSafeViewName`：补 `name.startsWith("/")` 显式拒绝——`Path.of("/x").isAbsolute()` 在 Windows 是 false，`/` 开头的名字原本能绕过模板逃逸检查。**这个对上游有价值，回馈上游可单独提 PR。**

---

## 5. 运行拓扑

```text
[访客/浏览器]
     │ http://localhost:8090            ← Halo（java -jar，前台+Console+UC）
     │
[Halo] ── r2dbc ──→ [MariaDB 11.4.7] 127.0.0.1:3306（库 halo，root/mariadb）
   │
   └── 文件系统 H:/halo2-maven-test/
         ├── db/            （未用 mariadb profile 时才是 H2）
         ├── themes/        theme-bootstrap3、theme-earth
         ├── plugins/       已安装插件
         ├── attachments/   附件
         ├── logs/halo.log  应用日志
         └── tmp/           -Djava.io.tmpdir 指向这里（插件 bundle 解压等）
```

启动命令（完整版，也是 USER-GUIDE 的标准命令）：

```bash
java -Djava.io.tmpdir=H:/halo2-maven-test/tmp -jar application/target/halo-2.28.0-SNAPSHOT.jar --spring.profiles.active=mariadb --halo.work-dir=H:/halo2-maven-test --server.port=8090
```

凭据：站点管理员 `admin / Admin123!`（初始化时创建，可在 Console 改）。

---

## 6. 日常开发命令

```bash
# 全量打包（先停 Halo！）
mvn package -DskipTests          # 或 cmd //c build-jar.bat

# 测试
mvn test -pl api                 # 264 个，~1 分钟
mvn test -pl application         # 2209 个，~5.5 分钟
mvn test -pl application "-Dtest=QueryVisitorTest"   # 单类

# 前端（改 ui/ 时）
pnpm -C ui install
pnpm -C ui dev                   # 开发服务器，代理 8090
pnpm -C ui lint && pnpm -C ui typecheck

# 首次 clone 后（本地仓库里没有 run.halo.app 父 pom 时）
mvn install -N                   # 装根 pom
mvn install -DskipTests -pl api,ui
```

改模板热调试：启动加 `--spring.thymeleaf.cache=false`；改 settings.yaml 后调 `PUT /apis/api.console.halo.run/v1alpha1/themes/theme-bootstrap3/reload`。

主题开发详见 [theme-bootstrap3-customization.md](./theme-bootstrap3-customization.md)（含模板变量、Finder API、踩坑 8 条、CLI 登录自动化脚本）。

---

## 7. Git 仓库布局与发布

```bash
git remote -v
# origin  https://github.com/halo-dev/halo.git      （官方，无推送权限）
# fork    https://github.com/m310851010/halo.git    （推送目标）

# 工作分支：本地 main == 远程 feat/maven-jdk17-local
git push fork main:refs/heads/feat/maven-jdk17-local

# tag（annotated，随改动推进 force 更新）
git tag -f v2.28.0-maven-jdk17
git push fork -f v2.28.0-maven-jdk17
```

push 触发 husky/lint-staged（ui 的 vp fmt），属正常现象。`.gitignore` 已含 `target/`、`build/`、`.zcode/`。

---

## 8. 已知限制与踩坑记录（必读）

### 运行时

1. **预置插件加载失败刷屏**：`presets/plugins/` 里的 10 个官方插件 jar 是 Java 21 字节码，JDK 17 启动时日志报 `UnsupportedClassVersionError` 并被清理（shiki、mcp-server 等最明显）。**不影响 Halo 本体**，文章/评论/搜索等核心功能正常；评论前台组件不可用（被禁）。装回 JDK 21 即恢复——`application/pom.xml` 的 compiler release 和 api 的代码改写是仅有的两处 Java 版本约束。
2. **war 只能 java -jar**：见 3.4。
3. **Console/UC 与主题无关**：主题只改前台（见 USER-GUIDE）；评论组件样式由插件控制，主题 CSS 只能覆盖微调。

### 构建时

4. **repackage 报 Unable to rename** = Halo 还在运行锁着 jar。先 `taskkill` java 进程。
5. **ui 构建随机失败 "Could not download the pnpm binary"**：网络抖动 + 缓存丢失的组合。按 3.2 的恢复流程处理；不要删 `ui/target/node/pnpm-native.exe.cached`。
6. **PowerShell `Compress-Archive` 打的 zip 不能用于主题安装**（反斜杠条目导致解压失败），用 `jar --create --no-manifest` 或 7-Zip。
7. **临时文件纪律**：surefire tmpdir 已钉 target/tmp；运行实例要带 `-Djava.io.tmpdir=H:/halo2-maven-test/tmp`；文档示例的临时文件一律 `D:/tmp`。别再往 C 盘 TEMP 塞东西。

### 测试

8. 全量测试基线：api 264 + application 2209，0 失败（2026-11-11）。新失败先对照第 4 节的三个平台修复是否还在。
9. `LuceneSearchEngineTest` 首次跑约 37s（tmpdir 迁移后），属正常。

### 与上游分歧

10. git pull upstream 会冲突：`pom.xml` vs `build.gradle` 体系、`AGENTS.md`/`CONTRIBUTING.md`、以及所有 JDK 17 改写的 Java 文件。合并策略：先看上游 commit 是否改了被我们改写的同一批文件（用 `git log -S` 定位），冲突时优先保上游业务逻辑、手工套用本仓库的 Java 17 写法。

---

## 9. 接手第一天检查清单

```bash
# 1. 构建链通
cd H:/aiWork/halo && mvn -v                 # Maven 3.8.7 + JDK 17
mvn package -DskipTests -pl application     # 需要 root pom 已 install（见第 6 节）

# 2. 数据库通
D:/mariadb/bin/mariadb.exe -u root -pmariadb -e "SELECT VERSION()"   # 11.4.7-MariaDB
# 若 MariaDB 没跑：双击 D:\mariadb\start.bat

# 3. 应用通（若 8090 空闲）
java -Djava.io.tmpdir=H:/halo2-maven-test/tmp -jar application/target/halo-2.28.0-SNAPSHOT.jar --spring.profiles.active=mariadb --halo.work-dir=H:/halo2-maven-test --server.port=8090 &
curl http://localhost:8090/actuator/health          # {"status":"UP",...}
curl -s http://localhost:8090/ | grep -c bootstrap.min.css   # ≥1 = 主题生效

# 4. 测试基线复现
mvn test -pl api                             # 264/0/0

# 5. 权限通
gh auth status                               # m310851010
git push fork main:refs/heads/feat/maven-jdk17-local --dry-run
```

四项全过 = 环境完好，可以接手开发。
