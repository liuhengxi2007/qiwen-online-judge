import type { PageResponse, ResourceAccessPolicy, ResourceStatus } from './shared'

export type ProblemSummary = {
  id: string
  slug: string
  title: string
  data: string | null
  timeLimitMs: number
  spaceLimitMb: number
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
  data: string | null
  timeLimitMs: number
  spaceLimitMb: number
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
  timeLimitMs: number
  spaceLimitMb: number
  accessPolicy: ResourceAccessPolicy
}

export type UpdateProblemRequest = {
  title: string
  statement: string
  timeLimitMs: number
  spaceLimitMb: number
  accessPolicy: ResourceAccessPolicy
}

export type UpdateProblemDataRequest = {
  filename: string
  contentBase64: string
}

export type ProblemDataFileListResponse = {
  items: string[]
}

export type ProblemListResponse = PageResponse<ProblemSummary>
