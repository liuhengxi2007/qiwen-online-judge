import { describe, expect, it } from 'vitest'

import {
  buildSubmissionPrograms,
  isTextSubmissionRole,
  isValidSubmissionRole,
  normalizeSubmitProgramDraft,
  type SubmitProgramDraft,
} from './SubmitPrograms'
import { submissionSourceCodeValue } from '@/objects/submission/SubmissionSourceCode'

const baseProgram: SubmitProgramDraft = {
  id: 'main',
  role: 'main',
  language: 'cpp17',
  sourceCode: 'int main() {}\n',
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
        sourceCode: '42\n',
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
})
