import { Navigate, useNavigate } from 'react-router-dom'
import { Users } from 'lucide-react'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { useSessionGuard } from '@/pages/hooks/useSessionGuard'
import { userGroupSlugValue } from '@/objects/usergroup/UserGroupSlug'
import { useCreateUserGroupPageModel } from './hooks/useCreateUserGroupPageModel'
import { PageShell } from '@/pages/components/PageShell'
import { useBeforeUnloadPrompt } from '@/pages/hooks/useBeforeUnloadPrompt'
import { usePageTitle } from '@/pages/hooks/usePageTitle'
import { useI18n } from '@/system/i18n/use-i18n'

export function CreateUserGroupPage() {
  const { t } = useI18n()
  usePageTitle(t('userGroup.create.pageTitle'))
  const { session: user, navigationIntent } = useSessionGuard()
  const navigate = useNavigate()
  const model = useCreateUserGroupPageModel()
  const hasUnsavedChanges =
    model.slug.trim().length > 0 || model.name.trim().length > 0 || model.description.trim().length > 0

  useBeforeUnloadPrompt(hasUnsavedChanges)

  if (navigationIntent) {
    return <Navigate replace={navigationIntent.replace} to={navigationIntent.to} />
  }

  if (!user) {
    return <Navigate replace to="/login" />
  }

  return (
    <PageShell
      title={t('userGroup.create.heading')}
      mainClassName="bg-[linear-gradient(180deg,#f8fafc_0%,#edf4fb_100%)]"
    >
      <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
        <CardHeader>
          <div className="flex items-center gap-3">
            <div className="flex size-12 items-center justify-center rounded-2xl bg-sky-100 text-sky-700">
              <Users className="size-5" />
            </div>
            <div>
              <CardTitle className="text-xl text-slate-950">{t('userGroup.create.cardTitle')}</CardTitle>
              <CardDescription>{t('userGroup.create.cardDescription')}</CardDescription>
            </div>
          </div>
        </CardHeader>
        <CardContent className="space-y-5">
          <div className="space-y-2">
            <Label htmlFor="user-group-slug">{t('userGroup.create.slug')}</Label>
            <Input
              id="user-group-slug"
              value={model.slug}
              onChange={(event) => model.setSlug(event.target.value.toLowerCase())}
            />
          </div>

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
            disabled={model.isSubmitting}
            className="rounded-2xl bg-emerald-300 text-emerald-950 hover:bg-emerald-400"
            onClick={() => {
              void model.submit().then((createdGroup) => {
                if (createdGroup) {
                  void navigate(`/user-groups/${userGroupSlugValue(createdGroup.slug)}`)
                }
              })
            }}
          >
            {model.isSubmitting ? t('userGroup.create.submitting') : t('userGroup.create.submit')}
          </Button>
        </CardContent>
      </Card>
    </PageShell>
  )
}
