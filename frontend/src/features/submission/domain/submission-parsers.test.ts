import { describe, expect, it } from 'vitest'

import {
  isSubmissionLanguage,
  isSubmissionSort,
  isSubmissionSortDirection,
  isSubmissionStatus,
  isSubmissionVerdict,
  isSubmissionVerdictFilter,
  parseSubmissionId,
  parseSubmissionProblemQuery,
  parseSubmissionSourceCode,
  parseSubmissionUserQuery,
  submissionLanguageLabel,
  submissionJudgeStateLabel,
  submissionStatusLabel,
  submissionVerdictLabel,
} from '@/features/submission/domain/submission-parsers'

describe('submission-parsers', () => {
  it('recognizes supported submission enum values', () => {
    expect(isSubmissionLanguage('cpp17')).toBe(true)
    expect(isSubmissionLanguage('python3')).toBe(true)
    expect(isSubmissionLanguage('java')).toBe(false)
    expect(isSubmissionStatus('queued')).toBe(true)
    expect(isSubmissionStatus('finished')).toBe(false)
    expect(isSubmissionVerdict('accepted')).toBe(true)
    expect(isSubmissionVerdict('pending')).toBe(false)
    expect(isSubmissionVerdictFilter('pending')).toBe(true)
    expect(isSubmissionVerdictFilter('mystery')).toBe(false)
    expect(isSubmissionSort('time')).toBe(true)
    expect(isSubmissionSort('speed')).toBe(false)
    expect(isSubmissionSortDirection('asc')).toBe(true)
    expect(isSubmissionSortDirection('sideways')).toBe(false)
  })

  it('validates submission ids and source code', () => {
    expect(parseSubmissionId(1.5)).toEqual({
      ok: false,
      error: 'Submission id must be an integer.',
    })
    expect(parseSubmissionId(0)).toEqual({
      ok: false,
      error: 'Submission id is required.',
    })
    expect(parseSubmissionId(-1)).toEqual({
      ok: false,
      error: 'Submission id is required.',
    })
    expect(parseSubmissionSourceCode('')).toEqual({
      ok: false,
      error: 'Source code is required.',
    })
    expect(parseSubmissionSourceCode('x'.repeat(200001))).toEqual({
      ok: false,
      error: 'Source code must be at most 200000 characters.',
    })
    expect(parseSubmissionSourceCode('x'.repeat(200000))).toEqual({
      ok: true,
      value: 'x'.repeat(200000),
    })
  })

  it('parses trimmed submission queries and rejects blank ones', () => {
    expect(parseSubmissionProblemQuery('  two-sum  ')).toEqual({
      ok: true,
      value: 'two-sum',
    })
    expect(parseSubmissionProblemQuery('   ')).toEqual({
      ok: false,
      error: 'Submission problem query is required.',
    })
    expect(parseSubmissionUserQuery('  alice  ')).toEqual({
      ok: true,
      value: 'alice',
    })
    expect(parseSubmissionUserQuery('   ')).toEqual({
      ok: false,
      error: 'Submission username query is required.',
    })
  })

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
})
