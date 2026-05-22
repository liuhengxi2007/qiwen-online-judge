import type { ProblemSearchQuery } from '@/features/problem/model/ProblemSearchQuery'
import type { PageRequest } from '@/shared/model/Pagination'

export type ProblemListRequest = {
  query: ProblemSearchQuery | null
  pageRequest: PageRequest
}
