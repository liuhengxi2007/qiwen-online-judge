import { mkdtemp, rm, writeFile } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { spawn } from 'node:child_process'

import type { ClaimJudgeTaskRequest, JudgeTask, ReportJudgeResultRequest } from './protocol.js'

const config = {
  backendBaseUrl: process.env.BACKEND_BASE_URL?.trim() || 'http://127.0.0.1:8080',
  judgeToken: process.env.JUDGE_TOKEN?.trim() || 'dev-judge-token',
  judgerName: process.env.JUDGER_NAME?.trim() || 'cpp17-local-judger',
  pollIntervalMs: Number(process.env.POLL_INTERVAL_MS || '2000'),
  cxx: process.env.CXX?.trim() || 'g++',
}

function sleep(ms: number) {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

function normalizeOutput(value: string): string {
  return value.replace(/\r\n/g, '\n').replace(/\r/g, '\n').trimEnd()
}

async function requestJson<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${config.backendBaseUrl}${path}`, {
    ...init,
    headers: {
      'Content-Type': 'application/json',
      'x-judge-token': config.judgeToken,
      ...(init?.headers ?? {}),
    },
  })

  if (!response.ok) {
    const message = (await response.text().catch(() => '')).trim()
    throw new Error(message || `Request failed with HTTP ${response.status}.`)
  }

  return (await response.json()) as T
}

async function claimTask(): Promise<JudgeTask | null> {
  const body: ClaimJudgeTaskRequest = { judgerName: config.judgerName }
  const response = await fetch(`${config.backendBaseUrl}/api/internal/judge/claim`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'x-judge-token': config.judgeToken,
    },
    body: JSON.stringify(body),
  })

  if (response.status === 204) {
    return null
  }

  if (!response.ok) {
    const message = (await response.text().catch(() => '')).trim()
    throw new Error(message || `Claim failed with HTTP ${response.status}.`)
  }

  return (await response.json()) as JudgeTask
}

async function reportResult(submissionId: number, result: ReportJudgeResultRequest): Promise<void> {
  await requestJson(`/api/internal/judge/submissions/${submissionId}/complete`, {
    method: 'POST',
    body: JSON.stringify(result),
  })
}

function runProcess(
  command: string,
  args: string[],
  options: {
    cwd: string
    stdin?: Buffer
    timeoutMs?: number
  },
): Promise<{ exitCode: number | null; stdout: string; stderr: string; timedOut: boolean }> {
  return new Promise((resolve, reject) => {
    const child = spawn(command, args, {
      cwd: options.cwd,
      stdio: 'pipe',
      windowsHide: true,
    })

    const stdoutChunks: Buffer[] = []
    const stderrChunks: Buffer[] = []
    let timedOut = false

    const timeout =
      options.timeoutMs && options.timeoutMs > 0
        ? setTimeout(() => {
            timedOut = true
            child.kill()
          }, options.timeoutMs)
        : null

    child.stdout.on('data', (chunk: Buffer) => stdoutChunks.push(chunk))
    child.stderr.on('data', (chunk: Buffer) => stderrChunks.push(chunk))
    child.on('error', reject)
    child.on('close', (exitCode) => {
      if (timeout) {
        clearTimeout(timeout)
      }

      resolve({
        exitCode,
        stdout: Buffer.concat(stdoutChunks).toString('utf8'),
        stderr: Buffer.concat(stderrChunks).toString('utf8'),
        timedOut,
      })
    })

    if (options.stdin) {
      child.stdin.write(options.stdin)
    }
    child.stdin.end()
  })
}

async function judgeCpp17Task(task: JudgeTask): Promise<ReportJudgeResultRequest> {
  const workingDirectory = await mkdtemp(join(tmpdir(), 'qiwen-judger-'))

  try {
    const sourceFilename = 'main.cpp'
    const executableFilename = process.platform === 'win32' ? 'main.exe' : 'main'
    const sourcePath = join(workingDirectory, sourceFilename)
    const executablePath = join(workingDirectory, executableFilename)

    await writeFile(sourcePath, task.sourceCode, 'utf8')

    const compileResult = await runProcess(config.cxx, [sourceFilename, '-o', 'main', '-O2', '-std=c++17'], {
      cwd: workingDirectory,
      timeoutMs: Math.max(task.timeLimitMs * 5, 10000),
    })

    if (compileResult.timedOut) {
      return {
        status: 'failed',
        verdict: 'system_error',
        judgeMessage: 'Compilation timed out on the judger machine.',
      }
    }

    if (compileResult.exitCode !== 0) {
      return {
        status: 'completed',
        verdict: 'compile_error',
        judgeMessage: compileResult.stderr || compileResult.stdout || 'Compilation failed.',
      }
    }

    for (const testcase of task.testcases) {
      const input = Buffer.from(testcase.inputBase64, 'base64')
      const expectedOutput = Buffer.from(testcase.expectedOutputBase64, 'base64').toString('utf8')

      const runResult = await runProcess(executablePath, [], {
        cwd: workingDirectory,
        stdin: input,
        timeoutMs: Math.max(task.timeLimitMs, 1),
      })

      if (runResult.timedOut) {
        return {
          status: 'completed',
          verdict: 'time_limit_exceeded',
          judgeMessage: `Time limit exceeded on testcase ${testcase.name}.`,
        }
      }

      if (runResult.exitCode !== 0) {
        return {
          status: 'completed',
          verdict: 'runtime_error',
          judgeMessage: runResult.stderr || `Runtime error on testcase ${testcase.name}.`,
        }
      }

      if (normalizeOutput(runResult.stdout) !== normalizeOutput(expectedOutput)) {
        return {
          status: 'completed',
          verdict: 'wrong_answer',
          judgeMessage: `Wrong answer on testcase ${testcase.name}.`,
        }
      }
    }

    return {
      status: 'completed',
      verdict: 'accepted',
      judgeMessage: `Accepted by ${config.judgerName}.`,
    }
  } catch (error) {
    const message = error instanceof Error ? error.message : 'Unknown judger error.'
    return {
      status: 'failed',
      verdict: 'system_error',
      judgeMessage: message,
    }
  } finally {
    await rm(workingDirectory, { recursive: true, force: true }).catch(() => undefined)
  }
}

async function processOnce(): Promise<void> {
  const task = await claimTask()
  if (!task) {
    return
  }

  console.log(`[judger] Claimed submission #${task.submissionId} (${task.language}) for problem ${task.problemSlug}.`)

  if (task.language !== 'cpp17') {
    await reportResult(task.submissionId, {
      status: 'failed',
      verdict: 'system_error',
      judgeMessage: `Unsupported language on this judger: ${task.language}.`,
    })
    return
  }

  const result = await judgeCpp17Task(task)
  await reportResult(task.submissionId, result)
  console.log(
    `[judger] Finished submission #${task.submissionId} with status=${result.status}, verdict=${result.verdict ?? 'pending'}.`,
  )
}

async function main(): Promise<void> {
  // eslint-disable-next-line no-constant-condition
  while (true) {
    try {
      await processOnce()
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error)
      console.error(`[judger] ${message}`)
    }

    await sleep(config.pollIntervalMs)
  }
}

void main()
