import type { ProblemData } from '@/features/problem/model/ProblemData'
import type { ProblemDataFilename } from '@/features/problem/model/ProblemDataFilename'
import type { ProblemDataPath } from '@/features/problem/model/ProblemDataPath'
import type { ProblemId } from '@/features/problem/model/ProblemId'
import type { ProblemSearchQuery } from '@/features/problem/model/ProblemSearchQuery'
import type { ProblemSlug } from '@/features/problem/model/ProblemSlug'
import type { ProblemSpaceLimitMb } from '@/features/problem/model/ProblemSpaceLimitMb'
import type { ProblemStatementText } from '@/features/problem/model/ProblemStatementText'
import type { ProblemTimeLimitMs } from '@/features/problem/model/ProblemTimeLimitMs'
import type { ProblemTitle } from '@/features/problem/model/ProblemTitle'

type ParseSuccess<T> = { ok: true; value: T }
type ParseFailure = { ok: false; error: string }
export type ParseResult<T> = ParseSuccess<T> | ParseFailure

const slugPattern = /^[a-z0-9]+(?:-[a-z0-9]+)*$/
const uuidPattern = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i

function createProblemId(value: string): ProblemId {
  return value as ProblemId
}

function createProblemSlug(value: string): ProblemSlug {
  return value as ProblemSlug
}

function createProblemSearchQuery(value: string): ProblemSearchQuery {
  return value as ProblemSearchQuery
}

function createProblemTitle(value: string): ProblemTitle {
  return value as ProblemTitle
}

function createProblemStatementText(value: string): ProblemStatementText {
  return value as ProblemStatementText
}

function createProblemDataFilename(value: string): ProblemDataFilename {
  return value as ProblemDataFilename
}

function createProblemDataPath(value: string): ProblemDataPath {
  return value as ProblemDataPath
}

function createProblemTimeLimitMs(value: number): ProblemTimeLimitMs {
  return value as ProblemTimeLimitMs
}

function createProblemSpaceLimitMb(value: number): ProblemSpaceLimitMb {
  return value as ProblemSpaceLimitMb
}

export function requireParsed<T>(result: ParseResult<T>, label: string): T {
  if (!result.ok) {
    throw new Error(`Invalid ${label} in contract payload: ${result.error}`)
  }

  return result.value
}

export function fromProblemDataContract(rawData: string | null, label: string): ProblemData {
  return {
    value: rawData === null ? null : requireParsed(parseProblemDataFilename(rawData), label),
  }
}

export function problemIdValue(problemId: ProblemId): string {
  return problemId
}

export function problemSlugValue(slug: ProblemSlug): string {
  return slug
}

export function problemSearchQueryValue(query: ProblemSearchQuery): string {
  return query
}

export function problemTitleValue(title: ProblemTitle): string {
  return title
}

export function problemStatementTextValue(statement: ProblemStatementText): string {
  return statement
}

export function problemDataFilenameValue(filename: ProblemDataFilename): string {
  return filename
}

export function problemDataPathValue(path: ProblemDataPath): string {
  return path
}

export function problemTimeLimitMsValue(timeLimitMs: ProblemTimeLimitMs): number {
  return timeLimitMs
}

export function problemSpaceLimitMbValue(spaceLimitMb: ProblemSpaceLimitMb): number {
  return spaceLimitMb
}

export function parseProblemSlug(rawSlug: string): ParseResult<ProblemSlug> {
  const normalized = rawSlug.trim()
  if (!normalized) {
    return { ok: false, error: 'Problem slug is required.' }
  }
  if (normalized.length < 3 || normalized.length > 64) {
    return { ok: false, error: 'Problem slug must be between 3 and 64 characters.' }
  }
  if (!slugPattern.test(normalized)) {
    return { ok: false, error: 'Problem slug may contain only lowercase letters, numbers, and hyphens.' }
  }
  return { ok: true, value: createProblemSlug(normalized) }
}

export function parseProblemSearchQuery(rawQuery: string): ParseResult<ProblemSearchQuery> {
  const normalized = rawQuery.trim()
  if (!normalized) {
    return { ok: false, error: 'Problem search query is required.' }
  }
  return { ok: true, value: createProblemSearchQuery(normalized) }
}

export function parseProblemId(rawId: string): ParseResult<ProblemId> {
  const normalized = rawId.trim()
  if (!normalized) {
    return { ok: false, error: 'Problem id is required.' }
  }
  if (!uuidPattern.test(normalized)) {
    return { ok: false, error: 'Problem id must be a valid UUID.' }
  }
  return { ok: true, value: createProblemId(normalized) }
}

export function parseProblemTitle(rawTitle: string): ParseResult<ProblemTitle> {
  const normalized = rawTitle.trim()
  if (!normalized) {
    return { ok: false, error: 'Problem title is required.' }
  }
  if (normalized.length > 120) {
    return { ok: false, error: 'Problem title must be at most 120 characters.' }
  }
  return { ok: true, value: createProblemTitle(normalized) }
}

export function parseProblemStatementText(rawStatement: string): ParseResult<ProblemStatementText> {
  const normalized = rawStatement.trim()
  if (!normalized) {
    return { ok: false, error: 'Problem statement is required.' }
  }
  if (normalized.length > 20000) {
    return { ok: false, error: 'Problem statement must be at most 20000 characters.' }
  }
  return { ok: true, value: createProblemStatementText(normalized) }
}

export function parseProblemDataFilename(rawFilename: string): ParseResult<ProblemDataFilename> {
  const normalized = rawFilename.trim()
  if (!normalized) {
    return { ok: false, error: 'Problem data file name is required.' }
  }
  if (normalized.length > 255) {
    return { ok: false, error: 'Problem data file name must be at most 255 characters.' }
  }
  return { ok: true, value: createProblemDataFilename(normalized) }
}

export function parseProblemDataPath(rawPath: string): ParseResult<ProblemDataPath> {
  const normalized = rawPath.trim().replaceAll('\\', '/')
  if (!normalized) {
    return { ok: false, error: 'Problem data path is required.' }
  }
  if (normalized.length > 1024) {
    return { ok: false, error: 'Problem data path must be at most 1024 characters.' }
  }
  if (normalized.startsWith('/') || normalized.endsWith('/')) {
    return { ok: false, error: "Problem data path must be relative and must not start or end with '/'." }
  }
  const segments = normalized.split('/')
  if (segments.some((segment) => !segment)) {
    return { ok: false, error: 'Problem data path must not contain empty segments.' }
  }
  if (segments.some((segment) => segment === '.' || segment === '..')) {
    return { ok: false, error: "Problem data path must not contain '.' or '..' segments." }
  }
  if (segments.some((segment) => segment.length > 255)) {
    return { ok: false, error: 'Each problem data path segment must be at most 255 characters.' }
  }
  return { ok: true, value: createProblemDataPath(normalized) }
}

export function parseProblemTimeLimitMs(rawTimeLimitMs: number): ParseResult<ProblemTimeLimitMs> {
  if (!Number.isInteger(rawTimeLimitMs)) {
    return { ok: false, error: 'Problem time limit must be an integer.' }
  }
  if (rawTimeLimitMs < 1 || rawTimeLimitMs > 600000) {
    return { ok: false, error: 'Problem time limit must be between 1 and 600000 ms.' }
  }
  return { ok: true, value: createProblemTimeLimitMs(rawTimeLimitMs) }
}

export function parseProblemSpaceLimitMb(rawSpaceLimitMb: number): ParseResult<ProblemSpaceLimitMb> {
  if (!Number.isInteger(rawSpaceLimitMb)) {
    return { ok: false, error: 'Problem space limit must be an integer.' }
  }
  if (rawSpaceLimitMb < 1 || rawSpaceLimitMb > 65536) {
    return { ok: false, error: 'Problem space limit must be between 1 and 65536 MB.' }
  }
  return { ok: true, value: createProblemSpaceLimitMb(rawSpaceLimitMb) }
}
