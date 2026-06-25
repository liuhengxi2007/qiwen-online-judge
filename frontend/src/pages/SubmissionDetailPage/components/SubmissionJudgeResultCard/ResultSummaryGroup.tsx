import type { CSSProperties } from 'react'

import type { JudgeResultSummary } from '@/objects/submission/JudgeResultSummary'
import { scoreTextStyleForRatio } from '@/pages/objects/ScoreDisplay'
import {
  formatOptionalDurationMs,
  formatOptionalMemoryKb,
  formatOptionalScore,
  submissionVerdictLabel,
  submissionVerdictTextStyle,
} from '@/pages/objects/SubmissionDisplay'

type ResultSummaryGroupProps = {
  label: string
  summary: JudgeResultSummary
  verdictLabel: string
  scoreLabel: string
  timeLabel: string
  memoryLabel: string
  reasonLabel: string
}

/**
 * 结果摘要分组，统一渲染基础结果或最差结果的 verdict、分数、耗时、内存和原因。
 */
export function ResultSummaryGroup({
  label,
  summary,
  verdictLabel,
  scoreLabel,
  timeLabel,
  memoryLabel,
  reasonLabel,
}: ResultSummaryGroupProps) {
  // 保留扁平 props：这是同一组摘要行的展示文案，字段数量固定且调用端全部具名。
  return (
    <div className="rounded-md border border-slate-200 px-3 py-2">
      <p className="text-xs uppercase tracking-normal text-slate-500">{label}</p>
      <div className="mt-2 grid gap-2 sm:grid-cols-4">
        <MetricValue
          label={verdictLabel}
          value={submissionVerdictLabel(summary.verdict)}
          valueStyle={submissionVerdictTextStyle(summary.verdict)}
        />
        <MetricValue label={scoreLabel} value={formatOptionalScore(summary.score)} valueStyle={scoreTextStyleForRatio(summary.score)} />
        <MetricValue label={timeLabel} value={formatOptionalDurationMs(summary.timeUsedMs)} />
        <MetricValue label={memoryLabel} value={formatOptionalMemoryKb(summary.memoryUsedKb)} />
      </div>
      <JudgeReasonLine label={reasonLabel} reason={summary.reason} />
    </div>
  )
}

/**
 * 单个结果指标展示项，可选 valueStyle 用于 verdict/分数颜色。
 */
function MetricValue({
  label,
  value,
  valueStyle,
}: {
  label: string
  value: string
  valueStyle?: CSSProperties
}) {
  return (
    <div>
      <p className="text-[0.7rem] uppercase tracking-normal text-slate-400">{label}</p>
      <p className="mt-0.5 font-medium text-slate-950" style={valueStyle}>{value}</p>
    </div>
  )
}

/**
 * 裁判原因行，仅在存在 reason 时展示，避免空原因占用布局。
 */
function JudgeReasonLine({ label, reason }: { label: string; reason: string | null }) {
  if (!reason) {
    return null
  }

  return (
    <p className="mt-1 text-xs text-slate-500">
      {label}: <span className="font-mono text-slate-700">{reason}</span>
    </p>
  )
}
