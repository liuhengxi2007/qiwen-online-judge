import type { UserIdentity } from './auth'
import type { PageResponse, ResourceAccessPolicy } from './shared'

export type OthersSubmissionAccess = 'none' | 'summary' | 'detail'

export type ProblemSummary = {
  id: string
  slug: string
  title: string
  data: string | null
  timeLimitMs: number
  spaceLimitMb: number
  accessPolicy: ResourceAccessPolicy
  othersSubmissionAccess: OthersSubmissionAccess
  creator: UserIdentity
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
  othersSubmissionAccess: OthersSubmissionAccess
  creator: UserIdentity
  canManage: boolean
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
  othersSubmissionAccess: OthersSubmissionAccess
}

export type UpdateProblemRequest = {
  title: string
  statement: string
  timeLimitMs: number
  spaceLimitMb: number
  accessPolicy: ResourceAccessPolicy
  othersSubmissionAccess: OthersSubmissionAccess
}

export type UpdateProblemDataRequest = {
  filename: string
  contentBase64: string
}

export type ProblemDataFileListResponse = {
  items: string[]
}

export type ProblemListResponse = PageResponse<ProblemSummary>
