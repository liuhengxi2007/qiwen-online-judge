import type { ProblemDataFilename } from '@/objects/problem/ProblemDataFilename'
import type { ProblemDataPath } from '@/objects/problem/ProblemDataPath'
import type { ProblemDataUploadResult } from '@/objects/problem/response/ProblemDataUploadResult'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import { problemDataFilenameValue, problemDataPathValue, problemSlugValue } from '@/objects/problem/problem-parsers'
import { fromProblemDataUploadResultContract } from '@/apis/problem/codecs/ProblemHttpCodecs'
import { postMultipart } from '@/system/api/http-client'

export async function uploadProblemDataFile(
  problemSlug: ProblemSlug,
  file: File,
  filename: ProblemDataFilename,
): Promise<ProblemDataUploadResult> {
  const formData = new FormData()
  formData.set('file', file)
  formData.set('path', problemDataFilenameValue(filename))

  return postMultipart(
    `/api/problems/${problemSlugValue(problemSlug)}/data/files`,
    fromProblemDataUploadResultContract,
    formData,
  )
}

export async function saveProblemDataText(
  problemSlug: ProblemSlug,
  path: ProblemDataPath,
  content: string,
): Promise<ProblemDataUploadResult> {
  const formData = new FormData()
  formData.set('file', new File([content], path.split('/').slice(-1)[0] || 'data.txt', { type: 'text/plain' }))
  formData.set('path', problemDataPathValue(path))

  return postMultipart(
    `/api/problems/${problemSlugValue(problemSlug)}/data/files`,
    fromProblemDataUploadResultContract,
    formData,
  )
}
