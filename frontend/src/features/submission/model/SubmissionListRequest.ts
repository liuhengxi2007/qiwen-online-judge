import type { SubmissionProblemQuery } from '@/features/submission/model/SubmissionProblemQuery'
import type { SubmissionSort } from '@/features/submission/model/SubmissionSort'
import type { SubmissionSortDirection } from '@/features/submission/model/SubmissionSortDirection'
import type { SubmissionUserQuery } from '@/features/submission/model/SubmissionUserQuery'
import type { SubmissionVerdictFilter } from '@/features/submission/model/SubmissionVerdictFilter'
import type { PageRequest } from '@/shared/model/Pagination'

export type SubmissionListRequest = {
  userQuery: SubmissionUserQuery | null
  problemQuery: SubmissionProblemQuery | null
  verdict: SubmissionVerdictFilter
  sort: SubmissionSort
  direction: SubmissionSortDirection
  pageRequest: PageRequest
}
