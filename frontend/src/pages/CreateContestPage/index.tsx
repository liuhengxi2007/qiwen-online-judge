import { useState } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import { CalendarPlus } from 'lucide-react'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { contestSlugValue } from '@/objects/contest/ContestSlug'
import { MarkdownEditorTabs } from '@/pages/components/MarkdownEditorTabs'
import { PageShell } from '@/pages/components/PageShell'
import { ResourceAccessEditor } from '@/pages/components/ResourceAccessEditor'
import { useBeforeUnloadPrompt } from '@/pages/hooks/useBeforeUnloadPrompt'
import { usePageTitle } from '@/pages/hooks/usePageTitle'
import { useSessionGuard } from '@/pages/hooks/useSessionGuard'
import { useI18n } from '@/system/i18n/use-i18n'
import { useCreateContestPageModel } from './hooks/useCreateContestPageModel'

/**
 * 创建比赛页入口，要求具备比赛管理权限后渲染创建表单。
 */
export function CreateContestPage() {
  const { t } = useI18n()
  usePageTitle(t('contest.create.pageTitle'))
  const { session: user, navigationIntent } = useSessionGuard()

  if (navigationIntent) {
    return <Navigate replace={navigationIntent.replace} to={navigationIntent.to} />
  }

  if (!user) {
    return <Navigate replace to="/login" />
  }

  return <CreateContestPageContent canCreate={user.contestManager} />
}

/**
 * 创建比赛页主体，组合权限状态、表单模型和成功跳转。
 */
function CreateContestPageContent({ canCreate }: { canCreate: boolean }) {
  const { t } = useI18n()
  const navigate = useNavigate()
  const model = useCreateContestPageModel(canCreate)
  const [descriptionTab, setDescriptionTab] = useState<'write' | 'preview'>('write')
  const hasUnsavedChanges =
    model.slug.trim().length > 0 ||
    model.title.trim().length > 0 ||
    model.description.trim().length > 0 ||
    model.startAt.trim().length > 0 ||
    model.endAt.trim().length > 0 ||
    model.baseAccess !== 'restricted' ||
    model.grantedUsersInput.trim().length > 0 ||
    model.grantedGroupsInput.trim().length > 0

  useBeforeUnloadPrompt(hasUnsavedChanges)

  return (
    <PageShell title={t('contest.create.heading')} mainClassName="bg-[linear-gradient(180deg,#f0fdfa_0%,#ecfeff_48%,#f8fafc_100%)]">
      <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
        <CardHeader>
          <div className="flex items-center gap-3">
            <div className="flex size-12 items-center justify-center rounded-2xl bg-cyan-100 text-cyan-700">
              <CalendarPlus className="size-5" />
            </div>
            <div>
              <CardTitle className="text-xl text-slate-950">{t('contest.create.cardTitle')}</CardTitle>
              <CardDescription>{t('contest.create.cardDescription')}</CardDescription>
            </div>
          </div>
        </CardHeader>
        <CardContent className="space-y-5">
          {!canCreate ? (
            <Alert className="rounded-2xl border-amber-200 bg-amber-50/95">
              <AlertDescription className="text-amber-900">{t('contest.create.permissionRequired')}</AlertDescription>
            </Alert>
          ) : null}

          <div className="space-y-2">
            <Label htmlFor="contest-slug">{t('contest.create.slug')}</Label>
            <Input id="contest-slug" value={model.slug} onChange={(event) => model.setSlug(event.target.value.toLowerCase())} />
          </div>

          <div className="space-y-2">
            <Label htmlFor="contest-title">{t('contest.create.titleLabel')}</Label>
            <Input id="contest-title" value={model.title} onChange={(event) => model.setTitle(event.target.value)} />
          </div>

          <div className="grid gap-5 md:grid-cols-2">
            <div className="space-y-2">
              <Label htmlFor="contest-start-at">{t('contest.create.startAt')}</Label>
              <Input id="contest-start-at" type="datetime-local" value={model.startAt} onChange={(event) => model.setStartAt(event.target.value)} />
            </div>
            <div className="space-y-2">
              <Label htmlFor="contest-end-at">{t('contest.create.endAt')}</Label>
              <Input id="contest-end-at" type="datetime-local" value={model.endAt} onChange={(event) => model.setEndAt(event.target.value)} />
            </div>
          </div>

          <div className="space-y-2">
            <Label htmlFor="contest-description">{t('contest.create.descriptionLabel')}</Label>
            <MarkdownEditorTabs
              textareaId="contest-description"
              value={model.description}
              tab={descriptionTab}
              onTabChange={setDescriptionTab}
              onValueChange={model.setDescription}
              textareaClassName="min-h-48 font-mono"
            />
            <p className="text-xs text-slate-500">{t('problem.create.markdownHelp')}</p>
          </div>

          <ResourceAccessEditor
            accessPolicy={model.accessPolicy}
            grantedUsersInput={model.grantedUsersInput}
            grantedGroupsInput={model.grantedGroupsInput}
            onBaseAccessChange={model.setBaseAccess}
            onGrantedUsersInputChange={model.setGrantedUsersInput}
            onGrantedGroupsInputChange={model.setGrantedGroupsInput}
          />

          {model.errorMessage ? (
            <Alert variant="destructive" className="rounded-2xl border-rose-200 bg-rose-50/95">
              <AlertDescription className="text-rose-700">{model.errorMessage}</AlertDescription>
            </Alert>
          ) : null}
          {model.successMessage ? (
            <Alert className="rounded-2xl border-emerald-200 bg-emerald-50/95">
              <AlertDescription className="text-emerald-700">{model.successMessage}</AlertDescription>
            </Alert>
          ) : null}
          <Button
            type="button"
            disabled={model.isSubmitting || !canCreate}
            className="rounded-2xl bg-slate-950 text-white hover:bg-slate-800"
            onClick={() => {
              void model.submit().then((createdContest) => {
                if (createdContest) {
                  void navigate(`/contests?created=${encodeURIComponent(contestSlugValue(createdContest.slug))}`)
                }
              })
            }}
          >
            {model.isSubmitting ? t('contest.create.submitting') : t('contest.create.submit')}
          </Button>
        </CardContent>
      </Card>
    </PageShell>
  )
}
