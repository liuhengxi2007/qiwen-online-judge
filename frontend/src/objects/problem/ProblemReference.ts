import type { ProblemId } from '@/objects/problem/ProblemId'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import type { ProblemTitle } from '@/objects/problem/ProblemTitle'

export type ProblemReference = {
  id: ProblemId
  slug: ProblemSlug
  title: ProblemTitle
}
