import { describe, expect, it } from 'vitest'

import { isJudgeFailureReason } from '@/objects/submission/JudgeFailureReason'
import { isSubmissionLanguage } from '@/objects/submission/SubmissionLanguage'
import { parseSubmissionId } from '@/objects/submission/SubmissionId'
import { parseSubmissionSourceCode } from '@/objects/submission/SubmissionSourceCode'
import { isSubmissionStatus } from '@/objects/submission/SubmissionStatus'
import { isSubmissionVerdict } from '@/objects/submission/SubmissionVerdict'
import { parseSubmissionProblemQuery } from '@/objects/submission/request/SubmissionProblemQuery'
import { isSubmissionSort } from '@/objects/submission/request/SubmissionSort'
import { isSubmissionSortDirection } from '@/objects/submission/request/SubmissionSortDirection'
import { parseSubmissionUserQuery } from '@/objects/submission/request/SubmissionUserQuery'
import { isSubmissionVerdictFilter } from '@/objects/submission/request/SubmissionVerdictFilter'

describe('submission-parsers', () => {
  it('recognizes supported submission enum values', () => {
    expect(isSubmissionLanguage('cpp17')).toBe(true)
    expect(isSubmissionLanguage('python3')).toBe(true)
    expect(isSubmissionLanguage('java')).toBe(false)
    expect(isSubmissionStatus('queued')).toBe(true)
    expect(isSubmissionStatus('finished')).toBe(false)
    expect(isSubmissionVerdict('accepted')).toBe(true)
    expect(isSubmissionVerdict('accepted_by_protocol')).toBe(true)
    expect(isSubmissionVerdict('idleness_limit_exceeded')).toBe(true)
    expect(isSubmissionVerdict('pending')).toBe(false)
    expect(isJudgeFailureReason('checker_runtime_failed')).toBe(true)
    expect(isJudgeFailureReason('wrong_answer')).toBe(false)
    expect(isSubmissionVerdictFilter('pending')).toBe(true)
    expect(isSubmissionVerdictFilter('accepted_by_protocol')).toBe(true)
    expect(isSubmissionVerdictFilter('idleness_limit_exceeded')).toBe(true)
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
    expect(parseSubmissionId(Number.MAX_SAFE_INTEGER + 1)).toEqual({
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
})
