import { describe, expect, it } from 'vitest'

import type { ManagedUserListItem } from '@/objects/user/response/ManagedUserListItem'
import { buildPermissionUpdate, displayedPermissionFlags } from './SiteManagePermissions'

describe('site-manage-permissions', () => {
  it('displays problem and contest permissions as inherited from site manager', () => {
    const user = managedUser({ siteManager: true, problemManager: false, contestManager: false })

    expect(displayedPermissionFlags(user)).toEqual({
      siteManager: true,
      problemManager: true,
      contestManager: true,
    })
  })

  it('submits all manager flags when enabling site manager', () => {
    const user = managedUser({ siteManager: false, problemManager: false, contestManager: false })

    expect(buildPermissionUpdate(user, 'siteManager', true)).toEqual({
      siteManager: true,
      problemManager: true,
      contestManager: true,
    })
  })

  it('keeps problem and contest flags independent without site manager', () => {
    const user = managedUser({ siteManager: false, problemManager: true, contestManager: false })

    expect(buildPermissionUpdate(user, 'contestManager', true)).toEqual({
      siteManager: false,
      problemManager: true,
      contestManager: true,
    })
  })
})

function managedUser(permissions: {
  siteManager: boolean
  problemManager: boolean
  contestManager: boolean
}): ManagedUserListItem {
  return {
    username: 'alice',
    displayName: 'Alice',
    email: 'alice@example.com',
    ...permissions,
  } as ManagedUserListItem
}
