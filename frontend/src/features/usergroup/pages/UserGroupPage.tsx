import { Link, Navigate } from 'react-router-dom'
import { ArrowRight, FolderKanban, Users } from 'lucide-react'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { displayNameValue, usernameValue } from '@/features/auth/domain/auth'
import { useSessionGuard } from '@/features/auth/hooks/use-session-guard'
import { userGroupDescriptionValue, userGroupNameValue, userGroupSlugValue } from '@/features/usergroup/domain/usergroup'
import { useUserGroupPageModel } from '@/features/usergroup/hooks/use-usergroup-page-model'
import { AncestorNavigation } from '@/shared/components/ancestor-navigation'
import { usePageTitle } from '@/shared/hooks/use-page-title'

export function UserGroupPage() {
  usePageTitle('Qiwen Online Judge - User Groups')
  const { session: user, navigationIntent } = useSessionGuard()
  const model = useUserGroupPageModel()

  if (navigationIntent) {
    return <Navigate replace={navigationIntent.replace} to={navigationIntent.to} />
  }

  if (!user) {
    return <Navigate replace to="/login" />
  }

  return (
    <main className="min-h-screen bg-[linear-gradient(180deg,#f8fafc_0%,#eef5f8_100%)] px-6 py-12 sm:px-8">
      <section className="mx-auto max-w-5xl">
        <div className="mb-8 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="space-y-2">
            <p className="text-sm uppercase tracking-[0.25em] text-slate-500">Qiwen Online Judge</p>
            <h1 className="font-['Georgia'] text-4xl font-semibold tracking-tight text-slate-950">User Groups</h1>
            <p className="text-sm text-slate-600">
              Signed in as {displayNameValue(user.displayName)} ({usernameValue(user.username)}).
            </p>
          </div>

          <AncestorNavigation />
        </div>

        {model.errorMessage ? (
          <Alert variant="destructive" className="mb-6 rounded-2xl border-rose-200 bg-rose-50/95">
            <AlertDescription className="text-rose-700">{model.errorMessage}</AlertDescription>
          </Alert>
        ) : null}

        <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
          <CardHeader>
            <div className="flex items-center gap-3">
              <div className="flex size-12 items-center justify-center rounded-2xl bg-sky-100 text-sky-700">
                <FolderKanban className="size-5" />
              </div>
              <div>
                <CardTitle className="text-xl text-slate-950">User Groups</CardTitle>
                <CardDescription>
                  Create groups, manage membership, and control collaboration in one place.
                </CardDescription>
              </div>
            </div>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="flex justify-start">
              <Button asChild variant="outline" className="rounded-2xl border-slate-300 bg-white">
                <Link to="/user-groups/new">
                  <Users className="size-4" />
                  Create User Group
                </Link>
              </Button>
            </div>
            {model.isLoading ? (
              <p className="text-sm text-slate-500">Loading user groups...</p>
            ) : model.groups.length === 0 ? (
              <p className="text-sm text-slate-500">No user groups available yet.</p>
            ) : (
              model.groups.map((group) => (
                <div key={group.id} className="rounded-2xl border border-slate-200 bg-slate-50 p-5">
                  <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
                    <div className="space-y-2">
                      <h2 className="text-lg font-semibold text-slate-950">{userGroupNameValue(group.name)}</h2>
                      <p className="font-mono text-xs text-slate-500">{userGroupSlugValue(group.slug)}</p>
                      <p className="text-sm leading-7 text-slate-600">
                        {userGroupDescriptionValue(group.description) || 'No description provided.'}
                      </p>
                    </div>

                    <Button asChild variant="outline" className="rounded-2xl border-slate-300 bg-white">
                      <Link to={`/user-groups/${userGroupSlugValue(group.slug)}`}>
                        Open detail
                        <ArrowRight className="size-4" />
                      </Link>
                    </Button>
                  </div>
                </div>
              ))
            )}
          </CardContent>
        </Card>
      </section>
    </main>
  )
}
