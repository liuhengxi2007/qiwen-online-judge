import { describe, expect, it } from 'vitest'

import type { ContestSlug } from '@/objects/contest/ContestSlug'
import type { ContestTitle } from '@/objects/contest/ContestTitle'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import {
  clampScoreRatio,
  scoreHueForRatio,
  scorePillStyleForRatio,
  scoreTextStyleForRatio,
} from './ScoreDisplay'
import {
  submissionProblemPath,
  submissionJudgeStateLabel,
  submissionLanguageLabel,
  submissionResultLabel,
  submissionResultTextStyle,
  submissionStatusLabel,
  submissionVerdictTextStyle,
  submissionVerdictLabel,
} from './SubmissionDisplay'

describe('submission-display', () => {
  it('formats verdict status language and judge-state labels', () => {
    expect(submissionLanguageLabel('cpp17')).toBe('C++17')
    expect(submissionLanguageLabel('python3')).toBe('Python 3')
    expect(submissionStatusLabel('queued')).toBe('Queued')
    expect(submissionStatusLabel('failed')).toBe('Failed')
    expect(submissionVerdictLabel('wrong_answer')).toBe('Wrong Answer')
    expect(submissionJudgeStateLabel('running', null)).toBe('Pending')
    expect(submissionJudgeStateLabel('failed', null)).toBe('Failed')
    expect(submissionJudgeStateLabel('completed', 'accepted')).toBe('Accepted')
  })

  it('formats result labels from the display mode', () => {
    expect(submissionResultLabel('verdict', 'completed', 'accepted', 0.5)).toBe('Accepted')
    expect(submissionResultLabel('score', 'completed', 'accepted', 0.5)).toBe('50')
    expect(submissionResultLabel('score', 'running', null, null)).toBe('--')
  })

  it('formats score styles from clamped ratios', () => {
    expect(clampScoreRatio(-0.1)).toBe(0)
    expect(clampScoreRatio(1.1)).toBe(1)
    expect(clampScoreRatio(Number.NaN)).toBe(0)
    expect(scoreHueForRatio(0)).toBe(0)
    expect(scoreHueForRatio(0.5)).toBe(58)
    expect(scoreHueForRatio(1)).toBe(115)
    expect(scoreTextStyleForRatio(0)).toEqual({ color: 'hsl(0, 72%, 34%)' })
    expect(scoreTextStyleForRatio(0.5)).toEqual({ color: 'hsl(58, 72%, 34%)' })
    expect(scoreTextStyleForRatio(1)).toEqual({ color: 'hsl(115, 72%, 34%)' })
    expect(scorePillStyleForRatio(0)).toEqual({
      backgroundColor: 'hsla(0, 82%, 92%, 0.9)',
      color: 'hsl(0, 72%, 34%)',
    })
    expect(scorePillStyleForRatio(0.5)).toEqual({
      backgroundColor: 'hsla(58, 82%, 92%, 0.9)',
      color: 'hsl(58, 72%, 34%)',
    })
    expect(scorePillStyleForRatio(1)).toEqual({
      backgroundColor: 'hsla(115, 82%, 92%, 0.9)',
      color: 'hsl(115, 72%, 34%)',
    })
  })

  it('formats verdict and result text styles', () => {
    expect(submissionVerdictTextStyle(null)).toEqual({ color: '#94A3B8' })
    expect(submissionVerdictTextStyle('accepted')).toEqual({ color: '#1B7837' })
    expect(submissionVerdictTextStyle('accepted_by_protocol')).toEqual({ color: '#1B7837' })
    expect(submissionVerdictTextStyle('wrong_answer')).toEqual({ color: '#B2182B' })
    expect(submissionVerdictTextStyle('compile_error')).toEqual({ color: '#64748B' })
    expect(submissionVerdictTextStyle('time_limit_exceeded')).toEqual({ color: '#B99024' })
    expect(submissionVerdictTextStyle('runtime_error')).toEqual({ color: '#7B3294' })
    expect(submissionVerdictTextStyle('system_error')).toEqual({ color: '#2166AC' })
    expect(submissionResultTextStyle('verdict', 'wrong_answer', 0.5)).toEqual({ color: '#B2182B' })
    expect(submissionResultTextStyle('score', 'wrong_answer', 0.5)).toEqual({
      color: 'hsl(58, 72%, 34%)',
    })
  })

  it('builds source-aware problem paths', () => {
    expect(
      submissionProblemPath(
        { contestSlug: null, contestTitle: null },
        'sample-problem' as ProblemSlug,
      ),
    ).toBe('/problems/sample-problem')
    expect(
      submissionProblemPath(
        {
          contestSlug: 'sample-contest' as ContestSlug,
          contestTitle: 'Sample Contest' as ContestTitle,
        },
        'sample-problem' as ProblemSlug,
      ),
    ).toBe('/contests/sample-contest/problems/sample-problem')
  })
})
