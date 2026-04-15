# backend

一个基于 Scala 3、Cats Effect、http4s、Circe 的在线评测后端。

## 目录结构

- `src/main/scala/Main.scala`
  启动 http4s 服务。
- `src/main/scala/database`
  共享数据库连接池、事务边界和配置。
- `src/main/scala/domains/<domain>`
  按业务域组织代码，而不是按技术层先分。

当前主要域包括：

- `domains/auth`
- `domains/judge`
- `domains/judger`
- `domains/problem`
- `domains/problemset`
- `domains/submission`
- `domains/system`
- `domains/usergroup`
- `domains/shared`

## 后端分层规则

每个业务域优先按这四层组织：

- `model`
  领域类型、HTTP 边界模型、值对象。
- `application`
  用例编排、校验、权限决策、结果 ADT。
- `http`
  HTTP 路由分发、请求解析、响应映射。
- `table`
  PostgreSQL 持久化、SQL、row reader、migration。

详细规则见：

- [architecture-guardrails.md](/mnt/d/thu0/132-typesafe-modern-sys/qiwen-online-judge/docs/architecture-guardrails.md)

## 运行

```bash
sbt run
```

默认监听：

- `http://0.0.0.0:8080`

## 数据库默认连接

- `host = 127.0.0.1`
- `port = 5432`
- `databaseName = 当前项目目录名，把 '-' 自动转成 '_'`
- 用户名：`db`
- 密码：`root`

可通过环境变量覆盖：

- `DB_HOST`
- `DB_PORT`
- `DB_NAME`
- `DB_USER`
- `DB_PASSWORD`
- `DB_MAX_POOL_SIZE`
- `DB_CONNECTION_TIMEOUT_MS`

## 数据库连接策略

- 应用启动时初始化 PostgreSQL 连接池。
- `DatabaseSession` 负责事务连接的申请、提交、回滚和归还。
- `application` 层不直接管理连接生命周期。
- 路由和 command 之间通过 `DatabaseSession.withTransactionConnection` 建立事务边界。

## 日志

服务启动后控制台会输出：

- 服务启动日志
- 请求访问日志
- http4s 中间件请求/响应日志
