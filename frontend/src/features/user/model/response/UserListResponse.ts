import type { AuthUserListItem } from '@/features/user/model/response/AuthUserListItem'
import type { PageResponse } from '@/shared/model/PageResponse'

export type UserListResponse = PageResponse<AuthUserListItem>
