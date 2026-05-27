import { useState } from 'react'
import { Navigate, useParams } from 'react-router-dom'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Card, CardContent } from '@/components/ui/card'
import { useSessionGuard } from '@/pages/hooks/use-session-guard'
import { userGroupDescriptionValue } from '@/objects/usergroup/UserGroupDescription'
import { userGroupNameValue } from '@/objects/usergroup/UserGroupName'
import { parseUserGroupSlug } from '@/objects/usergroup/UserGroupSlug'
import type { UserGroupSlug } from '@/objects/usergroup/UserGroupSlug'
import type { Username } from '@/objects/user/Username'
import { UserGroupAddMemberCard } from './components/usergroup-add-member-card'
import { UserGroupDeleteCard } from './components/usergroup-delete-card'
import { UserGroupEditCard } from './components/usergroup-edit-card'
import { UserGroupMembersCard } from './components/usergroup-members-card'
import { UserGroupOwnershipTransferDialog } from './components/usergroup-ownership-transfer-dialog'
import { UserGroupSummaryCard } from './components/usergroup-summary-card'
import { useUserGroupDetailPageModel } from './hooks/use-usergroup-detail-page-model'
import { AppSectionBar } from '@/pages/components/app-section-bar'
import { AncestorNavigation } from '@/pages/components/ancestor-navigation'
import { useBeforeUnloadPrompt } from '@/pages/hooks/use-before-unload-prompt'
import { usePageTitle } from '@/pages/hooks/use-page-title'
import { useI18n } from '@/system/i18n/use-i18n'

export function UserGroupDetailPage() {
  const { t } = useI18n()
  usePageTitle(t('userGroup.detail.pageTitle'))
  const { session: user, navigationIntent } = useSessionGuard()
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

  return <UserGroupDetailPageContent isSiteManager={user.siteManager} userGroupSlug={slugResult.value} viewerUsername={user.username} />
}

function UserGroupDetailPageContent({
  isSiteManager,
  userGroupSlug,
  viewerUsername,
}: {
  isSiteManager: boolean
  userGroupSlug: UserGroupSlug
  viewerUsername: Username
}) {
  const { t } = useI18n()
  const model = useUserGroupDetailPageModel(userGroupSlug, viewerUsername, isSiteManager)
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
          </div>

          <AncestorNavigation />
        </div>

        <AppSectionBar />

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
            <UserGroupSummaryCard model={model} />
            <UserGroupEditCard model={model} />
            <UserGroupMembersCard model={model} setOwnershipTargetUsername={setOwnershipTargetUsername} />
            <UserGroupAddMemberCard model={model} />
            <UserGroupDeleteCard model={model} />
          </div>
        ) : null}
      </section>
      <UserGroupOwnershipTransferDialog
        model={model}
        ownershipTargetMember={ownershipTargetMember}
        onOpenChange={(open) => {
          if (!open) {
            setOwnershipTargetUsername(null)
          }
        }}
      />
    </main>
  )
}
