import type { UserGroupSummary } from '@/objects/usergroup/response/UserGroupSummary'
import { fromUserGroupSummaryContract } from '@/objects/usergroup/response/UserGroupSummary'
import type { PageResponse } from '@/objects/shared/PageResponse'
import { fromPageResponseContract } from '@/objects/shared/PageResponse'

export type UserGroupListResponse = PageResponse<UserGroupSummary>

export function fromUserGroupListResponseContract(
  value: unknown,
  label = 'user group list response',
): UserGroupListResponse {
  return fromPageResponseContract(value, label, fromUserGroupSummaryContract)
}
