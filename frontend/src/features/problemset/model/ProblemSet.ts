import type { Username } from '@/features/auth/model/AuthValues'
import type { ProblemId, ProblemSlug, ProblemTitle } from '@/features/problem/model/Problem'
import type { ResourceAccessPolicy } from '@/shared/access/AccessPolicy'
import type { AuditFields } from '@/shared/model/AuditFields'
import type { PageResponse } from '@/shared/model/Pagination'

export type ProblemSetId = string & { readonly __brand: 'ProblemSetId' }
export type ProblemSetSlug = string & { readonly __brand: 'ProblemSetSlug' }
export type ProblemSetTitle = string & { readonly __brand: 'ProblemSetTitle' }
export type ProblemSetDescription = string & { readonly __brand: 'ProblemSetDescription' }

export type ProblemSetProblemSummary = {
  id: ProblemId
  slug: ProblemSlug
  title: ProblemTitle
  position: number
}

export type ProblemSetSummary = AuditFields & {
  id: ProblemSetId
  slug: ProblemSetSlug
  title: ProblemSetTitle
  description: ProblemSetDescription
  accessPolicy: ResourceAccessPolicy
  creatorUsername: Username
}

export type ProblemSet = AuditFields & {
  id: ProblemSetId
  slug: ProblemSetSlug
  title: ProblemSetTitle
  description: ProblemSetDescription
  problems: ProblemSetProblemSummary[]
  accessPolicy: ResourceAccessPolicy
  creatorUsername: Username
}

export type ProblemSetDetail = AuditFields & {
  id: ProblemSetId
  slug: ProblemSetSlug
  title: ProblemSetTitle
  description: ProblemSetDescription
  problems: ProblemSetProblemSummary[]
  accessPolicy: ResourceAccessPolicy
  creatorUsername: Username
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
