import type { SubmissionProblemQuery } from '@/objects/submission/request/SubmissionProblemQuery'
import type { SubmissionSort } from '@/objects/submission/request/SubmissionSort'
import type { SubmissionSortDirection } from '@/objects/submission/request/SubmissionSortDirection'
import type { SubmissionUserQuery } from '@/objects/submission/request/SubmissionUserQuery'
import type { SubmissionVerdictFilter } from '@/objects/submission/request/SubmissionVerdictFilter'
import type { PageRequest } from '@/objects/shared/PageRequest'

export type SubmissionListRequest = {
  userQuery: SubmissionUserQuery | null
  problemQuery: SubmissionProblemQuery | null
  verdict: SubmissionVerdictFilter
  sort: SubmissionSort
  direction: SubmissionSortDirection
  pageRequest: PageRequest
}
