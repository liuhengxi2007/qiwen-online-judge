import type { CreateSubmissionRequest } from '@/objects/submission/request/CreateSubmissionRequest'
import type { SubmissionLanguage } from '@/objects/submission/SubmissionLanguage'
import { parseSubmissionSourceCode } from '@/objects/submission/SubmissionSourceCode'

export type SubmitProgramDraft = {
  id: string
  role: string
  language: SubmissionLanguage
  sourceCode: string
}

const codeRolePattern = /^[A-Za-z0-9_-]+$/
const textRolePattern = /^[A-Za-z0-9_-]+\.txt$/

export function isValidSubmissionRole(role: string): boolean {
  return codeRolePattern.test(role) || textRolePattern.test(role)
}

export function isTextSubmissionRole(role: string): boolean {
  return textRolePattern.test(role)
}

export function normalizeSubmitProgramDraft(
  program: SubmitProgramDraft,
  update: Partial<Omit<SubmitProgramDraft, 'id'>>,
): SubmitProgramDraft {
  const nextProgram = { ...program, ...update }
  const role = nextProgram.role.trim()
  if (isTextSubmissionRole(role)) {
    return { ...nextProgram, language: 'text' }
  }
  if (nextProgram.language === 'text') {
    return { ...nextProgram, language: 'cpp17' }
  }
  return nextProgram
}

export type BuildSubmissionProgramsResult =
  | { ok: true; programs: CreateSubmissionRequest['programs'] }
  | { ok: false; error: string }

export function buildSubmissionPrograms(programs: SubmitProgramDraft[]): BuildSubmissionProgramsResult {
  const payloadPrograms: CreateSubmissionRequest['programs'] = {}
  const seenRoles = new Set<string>()

  for (const program of programs) {
    const role = program.role.trim()
    if (!role || !isValidSubmissionRole(role)) {
      return {
        ok: false,
        error: 'Role must contain only ASCII letters, digits, "_" or "-", with an optional single ".txt" suffix.',
      }
    }
    if (seenRoles.has(role)) {
      return { ok: false, error: `Role is duplicated: ${role}.` }
    }
    if (isTextSubmissionRole(role) && program.language !== 'text') {
      return { ok: false, error: `Role ${role} must use Text language.` }
    }
    if (!isTextSubmissionRole(role) && program.language === 'text') {
      return { ok: false, error: 'Text submissions must use a .txt role.' }
    }
    seenRoles.add(role)

    const sourceCodeResult = parseSubmissionSourceCode(program.sourceCode)
    if (!sourceCodeResult.ok) {
      return { ok: false, error: sourceCodeResult.error }
    }
    payloadPrograms[role] = {
      language: program.language,
      sourceCode: sourceCodeResult.value,
    }
  }

  if (Object.keys(payloadPrograms).length === 0) {
    return { ok: false, error: 'At least one program is required.' }
  }

  return { ok: true, programs: payloadPrograms }
}
