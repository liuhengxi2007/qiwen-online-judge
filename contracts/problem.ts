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

export type ProblemSuggestion = {
  slug: string
  title: string
}

export type ProblemListRequest = {
  query: string | null
  page: number
  pageSize: number
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

export type ProblemDataFileListResponse = {
  items: string[]
}

export type ProblemDataTreeNodeKind = 'file' | 'directory'

export type ProblemDataTreeNode = {
  path: string
  kind: ProblemDataTreeNodeKind
  sizeBytes: number | null
}

export type ProblemDataTreeResponse = {
  items: ProblemDataTreeNode[]
}

export type ProblemListResponse = PageResponse<ProblemSummary>
