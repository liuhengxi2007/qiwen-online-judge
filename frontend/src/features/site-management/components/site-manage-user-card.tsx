import { Link } from 'react-router-dom'
import { Settings2, Trash2 } from 'lucide-react'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Checkbox } from '@/components/ui/checkbox'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import {
  displayNameValue,
  emailAddressValue,
  usernameValue,
  type AuthUserListItem,
} from '@/features/auth/domain/auth'
import type { useSiteManageModel } from '@/features/site-management/hooks/use-site-manage-model'
import { ConfirmActionDialog } from '@/shared/components/confirm-action-dialog'
import { useI18n } from '@/shared/i18n/i18n'

type SiteManageModel = ReturnType<typeof useSiteManageModel>

type SiteManageUserCardProps = {
  model: SiteManageModel
  siteManagerSession: boolean
}

export function SiteManageUserCard({ model, siteManagerSession }: SiteManageUserCardProps) {
  const { t } = useI18n()
  const isProtectedAdmin = (listedUser: AuthUserListItem) => usernameValue(listedUser.username) === 'admin'

  return (
    <Card className="border-stone-200 bg-white shadow-[0_24px_60px_rgba(28,25,23,0.08)]">
      <CardHeader>
        <div className="flex items-center gap-3">
          <div className="flex size-12 items-center justify-center rounded-2xl bg-amber-100 text-amber-700">
            <Settings2 className="size-5" />
          </div>
          <div>
            <CardTitle className="text-xl text-stone-950">{t('siteManage.userManagementTitle')}</CardTitle>
            <CardDescription>{t('siteManage.userManagementDescription')}</CardDescription>
          </div>
        </div>
      </CardHeader>
      <CardContent className="space-y-4">
        {model.statusMessage ? (
          <Alert className="rounded-2xl border-emerald-200 bg-emerald-50/95">
            <AlertDescription className="text-emerald-700">{model.statusMessage}</AlertDescription>
          </Alert>
        ) : null}
        {model.userListError ? (
          <Alert variant="destructive" className="rounded-2xl border-rose-200 bg-rose-50/95">
            <AlertDescription className="text-rose-700">{model.userListError}</AlertDescription>
          </Alert>
        ) : null}
        {model.isLoadingUsers ? (
          <p className="text-sm text-stone-500">{t('siteManage.loadingUsers')}</p>
        ) : model.users.length === 0 ? (
          <div className="rounded-3xl border border-dashed border-stone-300 bg-stone-50 px-6 py-10 text-center">
            <p className="text-base font-medium text-stone-900">{t('siteManage.emptyUsersTitle')}</p>
            <p className="mt-2 text-sm leading-7 text-stone-600">{t('siteManage.emptyUsersDescription')}</p>
            <Button asChild variant="outline" className="mt-5 rounded-full border-stone-300 bg-white">
              <Link to="/">{t('siteManage.backToDashboard')}</Link>
            </Button>
          </div>
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>{t('common.username')}</TableHead>
                <TableHead>{t('common.displayName')}</TableHead>
                <TableHead>{t('common.email')}</TableHead>
                <TableHead>{t('common.settings')}</TableHead>
                <TableHead>{t('siteManage.siteManager')}</TableHead>
                <TableHead>{t('siteManage.problemManager')}</TableHead>
                <TableHead className="text-right">{t('common.actions')}</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {model.users.map((listedUser) => (
                <TableRow key={usernameValue(listedUser.username)}>
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
                      checked={listedUser.siteManager}
                      disabled={model.updatingUsername !== null || model.deletingUsername !== null || isProtectedAdmin(listedUser)}
                      aria-label="Site manager"
                      onCheckedChange={(checked) => {
                        if (siteManagerSession) {
                          void model.savePermissions(listedUser, {
                            siteManager: checked === true,
                            problemManager: listedUser.problemManager,
                          })
                        }
                      }}
                    />
                  </TableCell>
                  <TableCell>
                    <Checkbox
                      checked={listedUser.problemManager}
                      disabled={model.updatingUsername !== null || model.deletingUsername !== null || isProtectedAdmin(listedUser)}
                      aria-label="Problem manager"
                      onCheckedChange={(checked) => {
                        if (siteManagerSession) {
                          void model.savePermissions(listedUser, {
                            siteManager: listedUser.siteManager,
                            problemManager: checked === true,
                          })
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
                          disabled={model.updatingUsername !== null || model.deletingUsername !== null || isProtectedAdmin(listedUser)}
                        >
                          <Trash2 className="size-4" />
                        </Button>
                      }
                    />
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}
      </CardContent>
    </Card>
  )
}
