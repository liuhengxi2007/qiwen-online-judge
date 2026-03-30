import type { Username } from '@/features/auth/domain/auth'
import type { ProblemId, ProblemSlug, ProblemTitle } from '@/features/problem/domain/problem'
import type { PageResponse } from '@/shared/domain/pagination'
import type { ResourceStatus, ResourceVisibility } from '@/shared/domain/resource-lifecycle'

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
  visibility: ResourceVisibility
  status: ResourceStatus
  ownerUsername: Username
  createdAt: string
  updatedAt: string
}

export type ProblemSetDetail = {
  id: ProblemSetId
  slug: ProblemSetSlug
  title: ProblemSetTitle
  description: ProblemSetDescription
  problems: ProblemSetProblemSummary[]
  visibility: ResourceVisibility
  status: ResourceStatus
  ownerUsername: Username
  createdAt: string
  updatedAt: string
}

export type CreateProblemSetRequest = {
  slug: ProblemSetSlug
  title: ProblemSetTitle
  description: ProblemSetDescription
  visibility: ResourceVisibility
}

export type UpdateProblemSetRequest = {
  title: ProblemSetTitle
  description: ProblemSetDescription
  visibility: ResourceVisibility
}

export type AddProblemToProblemSetRequest = {
  problemSlug: ProblemSlug
}

export type ProblemSetListResponse = PageResponse<ProblemSetSummary>

const slugPattern = /^[a-z0-9]+(?:-[a-z0-9]+)*$/

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
