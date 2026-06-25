import { Link } from 'react-router-dom'
import { Trash2 } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { Checkbox } from '@/components/ui/checkbox'
import { TableCell, TableRow } from '@/components/ui/table'
import { emailAddressValue } from '@/objects/auth/EmailAddress'
import { displayNameValue } from '@/objects/user/DisplayName'
import { usernameValue } from '@/objects/user/Username'
import type { ManagedUserListItem } from '@/objects/user/response/ManagedUserListItem'
import { ConfirmActionDialog } from '@/pages/components/ConfirmActionDialog'
import { useI18n } from '@/system/i18n/use-i18n'

import { buildPermissionUpdate, displayedPermissionFlags } from '../../functions/SiteManagePermissions'
import type { SiteManageModel } from './objects/SiteManageModel'

type SiteManageUserRowProps = {
  model: SiteManageModel
  siteManagerSession: boolean
  listedUser: ManagedUserListItem
}

/**
 * 站点用户表格行，集中处理权限切换、设置入口和删除确认。
 */
export function SiteManageUserRow({ model, siteManagerSession, listedUser }: SiteManageUserRowProps) {
  const { t } = useI18n()
  const permissions = displayedPermissionFlags(listedUser)
  const permissionsDisabled =
    model.updatingUsername !== null || model.deletingUsername !== null || usernameValue(listedUser.username) === 'admin'
  const inheritedPermissionsDisabled = permissionsDisabled || permissions.siteManager

  return (
    <TableRow>
      <TableCell className="text-stone-700">{usernameValue(listedUser.username)}</TableCell>
      <TableCell className="font-medium text-stone-900">{displayNameValue(listedUser.displayName)}</TableCell>
      <TableCell>{emailAddressValue(listedUser.email)}</TableCell>
      <TableCell>
        <Button asChild variant="outline" size="sm" className="rounded-full border-stone-300 bg-white">
          <Link to={`/user/${usernameValue(listedUser.username)}/settings`}>{t('siteManage.openSettings')}</Link>
        </Button>
      </TableCell>
      <TableCell>
        <Checkbox
          checked={permissions.siteManager}
          disabled={permissionsDisabled}
          aria-label="Site manager"
          onCheckedChange={(checked) => {
            if (siteManagerSession) {
              void model.savePermissions(listedUser, buildPermissionUpdate(listedUser, 'siteManager', checked))
            }
          }}
        />
      </TableCell>
      <TableCell>
        <Checkbox
          checked={permissions.problemManager}
          disabled={inheritedPermissionsDisabled}
          aria-label="Problem manager"
          onCheckedChange={(checked) => {
            if (siteManagerSession) {
              void model.savePermissions(listedUser, buildPermissionUpdate(listedUser, 'problemManager', checked))
            }
          }}
        />
      </TableCell>
      <TableCell>
        <Checkbox
          checked={permissions.contestManager}
          disabled={inheritedPermissionsDisabled}
          aria-label="Contest manager"
          onCheckedChange={(checked) => {
            if (siteManagerSession) {
              void model.savePermissions(listedUser, buildPermissionUpdate(listedUser, 'contestManager', checked))
            }
          }}
        />
      </TableCell>
      <TableCell className="text-right">
        <ConfirmActionDialog
          title={t('siteManage.deleteUserTitle')}
          description={t('siteManage.deleteUserDescription', { username: displayNameValue(listedUser.displayName) })}
          confirmLabel={t('siteManage.deleteUserAction')}
          destructive
          onConfirm={() => {
            void model.deleteUser(listedUser)
          }}
          trigger={
            <Button
              type="button"
              variant="outline"
              size="sm"
              className="size-8 rounded-full border-rose-300 bg-white p-0 text-rose-700 hover:bg-rose-50 hover:text-rose-800"
              aria-label={`Delete ${displayNameValue(listedUser.displayName)}`}
              disabled={permissionsDisabled}
            >
              <Trash2 className="size-4" />
            </Button>
          }
        />
      </TableCell>
    </TableRow>
  )
}
