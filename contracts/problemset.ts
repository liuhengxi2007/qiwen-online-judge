import type { PageResponse, ResourceStatus, ResourceVisibility } from './shared'

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
  visibility: ResourceVisibility
  status: ResourceStatus
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
  visibility: ResourceVisibility
  status: ResourceStatus
  ownerUsername: string
  createdAt: string
  updatedAt: string
}

export type CreateProblemSetRequest = {
  slug: string
  title: string
  description: string
  visibility: ResourceVisibility
}

export type UpdateProblemSetRequest = {
  title: string
  description: string
  visibility: ResourceVisibility
}

export type LinkProblemRequest = {
  problemSlug: string
}

export type ProblemSetListResponse = PageResponse<ProblemSetSummary>
