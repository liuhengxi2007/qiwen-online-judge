import type { ProblemSearchQuery } from '@/features/problem/http/request/ProblemSearchQuery'
import type { PageRequest } from '@/shared/model/PageRequest'

export type ProblemListRequest = {
  query: ProblemSearchQuery | null
  pageRequest: PageRequest
}
