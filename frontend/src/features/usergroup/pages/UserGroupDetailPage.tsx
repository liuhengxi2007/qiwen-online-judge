import { useState } from 'react'
import { Navigate, useNavigate, useParams } from 'react-router-dom'
import { PencilLine, ShieldPlus, Trash2, Users } from 'lucide-react'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { RadioGroup, RadioGroupItem } from '@/components/ui/radio-group'
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
import { ConfirmActionDialog } from '@/shared/components/confirm-action-dialog'
import { AncestorNavigation } from '@/shared/components/ancestor-navigation'
import { useBeforeUnloadPrompt } from '@/shared/hooks/use-before-unload-prompt'
import { usePageTitle } from '@/shared/hooks/use-page-title'
import { useI18n } from '@/shared/i18n/i18n'

export function UserGroupDetailPage() {
  const { t } = useI18n()
  usePageTitle(t('userGroup.detail.pageTitle'))
  const { session: user, navigationIntent } = useSessionGuard()
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
  const [ownershipTargetUsername, setOwnershipTargetUsername] = useState<string | null>(null)
  const ownershipTargetMember =
    ownershipTargetUsername === null
      ? null
      : (model.userGroup?.members.find((member) => member.username === ownershipTargetUsername) ?? null)
  const hasUnsavedChanges =
    (model.userGroup !== null &&
      (model.name !== userGroupNameValue(model.userGroup.name) ||
        model.description !== userGroupDescriptionValue(model.userGroup.description))) ||
    model.memberUsername.trim().length > 0 ||
    model.memberRole !== 'member'

  useBeforeUnloadPrompt(hasUnsavedChanges)

  return (
    <main className="min-h-screen bg-[linear-gradient(180deg,#f8fafc_0%,#eff4fb_100%)] px-6 py-12 sm:px-8">
      <section className="mx-auto max-w-6xl">
        <div className="mb-8 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="space-y-2">
            <p className="text-sm uppercase tracking-[0.25em] text-slate-500">{t('common.siteName')}</p>
            <h1 className="font-['Georgia'] text-4xl font-semibold tracking-tight text-slate-950">{t('userGroup.detail.heading')}</h1>
            <p className="text-sm text-slate-600">
              {t('common.signedInAs', { displayName: displayNameValue(user.displayName), username: usernameValue(user.username) })}
            </p>
          </div>

          <AncestorNavigation />
        </div>

        {!model.isLoading && !model.userGroup && model.errorMessage ? (
          <Alert variant="destructive" className="mb-6 rounded-2xl border-rose-200 bg-rose-50/95">
            <AlertDescription className="text-rose-700">{model.errorMessage}</AlertDescription>
          </Alert>
        ) : null}

        {model.isLoading ? (
          <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
            <CardContent className="py-10 text-sm text-slate-500">{t('userGroup.detail.loading')}</CardContent>
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
                  {userGroupDescriptionValue(model.userGroup.description) || t('common.noDescription')}
                </p>
                <p className="text-xs uppercase tracking-[0.18em] text-slate-400">
                  {t('userGroup.detail.owner', { username: usernameValue(model.userGroup.ownerUsername) })}
                </p>
              </CardContent>
            </Card>

            {model.canManage ? (
              <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
                <CardHeader>
                  <div className="flex items-center gap-3">
                    <div className="flex size-12 items-center justify-center rounded-2xl bg-sky-100 text-sky-700">
                      <PencilLine className="size-5" />
                    </div>
                    <div>
                      <CardTitle className="text-xl text-slate-950">{t('userGroup.detail.editTitle')}</CardTitle>
                      <CardDescription>{t('userGroup.detail.editDescription')}</CardDescription>
                    </div>
                  </div>
                </CardHeader>
                <CardContent className="space-y-5">
                  <div className="space-y-2">
                    <Label htmlFor="user-group-name">{t('userGroup.create.name')}</Label>
                    <Input id="user-group-name" value={model.name} onChange={(event) => model.setName(event.target.value)} />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="user-group-description">{t('userGroup.create.description')}</Label>
                    <Textarea
                      id="user-group-description"
                      value={model.description}
                      onChange={(event) => model.setDescription(event.target.value)}
                    />
                  </div>
                  <Button
                    type="button"
                    className="rounded-2xl bg-sky-300 text-sky-950 hover:bg-sky-400"
                    disabled={model.isSaving}
                    onClick={() => {
                      void model.save()
                    }}
                  >
                    {model.isSaving ? t('userGroup.detail.saving') : t('userGroup.detail.save')}
                  </Button>
                  {model.saveErrorMessage ? (
                    <Alert variant="destructive" className="rounded-2xl border-rose-200 bg-rose-50/95">
                      <AlertDescription className="text-rose-700">{model.saveErrorMessage}</AlertDescription>
                    </Alert>
                  ) : null}
                  {model.saveSuccessMessage ? (
                    <Alert className="rounded-2xl border-emerald-200 bg-emerald-50/95">
                      <AlertDescription className="text-emerald-700">{model.saveSuccessMessage}</AlertDescription>
                    </Alert>
                  ) : null}
                </CardContent>
              </Card>
            ) : null}

            <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
              <CardHeader>
                <div className="flex items-center gap-3">
                    <div className="flex size-12 items-center justify-center rounded-2xl bg-sky-100 text-sky-700">
                      <Users className="size-5" />
                    </div>
                    <div>
                    <CardTitle className="text-xl text-slate-950">{t('userGroup.detail.membersTitle')}</CardTitle>
                    <CardDescription>{t('userGroup.detail.membersDescription')}</CardDescription>
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
                      <div className="flex flex-col items-stretch gap-3 sm:items-end">
                        <RadioGroup
                          value={member.role}
                          disabled={
                            !model.canManageMemberRoles ||
                            member.role === 'owner' ||
                            model.activeUpdatingUsername === member.username ||
                            model.activeRemovingUsername === member.username
                          }
                          onValueChange={(value) => {
                            if (value !== 'owner' && value !== 'manager' && value !== 'member') {
                              return
                            }

                            if (value === member.role) {
                              return
                            }

                            if (value === 'owner') {
                              setOwnershipTargetUsername(member.username)
                              return
                            }

                            void model.updateMemberRole(member.username, value)
                          }}
                          className="grid gap-2 sm:grid-cols-3"
                        >
                          <label className="flex items-center gap-2 rounded-2xl border border-slate-200 bg-white px-3 py-2 text-xs text-slate-600">
                            <RadioGroupItem
                              value="owner"
                              disabled={
                                !model.canManageMemberRoles ||
                                member.role === 'owner' ||
                                model.activeUpdatingUsername === member.username ||
                                model.activeRemovingUsername === member.username
                              }
                            />
                            <span>{t('userGroup.detail.role.owner')}</span>
                          </label>
                          <label className="flex items-center gap-2 rounded-2xl border border-slate-200 bg-white px-3 py-2 text-xs text-slate-600">
                            <RadioGroupItem
                              value="manager"
                              disabled={
                                !model.canManageMemberRoles ||
                                member.role === 'owner' ||
                                model.activeUpdatingUsername === member.username ||
                                model.activeRemovingUsername === member.username
                              }
                            />
                            <span>{t('userGroup.detail.role.manager')}</span>
                          </label>
                          <label className="flex items-center gap-2 rounded-2xl border border-slate-200 bg-white px-3 py-2 text-xs text-slate-600">
                            <RadioGroupItem
                              value="member"
                              disabled={
                                !model.canManageMemberRoles ||
                                member.role === 'owner' ||
                                model.activeUpdatingUsername === member.username ||
                                model.activeRemovingUsername === member.username
                              }
                            />
                            <span>{t('userGroup.detail.role.member')}</span>
                          </label>
                        </RadioGroup>

                        {member.role !== 'owner' ? (
                          <ConfirmActionDialog
                            title={t('userGroup.detail.removeMemberTitle')}
                            description={t('userGroup.detail.removeMemberDescription', { username: usernameValue(member.username) })}
                            confirmLabel={t('userGroup.detail.removeMemberAction')}
                            destructive
                            onConfirm={() => {
                              void model.removeMember(member.username)
                            }}
                            trigger={
                              <Button
                                type="button"
                                variant="outline"
                                size="sm"
                                className="size-8 rounded-full border-rose-300 bg-white p-0 text-rose-700 hover:bg-rose-50 hover:text-rose-800"
                                aria-label={`Remove ${usernameValue(member.username)} from the group`}
                                disabled={
                                  !model.canRemoveMember(member.username, member.role) ||
                                  model.activeUpdatingUsername !== null ||
                                  model.activeRemovingUsername !== null
                                }
                              >
                                <Trash2 className="size-4" />
                              </Button>
                            }
                          />
                        ) : null}
                      </div>
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
                      <CardTitle className="text-xl text-slate-950">{t('userGroup.detail.addMemberTitle')}</CardTitle>
                      <CardDescription>{t('userGroup.detail.addMemberDescription')}</CardDescription>
                    </div>
                  </div>
                </CardHeader>
                <CardContent className="space-y-4">
                  <div className="space-y-2">
                    <Label htmlFor="member-username">{t('common.username')}</Label>
                    <Input
                      id="member-username"
                      value={model.memberUsername}
                      placeholder={t('userGroup.detail.memberUsernamePlaceholder')}
                      onChange={(event) => model.setMemberUsername(event.target.value.toLowerCase())}
                    />
                  </div>
                  <div className="space-y-2">
                    <Label>{t('userGroup.detail.roleLabel')}</Label>
                    <Select value={model.memberRole} onValueChange={(value) => model.setMemberRole(value as 'manager' | 'member')}>
                      <SelectTrigger>
                        <SelectValue placeholder={t('userGroup.detail.rolePlaceholder')} />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value="member">{t('userGroup.detail.role.member')}</SelectItem>
                        <SelectItem value="manager">{t('userGroup.detail.role.manager')}</SelectItem>
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
                    {model.isAddingMember ? t('userGroup.detail.addingMember') : t('userGroup.detail.addMember')}
                  </Button>
                  {model.addMemberErrorMessage ? (
                    <Alert variant="destructive" className="rounded-2xl border-rose-200 bg-rose-50/95">
                      <AlertDescription className="text-rose-700">{model.addMemberErrorMessage}</AlertDescription>
                    </Alert>
                  ) : null}
                  {model.addMemberSuccessMessage ? (
                    <Alert className="rounded-2xl border-emerald-200 bg-emerald-50/95">
                      <AlertDescription className="text-emerald-700">{model.addMemberSuccessMessage}</AlertDescription>
                    </Alert>
                  ) : null}
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
                      <CardTitle className="text-xl text-rose-950">{t('userGroup.detail.deleteTitle')}</CardTitle>
                      <CardDescription>{t('userGroup.detail.deleteDescription')}</CardDescription>
                    </div>
                  </div>
                </CardHeader>
                <CardContent>
                  <ConfirmActionDialog
                    title={t('userGroup.detail.deleteConfirmTitle')}
                    description={t('userGroup.detail.deleteConfirmDescription')}
                    confirmLabel={model.isDeleting ? t('problemSet.detail.deletingAction') : t('userGroup.detail.deleteAction')}
                    destructive
                    onConfirm={() => {
                      void model.deleteCurrentUserGroup().then((deleted) => {
                        if (deleted) {
                          void navigate('/user-groups')
                        }
                      })
                    }}
                    trigger={
                      <Button
                        type="button"
                        variant="outline"
                        className="rounded-2xl border-rose-300 bg-white text-rose-700 hover:bg-rose-100 hover:text-rose-800"
                        disabled={model.isDeleting}
                      >
                        {model.isDeleting ? t('problemSet.detail.deletingAction') : t('userGroup.detail.deleteAction')}
                      </Button>
                    }
                  />
                </CardContent>
              </Card>
            ) : null}
          </div>
        ) : null}
      </section>
      <ConfirmActionDialog
        open={ownershipTargetMember !== null}
        onOpenChange={(open) => {
          if (!open) {
            setOwnershipTargetUsername(null)
          }
        }}
        title={t('userGroup.detail.transferOwnershipTitle')}
        description={
          ownershipTargetMember
            ? t('userGroup.detail.transferOwnershipDescription', {
                username: usernameValue(ownershipTargetMember.username),
              })
            : ''
        }
        confirmLabel={t('userGroup.detail.transferOwnershipAction')}
        onConfirm={() => {
          if (!ownershipTargetMember) {
            return
          }

          void model.updateMemberRole(ownershipTargetMember.username, 'owner').then(() => {
            setOwnershipTargetUsername(null)
          })
        }}
      />
    </main>
  )
}
