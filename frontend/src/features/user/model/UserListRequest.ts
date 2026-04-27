import type { UserSearchQuery } from '@/features/user/model/UserSearchQuery'
import type { PageRequest } from '@/shared/model/Pagination'

export type UserListRequest = {
  query: UserSearchQuery | null
  pageRequest: PageRequest
}
