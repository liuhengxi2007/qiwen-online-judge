import { describe, expect, it } from 'vitest'

import {
  parseProblemSetDescription,
  parseProblemSetProblemPosition,
  parseProblemSetSlug,
  parseProblemSetTitle,
  problemSetSlugValue,
} from '@/features/problemset/domain/problemset-parsers'

describe('problemset-parsers', () => {
  it('parses trimmed problem set slugs', () => {
    const parsed = parseProblemSetSlug('  graph-set  ')

    expect(parsed.ok).toBe(true)
    if (parsed.ok) {
      expect(problemSetSlugValue(parsed.value)).toBe('graph-set')
    }
  })

  it('rejects malformed problem set slugs', () => {
    expect(parseProblemSetSlug('graph set')).toEqual({
      ok: false,
      error: 'Problem set slug may contain only lowercase letters, numbers, and hyphens.',
    })
  })

  it('parses titles and descriptions', () => {
    expect(parseProblemSetTitle('  Graph Practice  ')).toEqual({
      ok: true,
      value: 'Graph Practice',
    })
    expect(parseProblemSetDescription('  selected problems  ')).toEqual({
      ok: true,
      value: 'selected problems',
    })
  })

  it('validates problem positions', () => {
    expect(parseProblemSetProblemPosition(0)).toEqual({
      ok: false,
      error: 'Problem set problem position must be a positive integer.',
    })
  })
})
