import { Link, Navigate } from 'react-router-dom'
import { LogOut, Settings2, Trash2 } from 'lucide-react'

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
import { AncestorNavigation } from '@/shared/components/ancestor-navigation'
import { ConfirmActionDialog } from '@/shared/components/confirm-action-dialog'
import { usePageTitle } from '@/shared/hooks/use-page-title'
import { useSiteManageModel } from '@/features/site-management/hooks/use-site-manage-model'
import { useSessionGuard } from '@/features/auth/hooks/use-session-guard'

export function SiteManagePage() {
  usePageTitle('Qiwen Online Judge - Site Management')
  const { session: user, siteManagerSession, signOut, navigationIntent: guardNavigationIntent } =
    useSessionGuard({ requireSiteManager: true })
  const {
    users,
    userListError,
    statusMessage,
    isLoadingUsers,
    updatingUsername,
    deletingUsername,
    navigationIntent: modelNavigationIntent,
    savePermissions,
    deleteUser,
  } = useSiteManageModel(Boolean(siteManagerSession))

  if (guardNavigationIntent) {
    return <Navigate replace={guardNavigationIntent.replace} to={guardNavigationIntent.to} />
  }

  if (modelNavigationIntent) {
    return <Navigate replace={modelNavigationIntent.replace} to={modelNavigationIntent.to} />
  }

  if (!user) {
    return <Navigate replace to="/login" />
  }

  const isProtectedAdmin = (listedUser: AuthUserListItem) => usernameValue(listedUser.username) === 'admin'
  return (
    <main className="min-h-screen bg-[linear-gradient(180deg,#fffaf4_0%,#f4efe5_100%)] px-6 py-12 sm:px-8">
      <section className="mx-auto max-w-6xl">
        <div className="mb-8 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="space-y-2">
            <p className="text-sm uppercase tracking-[0.25em] text-stone-500">Qiwen Online Judge</p>
            <h1 className="font-['Georgia'] text-4xl font-semibold tracking-tight text-stone-950">
              Site Management
            </h1>
            <p className="text-sm text-stone-600">
              Signed in as {displayNameValue(user.displayName)} ({usernameValue(user.username)}).
            </p>
          </div>

          <div className="flex flex-col gap-3 sm:flex-row">
            <AncestorNavigation buttonClassName="rounded-full border-stone-300 bg-white" />
            <Button
              type="button"
              variant="outline"
              className="rounded-full border-stone-300 bg-white"
              onClick={() => {
                void signOut()
              }}
            >
              <LogOut className="size-4" />
              Sign out
            </Button>
          </div>
        </div>

        <Card className="border-stone-200 bg-white shadow-[0_24px_60px_rgba(28,25,23,0.08)]">
          <CardHeader>
            <div className="flex items-center gap-3">
              <div className="flex size-12 items-center justify-center rounded-2xl bg-orange-100 text-orange-700">
                <Settings2 className="size-5" />
              </div>
              <div>
                <CardTitle className="text-xl text-stone-950">User Management</CardTitle>
                <CardDescription>
                  Site managers can review all registered users and current permission flags.
                </CardDescription>
              </div>
            </div>
          </CardHeader>
          <CardContent className="space-y-4">
            {statusMessage ? (
              <Alert className="rounded-2xl border-emerald-200 bg-emerald-50/95">
                <AlertDescription className="text-emerald-700">{statusMessage}</AlertDescription>
              </Alert>
            ) : null}
            {userListError ? (
              <Alert variant="destructive" className="rounded-2xl border-rose-200 bg-rose-50/95">
                <AlertDescription className="text-rose-700">{userListError}</AlertDescription>
              </Alert>
            ) : null}
            {isLoadingUsers ? (
              <p className="text-sm text-stone-500">Loading users...</p>
            ) : users.length === 0 ? (
              <div className="rounded-3xl border border-dashed border-stone-300 bg-stone-50 px-6 py-10 text-center">
                <p className="text-base font-medium text-stone-900">No users are available yet.</p>
                <p className="mt-2 text-sm leading-7 text-stone-600">
                  This management flow is still valid: the empty state is explicit, and you can return to the dashboard.
                </p>
                <Button asChild variant="outline" className="mt-5 rounded-full border-stone-300 bg-white">
                  <Link to="/">Back to Dashboard</Link>
                </Button>
              </div>
            ) : (
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Username</TableHead>
                    <TableHead>Display name</TableHead>
                    <TableHead>Email</TableHead>
                    <TableHead>Settings</TableHead>
                    <TableHead>Site manager</TableHead>
                    <TableHead>Problem manager</TableHead>
                    <TableHead className="text-right">Actions</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {users.map((listedUser) => (
                    <TableRow key={usernameValue(listedUser.username)}>
                      <TableCell className="font-medium text-stone-900">
                        {usernameValue(listedUser.username)}
                      </TableCell>
                      <TableCell>{displayNameValue(listedUser.displayName)}</TableCell>
                      <TableCell>{emailAddressValue(listedUser.email)}</TableCell>
                      <TableCell>
                        <Button asChild variant="outline" size="sm" className="rounded-full border-stone-300 bg-white">
                          <Link to={`/user/${usernameValue(listedUser.username)}/settings`}>
                            Open settings
                          </Link>
                        </Button>
                      </TableCell>
                      <TableCell>
                        <Checkbox
                          checked={listedUser.siteManager}
                          disabled={
                            updatingUsername !== null || deletingUsername !== null || isProtectedAdmin(listedUser)
                          }
                          aria-label="Site manager"
                          onCheckedChange={(checked) => {
                            if (siteManagerSession) {
                              void savePermissions(listedUser, {
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
                          disabled={
                            updatingUsername !== null || deletingUsername !== null || isProtectedAdmin(listedUser)
                          }
                          aria-label="Problem manager"
                          onCheckedChange={(checked) => {
                            if (siteManagerSession) {
                              void savePermissions(listedUser, {
                                siteManager: listedUser.siteManager,
                                problemManager: checked === true,
                              })
                            }
                          }}
                        />
                      </TableCell>
                      <TableCell className="text-right">
                        <ConfirmActionDialog
                          title="Delete user?"
                          description={`Delete ${usernameValue(listedUser.username)} from the site. This user must not own any problems, problem sets, or user groups before deletion.`}
                          confirmLabel="Delete user"
                          destructive
                          onConfirm={() => {
                            void deleteUser(listedUser)
                          }}
                          trigger={
                            <Button
                              type="button"
                              variant="outline"
                              size="sm"
                              className="size-8 rounded-full border-rose-300 bg-white p-0 text-rose-700 hover:bg-rose-50 hover:text-rose-800"
                              aria-label={`Delete ${usernameValue(listedUser.username)}`}
                              disabled={
                                updatingUsername !== null || deletingUsername !== null || isProtectedAdmin(listedUser)
                              }
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
      </section>
    </main>
  )
}
