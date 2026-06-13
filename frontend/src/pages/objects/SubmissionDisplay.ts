import type { CSSProperties } from 'react'

import { contestSlugValue } from '@/objects/contest/ContestSlug'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import { problemSlugValue } from '@/objects/problem/ProblemSlug'
import type { SubmissionLanguage } from '@/objects/submission/SubmissionLanguage'
import type { SubmissionResultDisplayMode } from '@/objects/submission/SubmissionResultDisplayMode'
import type { SubmissionSource } from '@/objects/submission/SubmissionSource'
import type { SubmissionStatus } from '@/objects/submission/SubmissionStatus'
import { isTerminalSubmissionStatus } from '@/objects/submission/SubmissionStatus'
import type { SubmissionVerdict } from '@/objects/submission/SubmissionVerdict'
import type { SubmissionVerdictFilter } from '@/objects/submission/request/SubmissionVerdictFilter'
import { scoreTextStyleForRatio } from '@/pages/objects/ScoreDisplay'
import { formatOptionalBinarySizeBytes } from '@/system/format/binary-size'
import { formatDateTime, formatUtcOffsetTitle } from '@/system/format/date-time'

const testcaseDetailMessageLimit = 125

/**
 * 格式化可缺失的运行耗时；null 表示尚无耗时数据。
 */
export function formatOptionalDurationMs(value: number | null): string {
  if (value === null) {
    return '--'
  }

  return `${value} ms`
}

/**
 * 格式化可缺失的内存用量，后端以 KiB 计数，展示层转换为二进制大小文案。
 */
export function formatOptionalMemoryKb(value: number | null): string {
  return formatOptionalBinarySizeBytes(value === null ? null : value * 1024, '--', { minimumUnit: 'KiB' })
}

/**
 * 格式化源代码长度，输入为字节数，当前展示保持 B 单位。
 */
export function formatCodeLength(value: number): string {
  return `${value} B`
}

/**
 * 格式化可缺失的得分比例；非空值按百分制数值展示，null 返回占位符。
 */
export function formatOptionalScore(value: number | null): string {
  if (value === null) {
    return '--'
  }

  return `${new Intl.NumberFormat(undefined, {
    maximumFractionDigits: 6,
  }).format(value * 100)}`
}

/**
 * 提交时间展示格式化函数，复用系统本地时间格式。
 */
export const formatSubmissionDateTime = formatDateTime

/**
 * 提交时间标题格式化函数，显示当前环境的 UTC 偏移。
 */
export const formatSubmissionDateTimeTitle = formatUtcOffsetTitle

/**
 * 将提交语言枚举转换为用户可读标签。
 */
export function submissionLanguageLabel(language: SubmissionLanguage): string {
  switch (language) {
    case 'cpp17':
      return 'C++17'
    case 'python3':
      return 'Python 3'
    case 'text':
      return 'Text'
  }
}

/**
 * 将提交生命周期状态转换为用户可读标签。
 */
export function submissionStatusLabel(status: SubmissionStatus): string {
  switch (status) {
    case 'queued':
      return 'Queued'
    case 'running':
      return 'Running'
    case 'completed':
      return 'Completed'
    case 'failed':
      return 'Failed'
  }
}

/**
 * 将判题结果转换为用户可读标签；null 表示尚未产出 verdict。
 */
export function submissionVerdictLabel(verdict: SubmissionVerdict | null): string {
  switch (verdict) {
    case null:
      return 'Pending'
    case 'accepted':
      return 'Accepted'
    case 'accepted_by_protocol':
      return 'Accepted by Protocol'
    case 'wrong_answer':
      return 'Wrong Answer'
    case 'compile_error':
      return 'Compile Error'
    case 'runtime_error':
      return 'Runtime Error'
    case 'time_limit_exceeded':
      return 'Time Limit Exceeded'
    case 'idleness_limit_exceeded':
      return 'Idleness Limit Exceeded'
    case 'system_error':
      return 'System Error'
  }
}

/**
 * 返回判题结果对应的文字颜色样式，供列表和详情保持一致。
 */
export function submissionVerdictTextStyle(verdict: SubmissionVerdict | null): CSSProperties {
  switch (verdict) {
    case null:
      return { color: '#94A3B8' }
    case 'accepted':
    case 'accepted_by_protocol':
      return { color: '#1B7837' }
    case 'wrong_answer':
      return { color: '#B2182B' }
    case 'compile_error':
      return { color: '#64748B' }
    case 'time_limit_exceeded':
    case 'idleness_limit_exceeded':
      return { color: '#B99024' }
    case 'runtime_error':
      return { color: '#7B3294' }
    case 'system_error':
      return { color: '#2166AC' }
  }
}

/**
 * 综合提交状态和 verdict 生成判题状态标签；verdict 优先于生命周期状态。
 */
export function submissionJudgeStateLabel(
  status: SubmissionStatus,
  verdict: SubmissionVerdict | null,
): string {
  if (verdict !== null) {
    return submissionVerdictLabel(verdict)
  }

  switch (status) {
    case 'queued':
      return 'Waiting'
    case 'running':
      return 'Judging'
    case 'completed':
    case 'failed':
      return submissionStatusLabel(status)
  }
}

/**
 * 综合提交状态和 verdict 生成判题状态颜色；verdict 优先于生命周期状态。
 */
export function submissionJudgeStateTextStyle(
  status: SubmissionStatus,
  verdict: SubmissionVerdict | null,
): CSSProperties {
  if (verdict !== null) {
    return submissionVerdictTextStyle(verdict)
  }

  switch (status) {
    case 'queued':
      return { color: '#94A3B8' }
    case 'running':
      return { color: '#94A3B8' }
    case 'completed':
      return { color: '#64748B' }
    case 'failed':
      return { color: '#B2182B' }
  }
}

/**
 * 为正在运行且尚无 verdict 的提交返回动画类名，终态提交不产生动画。
 */
export function submissionResultMotionClassName(
  status: SubmissionStatus,
  verdict: SubmissionVerdict | null,
): string | undefined {
  return status === 'running' && verdict === null ? 'motion-safe:animate-pulse' : undefined
}

/**
 * 根据提交来源生成题目链接；比赛提交会落到比赛内题目路径。
 */
export function submissionProblemPath(source: SubmissionSource, problemSlug: ProblemSlug): string {
  const problemPath = `/problems/${problemSlugValue(problemSlug)}`
  return source.contestSlug
    ? `/contests/${contestSlugValue(source.contestSlug)}/problems/${problemSlugValue(problemSlug)}`
    : problemPath
}

/**
 * 根据结果展示模式生成提交结果文本，非终态提交始终展示判题状态。
 */
export function submissionResultLabel(
  mode: SubmissionResultDisplayMode,
  status: SubmissionStatus,
  verdict: SubmissionVerdict | null,
  score: number | null,
): string {
  if (!isTerminalSubmissionStatus(status) && verdict === null) {
    return submissionJudgeStateLabel(status, verdict)
  }

  switch (mode) {
    case 'verdict':
      return submissionJudgeStateLabel(status, verdict)
    case 'score':
      return formatOptionalScore(score)
  }
}

/**
 * 根据结果展示模式生成提交结果样式，分数模式按得分比例映射颜色。
 */
export function submissionResultTextStyle(
  mode: SubmissionResultDisplayMode,
  status: SubmissionStatus,
  verdict: SubmissionVerdict | null,
  score: number | null,
): CSSProperties {
  if (!isTerminalSubmissionStatus(status) && verdict === null) {
    return submissionJudgeStateTextStyle(status, verdict)
  }

  switch (mode) {
    case 'verdict':
      return submissionJudgeStateTextStyle(status, verdict)
    case 'score':
      return scoreTextStyleForRatio(score ?? Number.NaN)
  }
}

/**
 * 格式化测试点详情，优先展示 reason，其次展示裁剪后的 message，缺失时返回占位符。
 */
export function formatTestcaseDetail(reason: string | null, message: string | null): string {
  if (reason !== null) {
    return reason
  }
  if (message === null) {
    return '--'
  }
  if (message.length <= testcaseDetailMessageLimit) {
    return message
  }
  return `${message.slice(0, testcaseDetailMessageLimit)}...`
}

/**
 * 将提交筛选用 verdict 值转换为下拉标签，特殊处理 all 和 pending。
 */
export function verdictFilterLabel(verdict: SubmissionVerdictFilter, allVerdictsLabel: string): string {
  if (verdict === 'all') {
    return allVerdictsLabel
  }
  if (verdict === 'pending') {
    return submissionVerdictLabel(null)
  }
  return submissionVerdictLabel(verdict)
}
