import { describe, expect, it } from 'vitest'

import {
  parseProblemSetDescription,
  parseProblemSetId,
  parseProblemSetProblemPosition,
  parseProblemSetSlug,
  parseProblemSetTitle,
  problemSetSlugValue,
} from '@/objects/problemset/problemset-parsers'

const problemSetId = '11111111-1111-4111-8111-111111111111'

describe('problemset-parsers', () => {
  it('parses trimmed problem set slugs', () => {
    const parsed = parseProblemSetSlug('  graph-set  ')

    expect(parsed.ok).toBe(true)
    if (parsed.ok) {
      expect(problemSetSlugValue(parsed.value)).toBe('graph-set')
    }
  })

  it('parses valid problem set ids and rejects malformed ids', () => {
    expect(parseProblemSetId(` ${problemSetId} `)).toEqual({
      ok: true,
      value: problemSetId,
    })
    expect(parseProblemSetId('not-a-uuid')).toEqual({
      ok: false,
      error: 'Problem set id must be a valid UUID.',
    })
  })

  it('rejects malformed and out-of-range problem set slugs', () => {
    expect(parseProblemSetSlug('  ')).toEqual({
      ok: false,
      error: 'Problem set slug is required.',
    })
    expect(parseProblemSetSlug('ab')).toEqual({
      ok: false,
      error: 'Problem set slug must be between 3 and 64 characters.',
    })
    expect(parseProblemSetSlug('a'.repeat(65))).toEqual({
      ok: false,
      error: 'Problem set slug must be between 3 and 64 characters.',
    })
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
    expect(parseProblemSetTitle('   ')).toEqual({
      ok: false,
      error: 'Problem set title is required.',
    })
    expect(parseProblemSetTitle('x'.repeat(121))).toEqual({
      ok: false,
      error: 'Problem set title must be at most 120 characters.',
    })
    expect(parseProblemSetDescription('  selected problems  ')).toEqual({
      ok: true,
      value: 'selected problems',
    })
    expect(parseProblemSetDescription('x'.repeat(2001))).toEqual({
      ok: false,
      error: 'Problem set description must be at most 2000 characters.',
    })
  })

  it('validates problem positions', () => {
    expect(parseProblemSetProblemPosition(1)).toEqual({ ok: true, value: 1 })
    expect(parseProblemSetProblemPosition(0)).toEqual({
      ok: false,
      error: 'Problem set problem position must be a positive integer.',
    })
    expect(parseProblemSetProblemPosition(-1)).toEqual({
      ok: false,
      error: 'Problem set problem position must be a positive integer.',
    })
    expect(parseProblemSetProblemPosition(1.5)).toEqual({
      ok: false,
      error: 'Problem set problem position must be a positive integer.',
    })
  })
})
