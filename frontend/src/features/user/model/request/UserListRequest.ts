import type { UserSearchQuery } from '@/features/user/model/request/UserSearchQuery'
import type { PageRequest } from '@/shared/model/PageRequest'

export type UserListRequest = {
  query: UserSearchQuery | null
  pageRequest: PageRequest
}
