import type { ProblemDataUploadResult } from '@/features/problem/http/response/ProblemDataUploadResult'
import type { ProblemSlug } from '@/features/problem/model/ProblemSlug'
import { problemSlugValue } from '@/features/problem/lib/problem-parsers'
import { fromProblemDataUploadResultContract } from '@/features/problem/http/codec/ProblemHttpCodecs'
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
