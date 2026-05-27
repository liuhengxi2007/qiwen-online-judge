import type { UserSearchQuery } from '@/objects/user/request/UserSearchQuery'
import type { PageRequest } from '@/objects/shared/PageRequest'

export type UserListRequest = {
  query: UserSearchQuery | null
  pageRequest: PageRequest
}
