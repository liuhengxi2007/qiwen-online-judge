import type { PageResponse, ResourceAccessPolicy } from './shared'

export type ProblemSetProblemSummary = {
  id: string
  slug: string
  title: string
  position: number
}

export type ProblemSetSummary = {
  id: string
  slug: string
  title: string
  description: string
  accessPolicy: ResourceAccessPolicy
  ownerUsername: string
  createdAt: string
  updatedAt: string
}

export type ProblemSetDetail = {
  id: string
  slug: string
  title: string
  description: string
  problems: ProblemSetProblemSummary[]
  accessPolicy: ResourceAccessPolicy
  ownerUsername: string
  createdAt: string
  updatedAt: string
}

export type CreateProblemSetRequest = {
  slug: string
  title: string
  description: string
  accessPolicy: ResourceAccessPolicy
}

export type UpdateProblemSetRequest = {
  title: string
  description: string
  accessPolicy: ResourceAccessPolicy
}

export type LinkProblemRequest = {
  problemSlug: string
}

export type ProblemSetListResponse = PageResponse<ProblemSetSummary>
