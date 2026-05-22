import type { AuthUserListItem } from '@/features/user/http/response/AuthUserListItem'
import type { PageResponse } from '@/shared/model/Pagination'

export type UserListResponse = PageResponse<AuthUserListItem>
