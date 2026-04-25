import type { SubmissionSort } from '@/features/submission/model/SubmissionSort'
import type { SubmissionSortDirection } from '@/features/submission/model/SubmissionSortDirection'
import type { SubmissionVerdictFilter } from '@/features/submission/model/SubmissionVerdictFilter'

export type SubmissionListRequest = {
  userQuery: string | null
  problemQuery: string | null
  verdict: SubmissionVerdictFilter
  sort: SubmissionSort
  direction: SubmissionSortDirection
  page: number
  pageSize: number
}
