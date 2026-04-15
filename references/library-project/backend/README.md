# backend-sample

一个基于 Scala 3、Cats Effect、http4s、Circe 的后端样本项目。

## 目录结构

- `src/main/scala/Main.scala`: 主程序，启动 http4s 服务
- `src/main/scala/routes`: 路由定义与总路由分发
- `src/main/scala/api`: `plan` 定义与具体业务实现
- `src/main/scala/database`: 通用数据库连接与事务管理
- `src/main/scala/tables`: 每张表一个文件，集中维护建表 SQL、常见 SQL 和表操作
- `src/main/scala/objects`: 请求/响应类型定义

## 运行

```bash
sbt run
```

默认监听：

- `http://0.0.0.0:8080`

数据库默认连接：

- `host = 127.0.0.1`
- `port = 5432`
- `databaseName = 当前项目目录名，把 '-' 自动转成 '_'`
- 用户名：`db`
- 密码：`root`

例如当前项目目录是 `backend-sample`，默认数据库名就会是 `backend_sample`。如果目录名推导失败，则会退回到 [DatabaseDefaults.scala](/home/yang/projects/commons/backend-sample/src/main/scala/database/DatabaseDefaults.scala#L1) 里的常量 `DefaultDatabaseName`。所以当前默认 JDBC URL 是：

- `jdbc:postgresql://127.0.0.1:5432/backend_sample`

如果你换到别的项目，默认会优先使用那个项目目录名生成数据库名。比如目录是 `user-center`，默认就会尝试连：

- `jdbc:postgresql://127.0.0.1:5432/user_center`

如果你不想按项目目录名推导，可以显式覆盖：

数据库连接策略：

- 应用启动时初始化 PostgreSQL 连接池
- 普通 plan 不会申请数据库连接
- 带 `Connection` 的 plan 会在 route 层自动申请一个事务连接
- 该 plan 成功时提交事务，失败时回滚事务
- 请求结束后连接归还给连接池，不是直接废弃整个连接

也可以通过环境变量覆盖：

- `DB_HOST`
- `DB_PORT`
- `DB_NAME`
- `DB_USER`
- `DB_PASSWORD`
- `DB_MAX_POOL_SIZE`
- `DB_CONNECTION_TIMEOUT_MS`

## API 示例

### 健康检查

```bash
curl http://127.0.0.1:8080/api/health
```

### EchoPlanner

```bash
curl -X POST http://127.0.0.1:8080/api/EchoPlanner \
  -H 'Content-Type: application/json' \
  -d '{"message":"hello world","uppercase":true}'
```

### SaveDemoNotePlanner

这个接口演示：

- 类型系统到数据库：`NoteTitle`、`NoteBody`、`NoteStatus` 写入 PostgreSQL
- 数据库到类型系统：插入后把返回结果反序列化成 `DemoNote`

```bash
curl -X POST http://127.0.0.1:8080/api/SaveDemoNotePlanner \
  -H 'Content-Type: application/json' \
  -d '{"title":"first note","body":"stored by jdbc","status":"draft"}'
```

初始化数据表：

[NoteTable.scala](/home/yang/projects/commons/backend-sample/src/main/scala/tables/NoteTable.scala#L1) 里提供了 `initTableSql`，可以直接拿去执行；这个文件同时也保留了插入、按 ID 查询、列表查询的常用 SQL 示例。

## 日志

服务启动后可以在控制台看到：

- 服务启动日志
- 每个 API 的访问日志
- http4s 中间件输出的请求日志和响应日志
