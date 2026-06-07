import { describe, expect, it } from 'vitest'

import {
  buildSubmissionPayload,
  buildSubmissionPrograms,
  hasSubmissionDraftSource,
  isTextSubmissionRole,
  isValidSubmissionRole,
  normalizeSubmitProgramDraft,
  type SubmitProgramDraft,
} from './SubmitPrograms'
import { parseProblemSlug } from '@/objects/problem/ProblemSlug'
import { submissionSourceCodeValue } from '@/objects/submission/SubmissionSourceCode'

const problemSlugResult = parseProblemSlug('two-sum')
if (!problemSlugResult.ok) {
  throw new Error(problemSlugResult.error)
}
const problemSlug = problemSlugResult.value

const baseProgram: SubmitProgramDraft = {
  id: 'main',
  role: 'main',
  language: 'cpp17',
  sourceMode: 'paste',
  sourceCode: 'int main() {}\n',
  sourceFile: null,
}

describe('submit-programs', () => {
  it('validates text role shape', () => {
    expect(isValidSubmissionRole('main')).toBe(true)
    expect(isValidSubmissionRole('main.txt')).toBe(true)
    expect(isTextSubmissionRole('main.txt')).toBe(true)
    expect(isValidSubmissionRole('a.b.txt')).toBe(false)
    expect(isValidSubmissionRole('dir/main.txt')).toBe(false)
    expect(isValidSubmissionRole('main.out')).toBe(false)
  })

  it('normalizes txt roles to text and moves non-txt roles away from text', () => {
    expect(normalizeSubmitProgramDraft(baseProgram, { role: 'chain.txt' }).language).toBe('text')
    expect(normalizeSubmitProgramDraft({ ...baseProgram, role: 'chain.txt', language: 'text' }, { role: 'main' }).language).toBe('cpp17')
  })

  it('builds programs payload for text answers', () => {
    const result = buildSubmissionPrograms([
      baseProgram,
      {
        id: 'chain',
        role: 'chain.txt',
        language: 'text',
        sourceMode: 'paste',
        sourceCode: '42\n',
        sourceFile: null,
      },
    ])

    expect(result.ok).toBe(true)
    if (result.ok) {
      expect(result.programs['chain.txt'].language).toBe('text')
      expect(submissionSourceCodeValue(result.programs['chain.txt'].sourceCode)).toBe('42\n')
    }
  })

  it('rejects role and language mismatches', () => {
    expect(buildSubmissionPrograms([{ ...baseProgram, role: 'chain.txt', language: 'cpp17' }]).ok).toBe(false)
    expect(buildSubmissionPrograms([{ ...baseProgram, role: 'main', language: 'text' }]).ok).toBe(false)
  })

  it('builds JSON payload when all sources are pasted', () => {
    const result = buildSubmissionPayload(problemSlug, [baseProgram])

    expect(result.ok).toBe(true)
    if (result.ok) {
      expect(result.payload.kind).toBe('json')
      if (result.payload.kind === 'json') {
        expect(submissionSourceCodeValue(result.payload.request.programs.main.sourceCode)).toBe('int main() {}\n')
      }
    }
  })

  it('builds multipart payload when any source uses file mode', () => {
    const file = new File(['print("ok")\n'], 'main.py', { type: 'text/plain' })
    const result = buildSubmissionPayload(problemSlug, [
      {
        ...baseProgram,
        language: 'python3',
        sourceMode: 'file',
        sourceCode: '',
        sourceFile: file,
      },
      {
        id: 'notes',
        role: 'notes.txt',
        language: 'text',
        sourceMode: 'paste',
        sourceCode: '42\n',
        sourceFile: null,
      },
    ])

    expect(result.ok).toBe(true)
    if (result.ok) {
      expect(result.payload.kind).toBe('multipart')
      if (result.payload.kind === 'multipart') {
        expect(result.payload.request.programs).toEqual([
          { role: 'main', language: 'python3', sourcePart: 'source-1' },
          { role: 'notes.txt', language: 'text', sourcePart: 'source-2' },
        ])
        expect(result.payload.request.sources[0]).toEqual({ sourcePart: 'source-1', source: file })
        expect(result.payload.request.sources[1]).toEqual({ sourcePart: 'source-2', source: '42\n' })
      }
    }
  })

  it('tracks whether a draft has usable source for its mode', () => {
    expect(hasSubmissionDraftSource(baseProgram)).toBe(true)
    expect(hasSubmissionDraftSource({ ...baseProgram, sourceCode: '   ' })).toBe(false)
    expect(hasSubmissionDraftSource({ ...baseProgram, sourceMode: 'file', sourceCode: '', sourceFile: null })).toBe(false)
    expect(hasSubmissionDraftSource({ ...baseProgram, sourceMode: 'file', sourceCode: '', sourceFile: new File(['x'], 'main.cpp') })).toBe(true)
  })
})
