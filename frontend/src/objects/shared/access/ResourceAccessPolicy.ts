import type { AccessSubject } from './AccessSubject'
import type { BaseAccess } from './BaseAccess'

/** 资源访问策略；描述基础可见性以及用户/用户组的查看和管理授权列表。 */
export type ResourceAccessPolicy = {
  baseAccess: BaseAccess
  viewerGrants: AccessSubject[]
  managerGrants: AccessSubject[]
}

/** 创建默认受限访问策略；用于新建资源初始状态，不产生外部副作用。 */
export function createRestrictedAccessPolicy(): ResourceAccessPolicy {
  return {
    baseAccess: 'restricted',
    viewerGrants: [],
    managerGrants: [],
  }
}
