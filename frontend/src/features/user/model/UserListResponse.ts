import type { AuthUserListItem } from '@/features/user/model/AuthUserListItem'
import type { PageResponse } from '@/shared/model/Pagination'

export type UserListResponse = PageResponse<AuthUserListItem>
