# Judger

独立的 Scala 判题 worker，职责只保留这条最小闭环：

- claim 任务
- 编译并运行提交代码
- report 结果

它不直接访问数据库，只通过 backend 暴露的内部 judge API 工作。

## 运行

Windows:

```bash
run-windows.bat
```

WSL:

```bash
./run-wsl.sh
```

可配置环境变量：

- `BACKEND_BASE_URL`，默认优先自动探测，否则使用 `http://localhost:8080`
- `JUDGE_TOKEN`，默认 `dev-judge-token`
- `JUDGER_NAME`，默认 `cpp17-judger`
- `POLL_INTERVAL_MS`，默认 `2000`
- `CXX`，默认 `g++`

## 当前范围

当前只支持 `cpp17`。
