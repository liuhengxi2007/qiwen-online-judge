import type { ProblemSearchQuery } from '@/objects/problem/request/ProblemSearchQuery'
import type { PageRequest } from '@/objects/shared/PageRequest'

export type ProblemListRequest = {
  query: ProblemSearchQuery | null
  pageRequest: PageRequest
}
