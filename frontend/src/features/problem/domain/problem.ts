import type {
  CreateProblemRequest as CreateProblemRequestContract,
  ProblemDetail as ProblemDetailContract,
  ProblemListResponse as ProblemListResponseContract,
  ProblemSummary as ProblemSummaryContract,
  UpdateProblemRequest as UpdateProblemRequestContract,
} from '@contracts/problem'
import type { Username } from '@/features/auth/domain/auth'
import { parseUsername } from '@/features/auth/domain/auth'
import type { PageResponse } from '@/shared/domain/pagination'
import type { ResourceAccessPolicy, ResourceStatus } from '@/shared/domain/resource-lifecycle'

type Brand<T, Name extends string> = T & { readonly __brand: Name }
type ParseSuccess<T> = { ok: true; value: T }
type ParseFailure = { ok: false; error: string }
type ParseResult<T> = ParseSuccess<T> | ParseFailure

export type ProblemId = Brand<string, 'ProblemId'>
export type ProblemSlug = Brand<string, 'ProblemSlug'>
export type ProblemTitle = Brand<string, 'ProblemTitle'>
export type ProblemStatementText = Brand<string, 'ProblemStatementText'>

export type ProblemSummary = {
  id: ProblemId
  slug: ProblemSlug
  title: ProblemTitle
  accessPolicy: ResourceAccessPolicy
  status: ResourceStatus
  ownerUsername: Username
  createdAt: string
  updatedAt: string
}

export type ProblemDetail = {
  id: ProblemId
  slug: ProblemSlug
  title: ProblemTitle
  statement: ProblemStatementText
  accessPolicy: ResourceAccessPolicy
  status: ResourceStatus
  ownerUsername: Username
  createdAt: string
  updatedAt: string
}

export type CreateProblemRequest = {
  slug: ProblemSlug
  title: ProblemTitle
  statement: ProblemStatementText
  accessPolicy: ResourceAccessPolicy
}

export type UpdateProblemRequest = {
  title: ProblemTitle
  statement: ProblemStatementText
  accessPolicy: ResourceAccessPolicy
}

export type ProblemListResponse = PageResponse<ProblemSummary>

const slugPattern = /^[a-z0-9]+(?:-[a-z0-9]+)*$/

function createProblemId(value: string): ProblemId {
  return value as ProblemId
}

function createProblemSlug(value: string): ProblemSlug {
  return value as ProblemSlug
}

function createProblemTitle(value: string): ProblemTitle {
  return value as ProblemTitle
}

function createProblemStatementText(value: string): ProblemStatementText {
  return value as ProblemStatementText
}

function requireParsed<T>(result: ParseResult<T>, label: string): T {
  if (!result.ok) {
    throw new Error(`Invalid ${label} in contract payload: ${result.error}`)
  }

  return result.value
}

export function problemIdValue(problemId: ProblemId): string {
  return problemId
}

export function problemSlugValue(slug: ProblemSlug): string {
  return slug
}

export function problemTitleValue(title: ProblemTitle): string {
  return title
}

export function problemStatementTextValue(statement: ProblemStatementText): string {
  return statement
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

export function parseProblemId(rawId: string): ParseResult<ProblemId> {
  const normalized = rawId.trim()
  if (!normalized) {
    return { ok: false, error: 'Problem id is required.' }
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

export function fromProblemSummaryContract(problem: ProblemSummaryContract): ProblemSummary {
  return {
    id: requireParsed(parseProblemId(problem.id), 'problem summary id'),
    slug: requireParsed(parseProblemSlug(problem.slug), 'problem summary slug'),
    title: requireParsed(parseProblemTitle(problem.title), 'problem summary title'),
    accessPolicy: problem.accessPolicy,
    status: problem.status as ResourceStatus,
    ownerUsername: requireParsed(parseUsername(problem.ownerUsername), 'problem summary owner username'),
    createdAt: problem.createdAt,
    updatedAt: problem.updatedAt,
  }
}

export function fromProblemDetailContract(problem: ProblemDetailContract): ProblemDetail {
  return {
    id: requireParsed(parseProblemId(problem.id), 'problem detail id'),
    slug: requireParsed(parseProblemSlug(problem.slug), 'problem detail slug'),
    title: requireParsed(parseProblemTitle(problem.title), 'problem detail title'),
    statement: requireParsed(parseProblemStatementText(problem.statement), 'problem detail statement'),
    accessPolicy: problem.accessPolicy,
    status: problem.status as ResourceStatus,
    ownerUsername: requireParsed(parseUsername(problem.ownerUsername), 'problem detail owner username'),
    createdAt: problem.createdAt,
    updatedAt: problem.updatedAt,
  }
}

export function fromProblemListResponseContract(response: ProblemListResponseContract): ProblemListResponse {
  return {
    items: response.items.map(fromProblemSummaryContract),
    page: response.page,
    pageSize: response.pageSize,
    totalItems: response.totalItems,
  }
}

export function toCreateProblemRequestContract(request: CreateProblemRequest): CreateProblemRequestContract {
  return {
    slug: problemSlugValue(request.slug),
    title: problemTitleValue(request.title),
    statement: problemStatementTextValue(request.statement),
    accessPolicy: request.accessPolicy,
  }
}

export function toUpdateProblemRequestContract(request: UpdateProblemRequest): UpdateProblemRequestContract {
  return {
    title: problemTitleValue(request.title),
    statement: problemStatementTextValue(request.statement),
    accessPolicy: request.accessPolicy,
  }
}
