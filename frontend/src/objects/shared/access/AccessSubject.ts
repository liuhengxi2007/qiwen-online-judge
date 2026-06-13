import type { AccessUserGroupSlug } from '@/objects/shared/access/AccessUserGroupSlug'
import type { AccessUsername } from '@/objects/shared/access/AccessUsername'

/** 单个用户访问主体；用于资源访问策略中的显式授权对象。 */
export type UserAccessSubject = {
  kind: 'user'
  username: AccessUsername
}

/** 用户组访问主体；用于把资源权限授予某个用户组。 */
export type UserGroupAccessSubject = {
  kind: 'user_group'
  slug: AccessUserGroupSlug
}

/** 资源访问主体联合类型；调用方通过 kind 区分用户和用户组授权。 */
export type AccessSubject = UserAccessSubject | UserGroupAccessSubject
