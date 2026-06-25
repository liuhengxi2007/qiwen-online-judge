import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import type { SubmissionHackAvailability } from '@/objects/hack/response/SubmissionHackAvailability'
import type { JudgeResult } from '@/objects/submission/JudgeResult'
import type { SubmissionId } from '@/objects/submission/SubmissionId'
import { useI18n } from '@/system/i18n/use-i18n'

import { HackSubtaskButton } from './HackSubtaskButton'
import { ResultSummaryGroup } from './ResultSummaryGroup'
import { SubtaskTestcaseTable } from './SubtaskTestcaseTable'
import { resultNodeTitle } from './resultNodeTitle'

/**
 * 裁判结果卡片属性，包含结果树、提交 id 和按子任务计算出的 hack 可用性。
 */
type SubmissionJudgeResultCardProps = {
  hackAvailability: SubmissionHackAvailability | null
  judgeResult: JudgeResult
  submissionId: SubmissionId
}

/**
 * 提交裁判结果卡片，展示总结果、子任务/测试点明细，并在允许时提供 hack 指定子任务入口。
 */
export function SubmissionJudgeResultCard({ hackAvailability, judgeResult, submissionId }: SubmissionJudgeResultCardProps) {
  const { t } = useI18n()
  const singleSubtask = judgeResult.subtasks.length === 1 ? judgeResult.subtasks[0] : null
  const hackAvailabilityBySubtask = new Map(hackAvailability?.subtasks.map((subtask) => [subtask.subtaskIndex, subtask.canHack]) ?? [])
  const singleSubtaskCanHack = singleSubtask !== null && hackAvailabilityBySubtask.get(singleSubtask.index) === true

  return (
    <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
      <CardHeader className={singleSubtaskCanHack ? 'flex flex-row items-center justify-between gap-3' : undefined}>
        <CardTitle className="min-w-0 text-xl text-slate-950">{t('submission.detail.judgeResult')}</CardTitle>
        {singleSubtaskCanHack ? (
          <HackSubtaskButton
            submissionId={submissionId}
            subtaskIndex={singleSubtask.index}
            label={t('hack.action')}
            className="shrink-0"
          />
        ) : null}
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="grid gap-3 text-sm lg:grid-cols-2">
          <ResultSummaryGroup
            label={t('submission.detail.baseResult')}
            summary={judgeResult.baseResult}
            verdictLabel={t('common.verdict')}
            scoreLabel={t('submission.list.score')}
            timeLabel={t('submission.list.timeUsed')}
            memoryLabel={t('submission.list.spaceUsed')}
            reasonLabel={t('submission.detail.reason')}
          />
          <ResultSummaryGroup
            label={t('submission.detail.worstResult')}
            summary={judgeResult.worstResult}
            verdictLabel={t('common.verdict')}
            scoreLabel={t('submission.list.score')}
            timeLabel={t('submission.list.timeUsed')}
            memoryLabel={t('submission.list.spaceUsed')}
            reasonLabel={t('submission.detail.reason')}
          />
        </div>

        {singleSubtask ? (
          <div className="rounded-lg border border-slate-200">
            <SubtaskTestcaseTable subtask={singleSubtask} />
          </div>
        ) : (
          judgeResult.subtasks.map((subtask) => {
            const canHackSubtask = hackAvailabilityBySubtask.get(subtask.index) === true

            return (
              <div key={subtask.index} className="rounded-lg border border-slate-200">
                <div className="relative border-b border-slate-200 px-4 py-3">
                  <div className="min-w-0">
                    <p className="pr-24 font-medium text-slate-950">{resultNodeTitle('subtask', subtask.index, subtask.label)}</p>
                    <div className="mt-3 grid gap-3 text-sm lg:grid-cols-2">
                      <ResultSummaryGroup
                        label={t('submission.detail.baseResult')}
                        summary={subtask.baseResult}
                        verdictLabel={t('common.verdict')}
                        scoreLabel={t('submission.list.score')}
                        timeLabel={t('submission.list.timeUsed')}
                        memoryLabel={t('submission.list.spaceUsed')}
                        reasonLabel={t('submission.detail.reason')}
                      />
                      <ResultSummaryGroup
                        label={t('submission.detail.worstResult')}
                        summary={subtask.worstResult}
                        verdictLabel={t('common.verdict')}
                        scoreLabel={t('submission.list.score')}
                        timeLabel={t('submission.list.timeUsed')}
                        memoryLabel={t('submission.list.spaceUsed')}
                        reasonLabel={t('submission.detail.reason')}
                      />
                    </div>
                  </div>
                  {canHackSubtask ? (
                    <HackSubtaskButton
                      submissionId={submissionId}
                      subtaskIndex={subtask.index}
                      label={t('hack.action')}
                      className="absolute top-3 right-4 z-10"
                    />
                  ) : null}
                </div>
                <SubtaskTestcaseTable subtask={subtask} />
              </div>
            )
          })
        )}
      </CardContent>
    </Card>
  )
}
