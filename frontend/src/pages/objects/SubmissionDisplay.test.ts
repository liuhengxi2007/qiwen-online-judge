import { describe, expect, it } from 'vitest'

import {
  submissionJudgeStateLabel,
  submissionLanguageLabel,
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
})
