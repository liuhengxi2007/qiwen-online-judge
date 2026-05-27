import type { ProblemDataUploadResult } from '@/objects/problem/response/ProblemDataUploadResult'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import { problemSlugValue } from '@/objects/problem/ProblemSlug'
import { fromProblemDataUploadResultContract } from '@/apis/problem/codecs/ProblemHttpCodecs'
import { postMultipart } from '@/system/api/http-client'

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
