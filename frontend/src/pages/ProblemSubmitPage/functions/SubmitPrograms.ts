import type { CreateSubmissionMultipartRequest } from '@/apis/submission/CreateSubmission'
import type { CreateSubmissionRequest } from '@/objects/submission/request/CreateSubmissionRequest'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import type { SubmissionLanguage } from '@/objects/submission/SubmissionLanguage'
import { parseSubmissionSourceCode } from '@/objects/submission/SubmissionSourceCode'

/**
 * 提交源码输入模式，支持粘贴文本或选择文件。
 */
export type SubmitSourceMode = 'paste' | 'file'

/**
 * 单个提交程序草稿，保存角色、语言、源码文本、文件和输入模式。
 */
export type SubmitProgramDraft = {
  id: string
  role: string
  language: SubmissionLanguage
  sourceMode: SubmitSourceMode
  sourceCode: string
  sourceFile: File | null
}

const codeRolePattern = /^[A-Za-z0-9_-]+$/
const textRolePattern = /^[A-Za-z0-9_-]+\.txt$/

/**
 * 校验提交角色名是否符合 judge.yaml 支持的 ASCII 标识格式。
 */
export function isValidSubmissionRole(role: string): boolean {
  return codeRolePattern.test(role) || textRolePattern.test(role)
}

/**
 * 判断角色是否为文本角色，文本角色以 .txt 后缀表示。
 */
export function isTextSubmissionRole(role: string): boolean {
  return textRolePattern.test(role)
}

/**
 * 规范化提交程序草稿；文本角色强制使用 text 语言，其余角色默认 cpp17。
 */
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

/**
 * 构造提交程序集合结果，成功时携带 JSON programs，失败时携带错误消息。
 */
export type BuildSubmissionProgramsResult =
  | { ok: true; programs: CreateSubmissionRequest['programs'] }
  | { ok: false; error: string }

/**
 * 从草稿构造 JSON 提交程序集合；要求角色合法、源码完整且不含文件模式。
 */
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

    if (program.sourceMode === 'file') {
      return { ok: false, error: 'File submissions require multipart payloads.' }
    }

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

/**
 * 提交载荷构造结果，成功时携带 JSON 或 multipart 提交数据。
 */
export type BuildSubmissionPayloadResult =
  | { ok: true; payload: SubmissionCreatePayload }
  | { ok: false; error: string }

/**
 * 提交创建载荷，区分普通 JSON 请求体和 FormData multipart 请求体。
 */
export type SubmissionCreatePayload =
  | { kind: 'json'; request: CreateSubmissionRequest }
  | { kind: 'multipart'; request: CreateSubmissionMultipartRequest }

/**
 * 判断提交程序草稿是否已有源码内容；文件模式要求选择文件。
 */
export function hasSubmissionDraftSource(program: SubmitProgramDraft): boolean {
  return program.sourceMode === 'file'
    ? program.sourceFile !== null
    : program.sourceCode.trim().length > 0
}

/**
 * 根据提交草稿构造后端创建提交载荷；存在文件时自动切换 multipart。
 */
export function buildSubmissionPayload(problemSlug: ProblemSlug, programs: SubmitProgramDraft[]): BuildSubmissionPayloadResult {
  const roleResult = validateProgramRoles(programs)
  if (!roleResult.ok) {
    return roleResult
  }

  const hasFileSource = programs.some((program) => program.sourceMode === 'file')
  if (!hasFileSource) {
    const programsResult = buildSubmissionPrograms(programs)
    if (!programsResult.ok) {
      return programsResult
    }
    return { ok: true, payload: { kind: 'json', request: { problemSlug, programs: programsResult.programs } } }
  }

  const multipartPrograms: CreateSubmissionMultipartRequest['programs'] = []
  const sources: CreateSubmissionMultipartRequest['sources'] = []

  for (const [index, program] of programs.entries()) {
    const role = program.role.trim()
    const sourcePart = `source-${index + 1}`
    multipartPrograms.push({
      role,
      language: program.language,
      sourcePart,
    })

    if (program.sourceMode === 'file') {
      if (!program.sourceFile) {
        return { ok: false, error: `Source file is required for role ${role}.` }
      }
      sources.push({ sourcePart, source: program.sourceFile })
    } else {
      if (!program.sourceCode.trim()) {
        return { ok: false, error: 'Source code is required.' }
      }
      sources.push({ sourcePart, source: program.sourceCode })
    }
  }

  return {
    ok: true,
    payload: {
      kind: 'multipart',
      request: {
        problemSlug,
        programs: multipartPrograms,
        sources,
      },
    },
  }
}

/**
 * 提交程序角色校验结果，失败时返回角色相关错误。
 */
type ValidateProgramRolesResult = { ok: true } | { ok: false; error: string }

/**
 * 校验提交程序角色的合法性和唯一性。
 */
function validateProgramRoles(programs: SubmitProgramDraft[]): ValidateProgramRolesResult {
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
  }

  if (programs.length === 0) {
    return { ok: false, error: 'At least one program is required.' }
  }

  return { ok: true }
}
