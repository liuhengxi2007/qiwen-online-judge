import type {
  ProblemDataUploadResult,
  ProblemSlug,
} from '@/features/problem/domain/problem'
import {
  fromProblemDataUploadResultContract,
  problemSlugValue,
} from '@/features/problem/domain/problem'
import { postMultipart } from '@/shared/api/http-client'

export async function uploadProblemDataArchive(
  problemSlug: ProblemSlug,
  file: File,
): Promise<ProblemDataUploadResult> {
  const formData = new FormData()
  formData.set('file', file)

  return postMultipart(
    `/api/problems/${problemSlugValue(problemSlug)}/data/archive`,
    fromProblemDataUploadResultContract,
    formData,
  )
}
