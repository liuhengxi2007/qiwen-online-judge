import { describe, expect, it } from 'vitest'

import { formatContestScore, type ContestScore } from '@/objects/contest/ContestScore'

describe('contest-score', () => {
  it('formats score ratios as contest points', () => {
    expect(formatContestScore(0 as ContestScore)).toBe('0')
    expect(formatContestScore(0.5 as ContestScore)).toBe('50')
    expect(formatContestScore(1 as ContestScore)).toBe('100')
    expect(formatContestScore(2.3456 as ContestScore)).toBe('234.56')
  })
})
