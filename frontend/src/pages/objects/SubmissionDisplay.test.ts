import { describe, expect, it } from 'vitest'

import type { ContestSlug } from '@/objects/contest/ContestSlug'
import type { ContestTitle } from '@/objects/contest/ContestTitle'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import {
  submissionProblemPath,
  submissionJudgeStateLabel,
  submissionLanguageLabel,
  submissionResultLabel,
  submissionStatusLabel,
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
