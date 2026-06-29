import type { AccessSubject } from './AccessSubject'
import type { BaseAccess } from './BaseAccess'

/** 资源可见性策略；只描述基础可见性以及用户/用户组的查看授权列表。 */
export type ResourceVisibilityPolicy = {
  baseAccess: BaseAccess
  viewerGrants: AccessSubject[]
}

/** 创建默认受限可见性策略；用于不支持资源级管理授权的资源。 */
export function createRestrictedVisibilityPolicy(): ResourceVisibilityPolicy {
  return {
    baseAccess: 'restricted',
    viewerGrants: [],
  }
}
