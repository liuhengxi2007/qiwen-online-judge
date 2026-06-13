import type { SubmissionProblemQuery } from '@/objects/submission/request/SubmissionProblemQuery'
import type { SubmissionSort } from '@/objects/submission/request/SubmissionSort'
import type { SubmissionSortDirection } from '@/objects/submission/request/SubmissionSortDirection'
import type { SubmissionUserQuery } from '@/objects/submission/request/SubmissionUserQuery'
import type { SubmissionVerdictFilter } from '@/objects/submission/request/SubmissionVerdictFilter'
import type { PageRequest } from '@/objects/shared/PageRequest'

/** 提交列表请求；组合用户/题目过滤、verdict 过滤、排序和分页条件。 */
export type SubmissionListRequest = {
  userQuery: SubmissionUserQuery | null
  problemQuery: SubmissionProblemQuery | null
  verdict: SubmissionVerdictFilter
  sort: SubmissionSort
  direction: SubmissionSortDirection
  pageRequest: PageRequest
}
