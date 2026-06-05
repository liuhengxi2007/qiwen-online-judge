import { normalizeAuthPermissionFlags } from '@/objects/auth/AuthPermissionFlags'
import type { UpdateUserPermissionsRequest } from '@/objects/auth/request/UpdateUserPermissionsRequest'
import type { ManagedUserListItem } from '@/objects/user/response/ManagedUserListItem'

type PermissionKey = keyof UpdateUserPermissionsRequest
type CheckedState = boolean | 'indeterminate'

export function displayedPermissionFlags(user: ManagedUserListItem): UpdateUserPermissionsRequest {
  return normalizeAuthPermissionFlags({
    siteManager: user.siteManager,
    problemManager: user.problemManager,
    contestManager: user.contestManager,
  })
}

export function buildPermissionUpdate(
  user: ManagedUserListItem,
  permission: PermissionKey,
  checked: CheckedState,
): UpdateUserPermissionsRequest {
  const nextChecked = checked === true

  if (permission === 'siteManager') {
    if (nextChecked) {
      return {
        siteManager: true,
        problemManager: true,
        contestManager: true,
      }
    }

    return {
      siteManager: false,
      problemManager: user.problemManager,
      contestManager: user.contestManager,
    }
  }

  if (user.siteManager) {
    return {
      siteManager: true,
      problemManager: true,
      contestManager: true,
    }
  }

  return {
    siteManager: user.siteManager,
    problemManager: permission === 'problemManager' ? nextChecked : user.problemManager,
    contestManager: permission === 'contestManager' ? nextChecked : user.contestManager,
  }
}
