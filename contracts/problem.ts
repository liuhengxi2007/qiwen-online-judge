import type { PageResponse, ResourceStatus, ResourceVisibility } from './shared'

export type ProblemSummary = {
  id: string
  slug: string
  title: string
  visibility: ResourceVisibility
  status: ResourceStatus
  ownerUsername: string
  createdAt: string
  updatedAt: string
}

export type ProblemDetail = {
  id: string
  slug: string
  title: string
  statement: string
  visibility: ResourceVisibility
  status: ResourceStatus
  ownerUsername: string
  createdAt: string
  updatedAt: string
}

export type CreateProblemRequest = {
  slug: string
  title: string
  statement: string
  visibility: ResourceVisibility
}

export type UpdateProblemRequest = {
  title: string
  statement: string
  visibility: ResourceVisibility
}

export type ProblemListResponse = PageResponse<ProblemSummary>
