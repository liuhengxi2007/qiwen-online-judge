import type { APIMessage } from '@/system/api/api-message'
import type { SubmissionLanguage } from '@/objects/submission/SubmissionLanguage'
import type { ClaimedHackAttempt } from '@/objects/hack/ClaimedHackAttempt'
import { parseHackId } from '@/objects/hack/HackId'
import { parseProblemId } from '@/objects/problem/ProblemId'
import { parseProblemSlug } from '@/objects/problem/ProblemSlug'
import { parseSubmissionId } from '@/objects/submission/SubmissionId'
import type { ClaimedSubmission } from '@/objects/submission/ClaimedSubmission'
import { isSubmissionLanguage } from '@/objects/submission/SubmissionLanguage'
import { decodeJudgeResult } from '@/objects/submission/JudgeResult'
import { parseUsername } from '@/objects/user/Username'

/** 内部领取 Hack 判题请求体；描述 worker 支持语言和开始时间。 */
type ClaimNextHackAttemptBody = {
  languages: SubmissionLanguage[]
  startedAt: string
}

/** Hack worker 领取下一个 Hack 尝试；输出任务载荷或空值。 */
export class ClaimNextHackAttempt implements APIMessage<ClaimedHackAttempt | null> {
  declare readonly responseType?: ClaimedHackAttempt | null
  readonly method = 'POST'
  readonly apiPath = 'internal/hacks/judge/claim-next'
  readonly decodeResponse = decodeClaimedHackAttemptResponse
  private readonly languages: SubmissionLanguage[]
  private readonly startedAt: string

  constructor(languages: SubmissionLanguage[], startedAt: string) {
    this.languages = languages
    this.startedAt = startedAt
  }

  body(): ClaimNextHackAttemptBody {
    return { languages: this.languages, startedAt: this.startedAt }
  }
}

function decodeClaimedHackAttemptResponse(value: unknown): ClaimedHackAttempt | null {
  if (value === null) {
    return null
  }
  if (!isRecord(value)) {
    throw new Error('Invalid claimed hack attempt payload.')
  }

  const hackId = parseNumberBrand(value.hackId, parseHackId)
  const authorUsername = parseStringBrand(value.authorUsername, parseUsername)
  const targetSubmission = decodeClaimedSubmission(value.targetSubmission)
  const oldResult = decodeJudgeResult(value.oldResult)
  const subtaskIndex = value.subtaskIndex

  if (typeof subtaskIndex !== 'number' || !Number.isSafeInteger(subtaskIndex) || subtaskIndex < 1) {
    throw new Error('Invalid claimed hack subtask index.')
  }
  if (typeof value.input !== 'string') {
    throw new Error('Invalid claimed hack input.')
  }
  if (value.strategyProviderSource !== null && typeof value.strategyProviderSource !== 'string') {
    throw new Error('Invalid claimed hack strategy provider source.')
  }

  return {
    hackId,
    targetSubmission,
    authorUsername,
    subtaskIndex,
    input: value.input,
    strategyProviderSource: value.strategyProviderSource,
    oldResult,
  }
}

function decodeClaimedSubmission(value: unknown): ClaimedSubmission {
  if (!isRecord(value)) {
    throw new Error('Invalid claimed submission payload.')
  }

  return {
    id: parseNumberBrand(value.id, parseSubmissionId),
    problemId: parseStringBrand(value.problemId, parseProblemId),
    problemSlug: parseStringBrand(value.problemSlug, parseProblemSlug),
    programManifest: decodeProgramManifest(value.programManifest),
  }
}

function decodeProgramManifest(value: unknown): ClaimedSubmission['programManifest'] {
  if (!isRecord(value) || typeof value.defaultProgramKey !== 'string' || !isRecord(value.programs)) {
    throw new Error('Invalid claimed submission program manifest.')
  }

  const programs = Object.fromEntries(
    Object.entries(value.programs).map(([key, program]) => [key, decodeProgram(program)]),
  )

  if (!(value.defaultProgramKey in programs)) {
    throw new Error('Claimed submission default program is missing.')
  }

  return {
    defaultProgramKey: value.defaultProgramKey,
    programs,
  }
}

function decodeProgram(value: unknown): ClaimedSubmission['programManifest']['programs'][string] {
  if (
    !isRecord(value) ||
    typeof value.language !== 'string' ||
    !isSubmissionLanguage(value.language) ||
    typeof value.sourceKey !== 'string' ||
    typeof value.sizeBytes !== 'number' ||
    !Number.isSafeInteger(value.sizeBytes) ||
    value.sizeBytes < 0 ||
    typeof value.sha256 !== 'string'
  ) {
    throw new Error('Invalid claimed submission program.')
  }

  return {
    language: value.language,
    sourceKey: value.sourceKey,
    sizeBytes: value.sizeBytes,
    sha256: value.sha256,
  }
}

function parseStringBrand<T>(value: unknown, parse: (raw: string) => { ok: true; value: T } | { ok: false; error: string }): T {
  if (typeof value !== 'string') {
    throw new Error('Expected string field.')
  }
  const parsed = parse(value)
  if (!parsed.ok) {
    throw new Error(parsed.error)
  }
  return parsed.value
}

function parseNumberBrand<T>(value: unknown, parse: (raw: number) => { ok: true; value: T } | { ok: false; error: string }): T {
  if (typeof value !== 'number') {
    throw new Error('Expected numeric field.')
  }
  const parsed = parse(value)
  if (!parsed.ok) {
    throw new Error(parsed.error)
  }
  return parsed.value
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null
}
