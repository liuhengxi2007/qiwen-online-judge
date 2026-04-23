import type { AccessSubject } from './AccessSubject'
import type { GrantRole } from './GrantRole'
import type { ResourceId } from './ResourceId'
import type { ResourceKind } from './ResourceKind'

export type ResourceAccessGrant = {
  resourceKind: ResourceKind
  resourceId: ResourceId
  grantRole: GrantRole
  subject: AccessSubject
  createdAt: string
}
