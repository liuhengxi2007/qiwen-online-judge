import type { SubmissionProblemQuery } from '@/features/submission/http/request/SubmissionProblemQuery'
import type { SubmissionSort } from '@/features/submission/http/request/SubmissionSort'
import type { SubmissionSortDirection } from '@/features/submission/http/request/SubmissionSortDirection'
import type { SubmissionUserQuery } from '@/features/submission/http/request/SubmissionUserQuery'
import type { SubmissionVerdictFilter } from '@/features/submission/http/request/SubmissionVerdictFilter'
import type { PageRequest } from '@/shared/model/PageRequest'

export type SubmissionListRequest = {
  userQuery: SubmissionUserQuery | null
  problemQuery: SubmissionProblemQuery | null
  verdict: SubmissionVerdictFilter
  sort: SubmissionSort
  direction: SubmissionSortDirection
  pageRequest: PageRequest
}
