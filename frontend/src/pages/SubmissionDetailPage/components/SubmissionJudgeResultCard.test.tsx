import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'

import { SubmissionJudgeResultCard } from './SubmissionJudgeResultCard'
import type { JudgeResult } from '@/objects/submission/JudgeResult'
import type { SubmissionId } from '@/objects/submission/SubmissionId'

vi.mock('@/system/i18n/use-i18n', () => ({
  useI18n: () => ({
    t: (key: string) => ({
      'common.verdict': 'Verdict',
      'submission.detail.baseResult': 'Base result',
      'submission.detail.judgeResult': 'Judge result',
      'submission.detail.noTestcases': 'No testcases',
      'submission.detail.reason': 'Reason',
      'submission.detail.testcaseDetail': 'Detail',
      'submission.detail.testcases': 'Testcases',
      'submission.detail.worstResult': 'Worst result',
      'submission.list.score': 'Score',
      'submission.list.spaceUsed': 'Memory',
      'submission.list.timeUsed': 'Time',
      'hack.action': 'Hack',
    }[key] ?? key),
  }),
}))

describe('SubmissionJudgeResultCard', () => {
  it('renders owned base and worst summaries without duplicate node summaries', () => {
    const judgeResult: JudgeResult = {
      baseResult: {
        score: 0,
        verdict: 'wrong_answer',
        reason: null,
        timeUsedMs: 30,
        memoryUsedKb: 3460,
      },
      worstResult: {
        score: 0,
        verdict: 'system_error',
        reason: 'checker_runtime_failed',
        timeUsedMs: 60,
        memoryUsedKb: 4096,
      },
      subtasks: [
        {
          index: 1,
          label: 'main',
          baseResult: {
            score: 0,
            verdict: 'wrong_answer',
            reason: null,
            timeUsedMs: 30,
            memoryUsedKb: 3460,
          },
          worstResult: {
            score: 0,
            verdict: 'wrong_answer',
            reason: null,
            timeUsedMs: 60,
            memoryUsedKb: 4096,
          },
          testcases: [
            {
              index: 1,
              label: '1',
              testcaseType: 'main',
              score: 0,
              verdict: 'wrong_answer',
              message: 'checker report',
              reason: null,
              timeUsedMs: 30,
              memoryUsedKb: 3460,
            },
          ],
        },
      ],
    }

    const { container } = render(
      <MemoryRouter>
        <SubmissionJudgeResultCard judgeResult={judgeResult} submissionId={1 as SubmissionId} />
      </MemoryRouter>,
    )

    expect(screen.getAllByText('Base result')).toHaveLength(2)
    expect(screen.getAllByText('Worst result')).toHaveLength(2)
    expect(screen.getAllByText('Verdict').length).toBeGreaterThan(1)
    expect(screen.getByText('System Error')).toBeTruthy()
    expect(screen.getByText('checker_runtime_failed')).toBeTruthy()
    expect(container.textContent).not.toContain('Wrong Answer · 0')
    expect(container.textContent).not.toContain('30 ms · 3.38 MiB')
  })
})
