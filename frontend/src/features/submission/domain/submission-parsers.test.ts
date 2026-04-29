import { describe, expect, it } from 'vitest'

import {
  isSubmissionLanguage,
  isSubmissionVerdictFilter,
  parseSubmissionId,
  parseSubmissionProblemQuery,
  parseSubmissionSourceCode,
  submissionJudgeStateLabel,
  submissionVerdictLabel,
} from '@/features/submission/domain/submission-parsers'

describe('submission-parsers', () => {
  it('recognizes supported submission enum values', () => {
    expect(isSubmissionLanguage('cpp17')).toBe(true)
    expect(isSubmissionLanguage('java')).toBe(false)
    expect(isSubmissionVerdictFilter('pending')).toBe(true)
  })

  it('validates submission ids and source code', () => {
    expect(parseSubmissionId(0)).toEqual({
      ok: false,
      error: 'Submission id is required.',
    })
    expect(parseSubmissionSourceCode('')).toEqual({
      ok: false,
      error: 'Source code is required.',
    })
  })

  it('parses trimmed submission problem queries', () => {
    expect(parseSubmissionProblemQuery('  two-sum  ')).toEqual({
      ok: true,
      value: 'two-sum',
    })
  })

  it('formats verdict and judge-state labels', () => {
    expect(submissionVerdictLabel('wrong_answer')).toBe('Wrong Answer')
    expect(submissionJudgeStateLabel('running', null)).toBe('Pending')
    expect(submissionJudgeStateLabel('completed', 'accepted')).toBe('Accepted')
  })
})
