import type {
  CreateSubmissionRequest,
  SubmissionDetail,
  SubmissionId,
  SubmissionListRequest,
  SubmissionListResponse,
} from '@/features/submission/domain/submission'
import {
  fromSubmissionDetailContract,
  fromSubmissionListResponseContract,
  submissionIdValue,
  toCreateSubmissionRequestContract,
  toSubmissionListRequestContract,
} from '@/features/submission/domain/submission'
import { decodeSuccessResponse, postJson, requestJson } from '@/shared/api/http-client'
import type { SuccessResponse } from '@contracts/shared'
import type { CreateSubmissionRequest as CreateSubmissionRequestContract } from '@contracts/submission'

export async function createSubmission(request: CreateSubmissionRequest): Promise<SubmissionDetail> {
  return postJson(
    '/api/submissions',
    fromSubmissionDetailContract,
    toCreateSubmissionRequestContract(request) satisfies CreateSubmissionRequestContract,
  )
}

export async function listSubmissions(request: SubmissionListRequest): Promise<SubmissionListResponse> {
  const url = new URL('/api/submissions', window.location.origin)
  const contractRequest = toSubmissionListRequestContract(request)

  if (contractRequest.userQuery !== null && contractRequest.userQuery.trim()) {
    url.searchParams.set('username', contractRequest.userQuery)
  }
  if (contractRequest.problemQuery !== null && contractRequest.problemQuery.trim()) {
    url.searchParams.set('problem', contractRequest.problemQuery)
  }
  url.searchParams.set('verdict', contractRequest.verdict)
  url.searchParams.set('sort', contractRequest.sort)
  url.searchParams.set('direction', contractRequest.direction)
  url.searchParams.set('page', String(contractRequest.page))
  url.searchParams.set('pageSize', String(contractRequest.pageSize))

  return requestJson(url.pathname + url.search, fromSubmissionListResponseContract)
}

export async function getSubmission(submissionId: SubmissionId): Promise<SubmissionDetail> {
  return requestJson(`/api/submissions/${submissionIdValue(submissionId)}`, fromSubmissionDetailContract)
}

export async function rejudgeSubmission(submissionId: SubmissionId): Promise<SubmissionDetail> {
  return postJson(`/api/submissions/${submissionIdValue(submissionId)}/rejudge`, fromSubmissionDetailContract, {})
}

export async function deleteSubmission(submissionId: SubmissionId): Promise<SuccessResponse> {
  return postJson(`/api/submissions/${submissionIdValue(submissionId)}/delete`, decodeSuccessResponse, {})
}
