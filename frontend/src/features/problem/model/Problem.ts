import type { Username } from '@/features/auth/model/AuthValues'
import type { ResourceAccessPolicy } from '@/shared/access/AccessPolicy'
import type { AuditFields } from '@/shared/model/AuditFields'
import type { PageResponse } from '@/shared/model/Pagination'

export type ProblemId = string & { readonly __brand: 'ProblemId' }
export type ProblemSlug = string & { readonly __brand: 'ProblemSlug' }
export type ProblemTitle = string & { readonly __brand: 'ProblemTitle' }
export type ProblemStatementText = string & { readonly __brand: 'ProblemStatementText' }
export type ProblemDataFilename = string & { readonly __brand: 'ProblemDataFilename' }
export type ProblemTimeLimitMs = number & { readonly __brand: 'ProblemTimeLimitMs' }
export type ProblemSpaceLimitMb = number & { readonly __brand: 'ProblemSpaceLimitMb' }
export type OthersSubmissionAccess = 'none' | 'summary' | 'detail'

export type ProblemData = {
  value: ProblemDataFilename | null
}

export type ProblemSummary = AuditFields & {
  id: ProblemId
  slug: ProblemSlug
  title: ProblemTitle
  data: ProblemData
  timeLimitMs: ProblemTimeLimitMs
  spaceLimitMb: ProblemSpaceLimitMb
  accessPolicy: ResourceAccessPolicy
  othersSubmissionAccess: OthersSubmissionAccess
  creatorUsername: Username
}

export type ProblemDetail = AuditFields & {
  id: ProblemId
  slug: ProblemSlug
  title: ProblemTitle
  statement: ProblemStatementText
  data: ProblemData
  timeLimitMs: ProblemTimeLimitMs
  spaceLimitMb: ProblemSpaceLimitMb
  accessPolicy: ResourceAccessPolicy
  othersSubmissionAccess: OthersSubmissionAccess
  creatorUsername: Username
  canManage: boolean
}

export type CreateProblemRequest = {
  slug: ProblemSlug
  title: ProblemTitle
  statement: ProblemStatementText
  timeLimitMs: ProblemTimeLimitMs
  spaceLimitMb: ProblemSpaceLimitMb
  accessPolicy: ResourceAccessPolicy
  othersSubmissionAccess: OthersSubmissionAccess
}

export type UpdateProblemRequest = {
  title: ProblemTitle
  statement: ProblemStatementText
  timeLimitMs: ProblemTimeLimitMs
  spaceLimitMb: ProblemSpaceLimitMb
  accessPolicy: ResourceAccessPolicy
  othersSubmissionAccess: OthersSubmissionAccess
}

export type UpdateProblemDataRequest = {
  filename: ProblemDataFilename
  contentBase64: string
}

export type ProblemListResponse = PageResponse<ProblemSummary>
export type ProblemDataFileListResponse = {
  items: ProblemDataFilename[]
}
