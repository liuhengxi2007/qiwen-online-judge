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
- `JUDGER_ID_PREFIX`，默认 `local-judger`
- `POLL_INTERVAL_MS`，默认 `2000`
- `CXX`，默认 `g++`
- `PYTHON3`，默认 `python3`
- `ISOLATE_BIN`，默认 `isolate`
- `ISOLATE_BOX_ID`，默认按当前进程 PID 推导
- `ISOLATE_PREFER_CGROUPS`，默认 `true`；若环境不支持，judger 会自动退回不带 `--cg` 的 isolate

## 当前范围

当前支持 `cpp17` 和 `python3`。Python3 提交会先编译为 `main.pyc` 字节码，测试点运行阶段执行该字节码文件。

## 安全执行

Linux / WSL 下，提交代码会通过 `isolate` 进入沙箱后再编译和运行。
如果环境支持 cgroup，judger 会优先使用 `isolate --cg`；若不支持，会自动退回普通 `isolate`。
如果 `isolate` 不可用，`run-wsl.sh` 会直接拒绝启动 judger，而不会退回到裸进程执行。
