import type { AccessSubject } from './AccessSubject'
import type { BaseAccess } from './BaseAccess'

export type ResourceAccessPolicy = {
  baseAccess: BaseAccess
  viewerGrants: AccessSubject[]
  managerGrants: AccessSubject[]
}
