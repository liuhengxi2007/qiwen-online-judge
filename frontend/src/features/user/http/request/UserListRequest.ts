import type { UserSearchQuery } from '@/features/user/http/request/UserSearchQuery'
import type { PageRequest } from '@/shared/model/Pagination'

export type UserListRequest = {
  query: UserSearchQuery | null
  pageRequest: PageRequest
}
