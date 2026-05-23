import { describe, expect, it } from 'vitest'

import {
  parseProblemDataFilename,
  parseProblemDataPath,
  parseProblemId,
  parseProblemSearchQuery,
  parseProblemSlug,
  parseProblemSpaceLimitMb,
  parseProblemStatementText,
  parseProblemTimeLimitMs,
  parseProblemTitle,
  problemDataFilenameValue,
  problemSlugValue,
} from '@/features/problem/lib/problem-parsers'

const problemId = '11111111-1111-4111-8111-111111111111'

describe('problem-parsers', () => {
  it('parses and normalizes problem slugs', () => {
    const parsed = parseProblemSlug('  sample-problem  ')

    expect(parsed.ok).toBe(true)
    if (parsed.ok) {
      expect(problemSlugValue(parsed.value)).toBe('sample-problem')
    }
  })

  it('rejects problem slugs across empty invalid and out-of-range cases', () => {
    expect(parseProblemSlug('   ')).toEqual({
      ok: false,
      error: 'Problem slug is required.',
    })
    expect(parseProblemSlug('ab')).toEqual({
      ok: false,
      error: 'Problem slug must be between 3 and 64 characters.',
    })
    expect(parseProblemSlug('a'.repeat(65))).toEqual({
      ok: false,
      error: 'Problem slug must be between 3 and 64 characters.',
    })
    expect(parseProblemSlug('bad slug')).toEqual({
      ok: false,
      error: 'Problem slug may contain only lowercase letters, numbers, and hyphens.',
    })
  })

  it('parses and validates problem ids', () => {
    expect(parseProblemId(` ${problemId} `)).toEqual({
      ok: true,
      value: problemId,
    })
    expect(parseProblemId('')).toEqual({
      ok: false,
      error: 'Problem id is required.',
    })
    expect(parseProblemId('not-a-uuid')).toEqual({
      ok: false,
      error: 'Problem id must be a valid UUID.',
    })
  })

  it('validates problem titles statements and data filenames at their boundaries', () => {
    expect(parseProblemTitle('   ')).toEqual({
      ok: false,
      error: 'Problem title is required.',
    })
    expect(parseProblemTitle('x'.repeat(121))).toEqual({
      ok: false,
      error: 'Problem title must be at most 120 characters.',
    })
    expect(parseProblemStatementText('   ')).toEqual({
      ok: false,
      error: 'Problem statement is required.',
    })
    expect(parseProblemStatementText('x'.repeat(20001))).toEqual({
      ok: false,
      error: 'Problem statement must be at most 20000 characters.',
    })
    const filename = parseProblemDataFilename(` ${'a'.repeat(255)} `)
    expect(filename.ok).toBe(true)
    if (filename.ok) {
      expect(problemDataFilenameValue(filename.value)).toBe('a'.repeat(255))
    }
    expect(parseProblemDataFilename('x'.repeat(256))).toEqual({
      ok: false,
      error: 'Problem data file name must be at most 255 characters.',
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

  it('rejects problem data paths with structural path errors', () => {
    expect(parseProblemDataPath('')).toEqual({
      ok: false,
      error: 'Problem data path is required.',
    })
    expect(parseProblemDataPath('/cases/1.in')).toEqual({
      ok: false,
      error: "Problem data path must be relative and must not start or end with '/'.",
    })
    expect(parseProblemDataPath('cases/1.in/')).toEqual({
      ok: false,
      error: "Problem data path must be relative and must not start or end with '/'.",
    })
    expect(parseProblemDataPath('cases//1.in')).toEqual({
      ok: false,
      error: 'Problem data path must not contain empty segments.',
    })
    expect(parseProblemDataPath(`${'a'.repeat(256)}/1.in`)).toEqual({
      ok: false,
      error: 'Each problem data path segment must be at most 255 characters.',
    })
  })

  it('parses problem search queries', () => {
    expect(parseProblemSearchQuery('  dp  ')).toEqual({
      ok: true,
      value: 'dp',
    })
  })

  it('validates numeric problem limits', () => {
    expect(parseProblemTimeLimitMs(1)).toEqual({
      ok: true,
      value: 1,
    })
    expect(parseProblemTimeLimitMs(0)).toEqual({
      ok: false,
      error: 'Problem time limit must be between 1 and 600000 ms.',
    })
    expect(parseProblemTimeLimitMs(600001)).toEqual({
      ok: false,
      error: 'Problem time limit must be between 1 and 600000 ms.',
    })
    expect(parseProblemTimeLimitMs(1.5)).toEqual({
      ok: false,
      error: 'Problem time limit must be an integer.',
    })
    expect(parseProblemSpaceLimitMb(1)).toEqual({
      ok: true,
      value: 1,
    })
    expect(parseProblemSpaceLimitMb(256)).toEqual({
      ok: true,
      value: 256,
    })
    expect(parseProblemSpaceLimitMb(65536)).toEqual({
      ok: true,
      value: 65536,
    })
    expect(parseProblemSpaceLimitMb(65537)).toEqual({
      ok: false,
      error: 'Problem space limit must be between 1 and 65536 MB.',
    })
    expect(parseProblemSpaceLimitMb(1.5)).toEqual({
      ok: false,
      error: 'Problem space limit must be an integer.',
    })
  })
})
