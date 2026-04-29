import { describe, expect, it } from 'vitest'

import {
  fromProblemDataContract,
  parseProblemDataPath,
  parseProblemSearchQuery,
  parseProblemSlug,
  parseProblemSpaceLimitMb,
  parseProblemTimeLimitMs,
  problemSlugValue,
} from '@/features/problem/domain/problem-parsers'

describe('problem-parsers', () => {
  it('parses and normalizes problem slugs', () => {
    const parsed = parseProblemSlug('  sample-problem  ')

    expect(parsed.ok).toBe(true)
    if (parsed.ok) {
      expect(problemSlugValue(parsed.value)).toBe('sample-problem')
    }
  })

  it('rejects problem slugs with invalid characters', () => {
    expect(parseProblemSlug('bad slug')).toEqual({
      ok: false,
      error: 'Problem slug may contain only lowercase letters, numbers, and hyphens.',
    })
  })

  it('normalizes windows-style problem data paths', () => {
    expect(parseProblemDataPath('cases\\sample\\1.in')).toEqual({
      ok: true,
      value: 'cases/sample/1.in',
    })
  })

  it('rejects problem data paths with parent segments', () => {
    expect(parseProblemDataPath('../secret.out')).toEqual({
      ok: false,
      error: "Problem data path must not contain '.' or '..' segments.",
    })
  })

  it('parses problem search queries and nullable contract data', () => {
    expect(parseProblemSearchQuery('  dp  ')).toEqual({
      ok: true,
      value: 'dp',
    })
    expect(fromProblemDataContract(null, 'problem data')).toEqual({ value: null })
  })

  it('validates numeric problem limits', () => {
    expect(parseProblemTimeLimitMs(0)).toEqual({
      ok: false,
      error: 'Problem time limit must be between 1 and 600000 ms.',
    })
    expect(parseProblemSpaceLimitMb(256)).toEqual({
      ok: true,
      value: 256,
    })
  })
})
