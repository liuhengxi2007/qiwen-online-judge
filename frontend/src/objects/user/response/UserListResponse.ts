import type { ManagedUserListItem } from '@/objects/user/response/ManagedUserListItem'
import type { PageResponse } from '@/objects/shared/PageResponse'

/** 用户管理分页响应；条目包含管理视图所需的账号信息。 */
export type UserListResponse = PageResponse<ManagedUserListItem>
