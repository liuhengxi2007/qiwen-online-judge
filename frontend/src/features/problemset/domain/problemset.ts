import type {
  CreateProblemSetRequest as CreateProblemSetRequestContract,
  LinkProblemRequest as LinkProblemRequestContract,
  ProblemSetDetail as ProblemSetDetailContract,
  ProblemSetListResponse as ProblemSetListResponseContract,
  ProblemSetProblemSummary as ProblemSetProblemSummaryContract,
  ProblemSetSummary as ProblemSetSummaryContract,
  UpdateProblemSetRequest as UpdateProblemSetRequestContract,
} from '@contracts/problemset'
import type { Username } from '@/features/auth/domain/auth'
import { parseUsername } from '@/features/auth/domain/auth'
import {
  parseProblemId,
  parseProblemSlug,
  parseProblemTitle,
  problemSlugValue,
  type ProblemId,
  type ProblemSlug,
  type ProblemTitle,
} from '@/features/problem/domain/problem'
import type { PageResponse } from '@/shared/domain/pagination'
import type { ResourceAccessPolicy } from '@/shared/domain/resource-lifecycle'

type Brand<T, Name extends string> = T & { readonly __brand: Name }
type ParseSuccess<T> = { ok: true; value: T }
type ParseFailure = { ok: false; error: string }
type ParseResult<T> = ParseSuccess<T> | ParseFailure

export type ProblemSetId = Brand<string, 'ProblemSetId'>
export type ProblemSetSlug = Brand<string, 'ProblemSetSlug'>
export type ProblemSetTitle = Brand<string, 'ProblemSetTitle'>
export type ProblemSetDescription = Brand<string, 'ProblemSetDescription'>
export type ProblemSetProblemPosition = Brand<number, 'ProblemSetProblemPosition'>

export type ProblemSetProblemSummary = {
  id: ProblemId
  slug: ProblemSlug
  title: ProblemTitle
  position: ProblemSetProblemPosition
}

export type ProblemSetSummary = {
  id: ProblemSetId
  slug: ProblemSetSlug
  title: ProblemSetTitle
  description: ProblemSetDescription
  accessPolicy: ResourceAccessPolicy
  creatorUsername: Username
  createdAt: string
  updatedAt: string
}

export type ProblemSetDetail = {
  id: ProblemSetId
  slug: ProblemSetSlug
  title: ProblemSetTitle
  description: ProblemSetDescription
  problems: ProblemSetProblemSummary[]
  accessPolicy: ResourceAccessPolicy
  creatorUsername: Username
  createdAt: string
  updatedAt: string
}

export type CreateProblemSetRequest = {
  slug: ProblemSetSlug
  title: ProblemSetTitle
  description: ProblemSetDescription
  accessPolicy: ResourceAccessPolicy
}

export type UpdateProblemSetRequest = {
  title: ProblemSetTitle
  description: ProblemSetDescription
  accessPolicy: ResourceAccessPolicy
}

export type AddProblemToProblemSetRequest = {
  problemSlug: ProblemSlug
}

export type ProblemSetListResponse = PageResponse<ProblemSetSummary>

const slugPattern = /^[a-z0-9]+(?:-[a-z0-9]+)*$/

function createProblemSetId(value: string): ProblemSetId {
  return value as ProblemSetId
}

function createProblemSetSlug(value: string): ProblemSetSlug {
  return value as ProblemSetSlug
}

function createProblemSetTitle(value: string): ProblemSetTitle {
  return value as ProblemSetTitle
}

function createProblemSetDescription(value: string): ProblemSetDescription {
  return value as ProblemSetDescription
}

function createProblemSetProblemPosition(value: number): ProblemSetProblemPosition {
  return value as ProblemSetProblemPosition
}

function requireParsed<T>(result: ParseResult<T>, label: string): T {
  if (!result.ok) {
    throw new Error(`Invalid ${label} in contract payload: ${result.error}`)
  }

  return result.value
}

export function problemSetSlugValue(slug: ProblemSetSlug): string {
  return slug
}

export function problemSetTitleValue(title: ProblemSetTitle): string {
  return title
}

export function problemSetDescriptionValue(description: ProblemSetDescription): string {
  return description
}

export function problemSetProblemPositionValue(position: ProblemSetProblemPosition): number {
  return position
}

export function parseProblemSetId(rawId: string): ParseResult<ProblemSetId> {
  const normalized = rawId.trim()
  if (!normalized) {
    return { ok: false, error: 'Problem set id is required.' }
  }
  return { ok: true, value: createProblemSetId(normalized) }
}

export function parseProblemSetSlug(rawSlug: string): ParseResult<ProblemSetSlug> {
  const normalized = rawSlug.trim()
  if (!normalized) {
    return { ok: false, error: 'Problem set slug is required.' }
  }
  if (normalized.length < 3 || normalized.length > 64) {
    return { ok: false, error: 'Problem set slug must be between 3 and 64 characters.' }
  }
  if (!slugPattern.test(normalized)) {
    return { ok: false, error: 'Problem set slug may contain only lowercase letters, numbers, and hyphens.' }
  }
  return { ok: true, value: createProblemSetSlug(normalized) }
}

export function parseProblemSetTitle(rawTitle: string): ParseResult<ProblemSetTitle> {
  const normalized = rawTitle.trim()
  if (!normalized) {
    return { ok: false, error: 'Problem set title is required.' }
  }
  if (normalized.length > 120) {
    return { ok: false, error: 'Problem set title must be at most 120 characters.' }
  }
  return { ok: true, value: createProblemSetTitle(normalized) }
}

export function parseProblemSetDescription(rawDescription: string): ParseResult<ProblemSetDescription> {
  const normalized = rawDescription.trim()
  if (normalized.length > 2000) {
    return { ok: false, error: 'Problem set description must be at most 2000 characters.' }
  }
  return { ok: true, value: createProblemSetDescription(normalized) }
}

export function parseProblemSetProblemPosition(rawPosition: number): ParseResult<ProblemSetProblemPosition> {
  if (!Number.isInteger(rawPosition) || rawPosition <= 0) {
    return { ok: false, error: 'Problem set problem position must be a positive integer.' }
  }
  return { ok: true, value: createProblemSetProblemPosition(rawPosition) }
}

export function fromProblemSetProblemSummaryContract(
  problem: ProblemSetProblemSummaryContract,
): ProblemSetProblemSummary {
  return {
    id: requireParsed(parseProblemId(problem.id), 'problem set problem id'),
    slug: requireParsed(parseProblemSlug(problem.slug), 'problem set problem slug'),
    title: requireParsed(parseProblemTitle(problem.title), 'problem set problem title'),
    position: requireParsed(parseProblemSetProblemPosition(problem.position), 'problem set problem position'),
  }
}

export function fromProblemSetSummaryContract(problemSet: ProblemSetSummaryContract): ProblemSetSummary {
  return {
    id: requireParsed(parseProblemSetId(problemSet.id), 'problem set summary id'),
    slug: requireParsed(parseProblemSetSlug(problemSet.slug), 'problem set summary slug'),
    title: requireParsed(parseProblemSetTitle(problemSet.title), 'problem set summary title'),
    description: requireParsed(
      parseProblemSetDescription(problemSet.description),
      'problem set summary description',
    ),
    accessPolicy: problemSet.accessPolicy,
    creatorUsername: requireParsed(parseUsername(problemSet.creatorUsername), 'problem set summary creator username'),
    createdAt: problemSet.createdAt,
    updatedAt: problemSet.updatedAt,
  }
}

export function fromProblemSetDetailContract(problemSet: ProblemSetDetailContract): ProblemSetDetail {
  return {
    id: requireParsed(parseProblemSetId(problemSet.id), 'problem set detail id'),
    slug: requireParsed(parseProblemSetSlug(problemSet.slug), 'problem set detail slug'),
    title: requireParsed(parseProblemSetTitle(problemSet.title), 'problem set detail title'),
    description: requireParsed(
      parseProblemSetDescription(problemSet.description),
      'problem set detail description',
    ),
    problems: problemSet.problems.map(fromProblemSetProblemSummaryContract),
    accessPolicy: problemSet.accessPolicy,
    creatorUsername: requireParsed(parseUsername(problemSet.creatorUsername), 'problem set detail creator username'),
    createdAt: problemSet.createdAt,
    updatedAt: problemSet.updatedAt,
  }
}

export function fromProblemSetListResponseContract(
  response: ProblemSetListResponseContract,
): ProblemSetListResponse {
  return {
    items: response.items.map(fromProblemSetSummaryContract),
    page: response.page,
    pageSize: response.pageSize,
    totalItems: response.totalItems,
  }
}

export function toCreateProblemSetRequestContract(
  request: CreateProblemSetRequest,
): CreateProblemSetRequestContract {
  return {
    slug: problemSetSlugValue(request.slug),
    title: problemSetTitleValue(request.title),
    description: problemSetDescriptionValue(request.description),
    accessPolicy: request.accessPolicy,
  }
}

export function toUpdateProblemSetRequestContract(
  request: UpdateProblemSetRequest,
): UpdateProblemSetRequestContract {
  return {
    title: problemSetTitleValue(request.title),
    description: problemSetDescriptionValue(request.description),
    accessPolicy: request.accessPolicy,
  }
}

export function toLinkProblemRequestContract(
  request: AddProblemToProblemSetRequest,
): LinkProblemRequestContract {
  return {
    problemSlug: problemSlugValue(request.problemSlug),
  }
}
