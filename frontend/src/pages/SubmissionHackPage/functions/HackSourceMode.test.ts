import { describe, expect, it } from 'vitest'

import { canSubmitHackSources, hasHackSourceContent, usesHackMultipart, type HackSourceDraft } from './HackSourceMode'

const pasteSource: HackSourceDraft = {
  mode: 'paste',
  text: '1 2\n',
  file: null,
}

describe('hack source mode', () => {
  it('checks source content according to mode', () => {
    expect(hasHackSourceContent(pasteSource)).toBe(true)
    expect(hasHackSourceContent({ ...pasteSource, text: '' })).toBe(false)
    expect(hasHackSourceContent({ mode: 'file', text: '', file: null })).toBe(false)
    expect(hasHackSourceContent({ mode: 'file', text: '', file: new File(['1 2\n'], 'case.in') })).toBe(true)
  })

  it('requires strategy provider source only when the target needs it', () => {
    const emptyStrategy: HackSourceDraft = { mode: 'paste', text: '', file: null }

    expect(canSubmitHackSources(pasteSource, emptyStrategy, false)).toBe(true)
    expect(canSubmitHackSources(pasteSource, emptyStrategy, true)).toBe(false)
    expect(canSubmitHackSources(pasteSource, { ...emptyStrategy, text: 'int main() {}\n' }, true)).toBe(true)
  })

  it('uses multipart when either submitted source is file mode', () => {
    const fileSource: HackSourceDraft = { mode: 'file', text: '', file: new File(['1 2\n'], 'case.in') }

    expect(usesHackMultipart(pasteSource, { mode: 'paste', text: '', file: null }, false)).toBe(false)
    expect(usesHackMultipart(fileSource, { mode: 'paste', text: '', file: null }, false)).toBe(true)
    expect(usesHackMultipart(pasteSource, fileSource, true)).toBe(true)
    expect(usesHackMultipart(pasteSource, fileSource, false)).toBe(false)
  })
})
