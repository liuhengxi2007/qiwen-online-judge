import type { SubmissionProblemQuery } from '@/features/submission/model/request/SubmissionProblemQuery'
import type { SubmissionSort } from '@/features/submission/model/request/SubmissionSort'
import type { SubmissionSortDirection } from '@/features/submission/model/request/SubmissionSortDirection'
import type { SubmissionUserQuery } from '@/features/submission/model/request/SubmissionUserQuery'
import type { SubmissionVerdictFilter } from '@/features/submission/model/request/SubmissionVerdictFilter'
import type { PageRequest } from '@/shared/model/PageRequest'

export type SubmissionListRequest = {
  userQuery: SubmissionUserQuery | null
  problemQuery: SubmissionProblemQuery | null
  verdict: SubmissionVerdictFilter
  sort: SubmissionSort
  direction: SubmissionSortDirection
  pageRequest: PageRequest
}
