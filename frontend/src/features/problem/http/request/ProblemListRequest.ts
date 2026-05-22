import type { ProblemSearchQuery } from '@/features/problem/http/request/ProblemSearchQuery'
import type { PageRequest } from '@/shared/model/Pagination'

export type ProblemListRequest = {
  query: ProblemSearchQuery | null
  pageRequest: PageRequest
}
