import { useState } from 'react'
import { Navigate, useParams } from 'react-router-dom'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { useSessionGuard } from '@/pages/hooks/useSessionGuard'
import { userGroupDescriptionValue } from '@/objects/usergroup/UserGroupDescription'
import { userGroupNameValue } from '@/objects/usergroup/UserGroupName'
import { parseUserGroupSlug } from '@/objects/usergroup/UserGroupSlug'
import type { UserGroupSlug } from '@/objects/usergroup/UserGroupSlug'
import type { Username } from '@/objects/user/Username'
import { UserGroupAddMemberCard } from './components/UserGroupAddMemberCard'
import { UserGroupDeleteCard } from './components/UserGroupDeleteCard'
import { UserGroupEditCard } from './components/UserGroupEditCard'
import { UserGroupMembersCard } from './components/UserGroupMembersCard'
import { UserGroupOwnershipTransferDialog } from './components/UserGroupOwnershipTransferDialog'
import { UserGroupSummaryCard } from './components/UserGroupSummaryCard'
import { useUserGroupDetailPageModel } from './hooks/useUserGroupDetailPageModel'
import { PageLoadingCard } from '@/pages/components/PageLoadingCard'
import { PageShell } from '@/pages/components/PageShell'
import { useBeforeUnloadPrompt } from '@/pages/hooks/useBeforeUnloadPrompt'
import { usePageTitle } from '@/pages/hooks/usePageTitle'
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
    <>
      <PageShell
        title={t('userGroup.detail.heading')}
        mainClassName="bg-[linear-gradient(180deg,#f8fafc_0%,#eff4fb_100%)]"
      >
        {!model.isLoading && !model.userGroup && model.errorMessage ? (
          <Alert variant="destructive" className="mb-6 rounded-2xl border-rose-200 bg-rose-50/95">
            <AlertDescription className="text-rose-700">{model.errorMessage}</AlertDescription>
          </Alert>
        ) : null}

        {model.isLoading ? (
          <PageLoadingCard message={t('userGroup.detail.loading')} />
        ) : model.userGroup ? (
          <div className="space-y-6">
            <UserGroupSummaryCard model={model} />
            <UserGroupEditCard model={model} />
            <UserGroupMembersCard model={model} setOwnershipTargetUsername={setOwnershipTargetUsername} />
            <UserGroupAddMemberCard model={model} />
            <UserGroupDeleteCard model={model} />
          </div>
        ) : null}
      </PageShell>
      <UserGroupOwnershipTransferDialog
        model={model}
        ownershipTargetMember={ownershipTargetMember}
        onOpenChange={(open) => {
          if (!open) {
            setOwnershipTargetUsername(null)
          }
        }}
      />
    </>
  )
}
