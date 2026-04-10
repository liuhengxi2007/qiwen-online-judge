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
import type { CreateSubmissionRequest as CreateSubmissionRequestContract } from '@contracts/submission'

export async function createSubmission(request: CreateSubmissionRequest): Promise<SubmissionDetail> {
  return postJson(
    '/api/submissions',
    fromSubmissionDetailContract,
    toCreateSubmissionRequestContract(request) satisfies CreateSubmissionRequestContract,
  )
}

export async function listSubmissions(submitterUsername?: Username | null): Promise<SubmissionListResponse> {
  const url = new URL('/api/submissions', window.location.origin)
  if (submitterUsername) {
    url.searchParams.set('username', usernameValue(submitterUsername))
  }

  return requestJson(url.pathname + url.search, fromSubmissionListResponseContract)
}

export async function getSubmission(submissionId: SubmissionId): Promise<SubmissionDetail> {
  return requestJson(`/api/submissions/${submissionIdValue(submissionId)}`, fromSubmissionDetailContract)
}
