import type {
  ProblemDataFilename,
  ProblemDataPath,
  ProblemDataUploadResult,
  ProblemSlug,
} from '@/features/problem/domain/problem'
import {
  fromProblemDataUploadResultContract,
  problemDataFilenameValue,
  problemDataPathValue,
  problemSlugValue,
} from '@/features/problem/domain/problem'
import { postMultipart } from '@/shared/api/http-client'

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
