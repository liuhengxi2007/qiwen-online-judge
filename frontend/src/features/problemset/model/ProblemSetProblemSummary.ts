import type { ProblemId } from '@/features/problem/model/ProblemId'
import type { ProblemSlug } from '@/features/problem/model/ProblemSlug'
import type { ProblemTitle } from '@/features/problem/model/ProblemTitle'

export type ProblemSetProblemSummary = {
  id: ProblemId
  slug: ProblemSlug
  title: ProblemTitle
  position: number
}
