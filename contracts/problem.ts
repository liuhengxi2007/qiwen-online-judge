import type { PageResponse, ResourceAccessPolicy, ResourceStatus } from './shared'

export type ProblemSummary = {
  id: string
  slug: string
  title: string
  accessPolicy: ResourceAccessPolicy
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
  accessPolicy: ResourceAccessPolicy
  status: ResourceStatus
  ownerUsername: string
  createdAt: string
  updatedAt: string
}

export type CreateProblemRequest = {
  slug: string
  title: string
  statement: string
  accessPolicy: ResourceAccessPolicy
}

export type UpdateProblemRequest = {
  title: string
  statement: string
  accessPolicy: ResourceAccessPolicy
}

export type ProblemListResponse = PageResponse<ProblemSummary>
