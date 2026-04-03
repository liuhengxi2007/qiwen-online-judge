import { Link, Navigate, useNavigate, useParams } from 'react-router-dom'
import { ArrowLeft, LogOut, PencilLine, ShieldPlus, Trash2, Users } from 'lucide-react'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Textarea } from '@/components/ui/textarea'
import { displayNameValue, usernameValue } from '@/features/auth/domain/auth'
import { useSessionGuard } from '@/features/auth/hooks/use-session-guard'
import {
  parseUserGroupSlug,
  userGroupDescriptionValue,
  userGroupNameValue,
  userGroupSlugValue,
} from '@/features/usergroup/domain/usergroup'
import { useUserGroupDetailPageModel } from '@/features/usergroup/hooks/use-usergroup-detail-page-model'
import { usePageTitle } from '@/shared/hooks/use-page-title'

export function UserGroupDetailPage() {
  usePageTitle('Qiwen Online Judge - User Group Detail')
  const { session: user, signOut, navigationIntent } = useSessionGuard()
  const navigate = useNavigate()
  const { slug } = useParams<{ slug: string }>()

  if (navigationIntent) {
    return <Navigate replace={navigationIntent.replace} to={navigationIntent.to} />
  }

  if (!user) {
    return <Navigate replace to="/login" />
  }

  const slugResult = parseUserGroupSlug(slug ?? '')
  if (!slugResult.ok) {
    return <Navigate replace to="/user-groups" />
  }

  const model = useUserGroupDetailPageModel(slugResult.value, user.username, user.siteManager)

  return (
    <main className="min-h-screen bg-[linear-gradient(180deg,#f8fafc_0%,#eff4fb_100%)] px-6 py-12 sm:px-8">
      <section className="mx-auto max-w-5xl">
        <div className="mb-8 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="space-y-2">
            <p className="text-sm uppercase tracking-[0.25em] text-slate-500">Qiwen Online Judge</p>
            <h1 className="font-['Georgia'] text-4xl font-semibold tracking-tight text-slate-950">User Group Detail</h1>
            <p className="text-sm text-slate-600">
              Signed in as {displayNameValue(user.displayName)} ({usernameValue(user.username)}).
            </p>
          </div>

          <div className="flex flex-col gap-3 sm:flex-row">
            <Button asChild variant="outline" className="rounded-full border-slate-300 bg-white">
              <Link to="/user-groups">
                <ArrowLeft className="size-4" />
                Back to User Groups
              </Link>
            </Button>
            <Button
              type="button"
              variant="outline"
              className="rounded-full border-slate-300 bg-white"
              onClick={() => {
                void signOut()
              }}
            >
              <LogOut className="size-4" />
              Sign out
            </Button>
          </div>
        </div>

        {model.errorMessage ? (
          <Alert variant="destructive" className="mb-6 rounded-2xl border-rose-200 bg-rose-50/95">
            <AlertDescription className="text-rose-700">{model.errorMessage}</AlertDescription>
          </Alert>
        ) : null}

        {model.successMessage ? (
          <Alert className="mb-6 rounded-2xl border-emerald-200 bg-emerald-50/95">
            <AlertDescription className="text-emerald-700">{model.successMessage}</AlertDescription>
          </Alert>
        ) : null}

        {model.isLoading ? (
          <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
            <CardContent className="py-10 text-sm text-slate-500">Loading user group detail...</CardContent>
          </Card>
        ) : model.userGroup ? (
          <div className="space-y-6">
            <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
              <CardHeader>
                <CardTitle className="text-2xl text-slate-950">{userGroupNameValue(model.userGroup.name)}</CardTitle>
                <CardDescription className="mt-2 font-mono text-sm text-slate-500">
                  {userGroupSlugValue(model.userGroup.slug)}
                </CardDescription>
              </CardHeader>
              <CardContent className="space-y-4">
                <p className="text-sm leading-7 text-slate-600">
                  {userGroupDescriptionValue(model.userGroup.description) || 'No description provided.'}
                </p>
                <p className="text-xs uppercase tracking-[0.18em] text-slate-400">
                  Owner {usernameValue(model.userGroup.ownerUsername)}
                </p>
              </CardContent>
            </Card>

            {model.canManage ? (
              <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
                <CardHeader>
                  <div className="flex items-center gap-3">
                    <div className="flex size-12 items-center justify-center rounded-2xl bg-amber-100 text-amber-700">
                      <PencilLine className="size-5" />
                    </div>
                    <div>
                      <CardTitle className="text-xl text-slate-950">Edit User Group</CardTitle>
                      <CardDescription>Update the name and description of this group.</CardDescription>
                    </div>
                  </div>
                </CardHeader>
                <CardContent className="space-y-5">
                  <div className="space-y-2">
                    <Label htmlFor="user-group-name">Name</Label>
                    <Input id="user-group-name" value={model.name} onChange={(event) => model.setName(event.target.value)} />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="user-group-description">Description</Label>
                    <Textarea
                      id="user-group-description"
                      value={model.description}
                      onChange={(event) => model.setDescription(event.target.value)}
                    />
                  </div>
                  <Button
                    type="button"
                    className="rounded-2xl bg-slate-950 text-white hover:bg-slate-800"
                    disabled={model.isSaving}
                    onClick={() => {
                      void model.save()
                    }}
                  >
                    {model.isSaving ? 'Saving changes...' : 'Save changes'}
                  </Button>
                </CardContent>
              </Card>
            ) : null}

            <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
              <CardHeader>
                <div className="flex items-center gap-3">
                  <div className="flex size-12 items-center justify-center rounded-2xl bg-violet-100 text-violet-700">
                    <Users className="size-5" />
                  </div>
                  <div>
                    <CardTitle className="text-xl text-slate-950">Members</CardTitle>
                    <CardDescription>Membership is the first piece of the future resource permission chain.</CardDescription>
                  </div>
                </div>
              </CardHeader>
              <CardContent className="space-y-4">
                {model.userGroup.members.map((member) => (
                  <div key={member.username} className="rounded-2xl border border-slate-200 bg-slate-50 p-4">
                    <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
                      <div>
                        <p className="text-sm font-medium text-slate-900">{displayNameValue(member.displayName)}</p>
                        <p className="font-mono text-xs text-slate-500">{usernameValue(member.username)}</p>
                      </div>
                      <Badge variant="outline">{member.role}</Badge>
                    </div>
                  </div>
                ))}
              </CardContent>
            </Card>

            {model.canManage ? (
              <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
                <CardHeader>
                  <div className="flex items-center gap-3">
                    <div className="flex size-12 items-center justify-center rounded-2xl bg-sky-100 text-sky-700">
                      <ShieldPlus className="size-5" />
                    </div>
                    <div>
                      <CardTitle className="text-xl text-slate-950">Add Member</CardTitle>
                      <CardDescription>Add an existing user into this group with a typed group role.</CardDescription>
                    </div>
                  </div>
                </CardHeader>
                <CardContent className="space-y-4">
                  <div className="space-y-2">
                    <Label htmlFor="member-username">Username</Label>
                    <Input
                      id="member-username"
                      value={model.memberUsername}
                      placeholder="alice"
                      onChange={(event) => model.setMemberUsername(event.target.value)}
                    />
                  </div>
                  <div className="space-y-2">
                    <Label>Role</Label>
                    <Select value={model.memberRole} onValueChange={(value) => model.setMemberRole(value as 'owner' | 'manager' | 'member')}>
                      <SelectTrigger>
                        <SelectValue placeholder="Select role" />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value="manager">Manager</SelectItem>
                        <SelectItem value="member">Member</SelectItem>
                        <SelectItem value="owner">Owner</SelectItem>
                      </SelectContent>
                    </Select>
                  </div>
                  <Button
                    type="button"
                    variant="outline"
                    className="rounded-2xl border-slate-300 bg-white"
                    disabled={model.isAddingMember}
                    onClick={() => {
                      void model.addMember()
                    }}
                  >
                    {model.isAddingMember ? 'Adding member...' : 'Add member'}
                  </Button>
                </CardContent>
              </Card>
            ) : null}

            {model.canDelete ? (
              <Card className="border-rose-200 bg-rose-50/60 shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
                <CardHeader>
                  <div className="flex items-center gap-3">
                    <div className="flex size-12 items-center justify-center rounded-2xl bg-rose-100 text-rose-700">
                      <Trash2 className="size-5" />
                    </div>
                    <div>
                      <CardTitle className="text-xl text-rose-950">Delete User Group</CardTitle>
                      <CardDescription>This removes the group and all of its membership records.</CardDescription>
                    </div>
                  </div>
                </CardHeader>
                <CardContent>
                  <Button
                    type="button"
                    variant="outline"
                    className="rounded-2xl border-rose-300 bg-white text-rose-700 hover:bg-rose-100 hover:text-rose-800"
                    disabled={model.isDeleting}
                    onClick={() => {
                      const confirmed = window.confirm('Delete this user group? This action cannot be undone.')
                      if (!confirmed) {
                        return
                      }

                      void model.deleteCurrentUserGroup().then((deleted) => {
                        if (deleted) {
                          void navigate('/user-groups')
                        }
                      })
                    }}
                  >
                    {model.isDeleting ? 'Deleting...' : 'Delete user group'}
                  </Button>
                </CardContent>
              </Card>
            ) : null}
          </div>
        ) : null}
      </section>
    </main>
  )
}
