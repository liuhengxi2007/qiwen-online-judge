import type { AuthUserListItem } from '@/objects/user/response/AuthUserListItem'
import type { PageResponse } from '@/objects/shared/PageResponse'

export type UserListResponse = PageResponse<AuthUserListItem>
