import type { UserGroupSlug } from '@/objects/usergroup/UserGroupSlug'

/** 用户所属用户组 slug 列表响应；用于授权策略选择器等内部查询。 */
export type UserGroupSlugListResponse = {
  slugs: UserGroupSlug[]
}
