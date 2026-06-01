import type { ManagedUserListItem } from '@/objects/user/response/ManagedUserListItem'
import { fromManagedUserListItemContract } from '@/objects/user/response/ManagedUserListItem'
import type { PageResponse } from '@/objects/shared/PageResponse'
import { fromPageResponseContract } from '@/objects/shared/PageResponse'

export type UserListResponse = PageResponse<ManagedUserListItem>

export function fromUserListResponseContract(value: unknown, label = 'user list response'): UserListResponse {
  return fromPageResponseContract(value, label, fromManagedUserListItemContract)
}
