import type {
  CreateSubmissionRequest,
  SubmissionDetail,
  SubmissionId,
  SubmissionListResponse,
} from '@/features/submission/domain/submission'
import type { Username } from '@/features/auth/domain/auth'
import { usernameValue } from '@/features/auth/domain/auth'
import {
  fromSubmissionDetailContract,
  fromSubmissionListResponseContract,
  submissionIdValue,
  toCreateSubmissionRequestContract,
} from '@/features/submission/domain/submission'
import { postJson, requestJson } from '@/shared/api/http-client'
import type {
  CreateSubmissionRequest as CreateSubmissionRequestContract,
  SubmissionDetail as SubmissionDetailContract,
  SubmissionListResponse as SubmissionListResponseContract,
} from '@contracts/submission'

export async function createSubmission(request: CreateSubmissionRequest): Promise<SubmissionDetail> {
  const response = await postJson<SubmissionDetailContract>(
    '/api/submissions',
    toCreateSubmissionRequestContract(request) satisfies CreateSubmissionRequestContract,
  )

  return fromSubmissionDetailContract(response)
}

export async function listSubmissions(submitterUsername?: Username | null): Promise<SubmissionListResponse> {
  const url = new URL('/api/submissions', window.location.origin)
  if (submitterUsername) {
    url.searchParams.set('username', usernameValue(submitterUsername))
  }

  const response = await requestJson<SubmissionListResponseContract>(url.pathname + url.search)
  return fromSubmissionListResponseContract(response)
}

export async function getSubmission(submissionId: SubmissionId): Promise<SubmissionDetail> {
  const response = await requestJson<SubmissionDetailContract>(`/api/submissions/${submissionIdValue(submissionId)}`)
  return fromSubmissionDetailContract(response)
}
