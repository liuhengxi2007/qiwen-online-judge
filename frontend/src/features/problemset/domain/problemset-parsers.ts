import type { ProblemSetDescription } from '@/features/problemset/model/ProblemSetDescription'
import type { ProblemSetId } from '@/features/problemset/model/ProblemSetId'
import type { ProblemSetSlug } from '@/features/problemset/model/ProblemSetSlug'
import type { ProblemSetTitle } from '@/features/problemset/model/ProblemSetTitle'

type ParseSuccess<T> = { ok: true; value: T }
type ParseFailure = { ok: false; error: string }
export type ParseResult<T> = ParseSuccess<T> | ParseFailure

const slugPattern = /^[a-z0-9]+(?:-[a-z0-9]+)*$/
const uuidPattern = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i

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

export function requireParsed<T>(result: ParseResult<T>, label: string): T {
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

export function parseProblemSetId(rawId: string): ParseResult<ProblemSetId> {
  const normalized = rawId.trim()
  if (!normalized) {
    return { ok: false, error: 'Problem set id is required.' }
  }
  if (!uuidPattern.test(normalized)) {
    return { ok: false, error: 'Problem set id must be a valid UUID.' }
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

export function parseProblemSetProblemPosition(rawPosition: number): ParseResult<number> {
  if (!Number.isInteger(rawPosition) || rawPosition <= 0) {
    return { ok: false, error: 'Problem set problem position must be a positive integer.' }
  }
  return { ok: true, value: rawPosition }
}
