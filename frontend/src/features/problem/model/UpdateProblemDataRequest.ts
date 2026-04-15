import type { ProblemDataFilename } from '@/features/problem/model/ProblemDataFilename'

export type UpdateProblemDataRequest = {
  filename: ProblemDataFilename
  contentBase64: string
}
