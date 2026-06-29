import { Navigate } from 'react-router-dom'
import { Gauge, Plus, RotateCcw } from 'lucide-react'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { PageShell } from '@/pages/components/PageShell'
import { usePageTitle } from '@/pages/hooks/usePageTitle'
import { useSessionGuard } from '@/pages/hooks/useSessionGuard'
import { useI18n } from '@/system/i18n/use-i18n'

import { RatingContestSequence } from './components/RatingContestSequence'
import { useRatingManagePageModel } from './hooks/useRatingManagePageModel'

/**
 * Rating 管理页面，要求站点管理员权限并组合追加、回退和比赛序列展示。
 */
export function RatingManagePage() {
  const { t } = useI18n()
  usePageTitle(t('ratingManage.pageTitle'))
  const { session: user, siteManagerSession, navigationIntent } = useSessionGuard({ requireSiteManager: true })
  const model = useRatingManagePageModel(Boolean(siteManagerSession))
  const contests = model.manageState?.contests ?? []

  if (navigationIntent) {
    return <Navigate replace={navigationIntent.replace} to={navigationIntent.to} />
  }

  if (!user) {
    return <Navigate replace to="/login" />
  }

  return (
    <PageShell
      title={t('ratingManage.heading')}
      description={t('ratingManage.description')}
      mainClassName="bg-[linear-gradient(180deg,#fffaf4_0%,#eef2f7_100%)]"
    >
      {model.errorMessage ? (
        <Alert variant="destructive" className="mb-6 rounded-2xl border-rose-200 bg-rose-50/95">
          <AlertDescription className="text-rose-700">{model.errorMessage}</AlertDescription>
        </Alert>
      ) : null}

      {model.noticeMessage ? (
        <Alert className="mb-6 rounded-2xl border-emerald-200 bg-emerald-50/95">
          <AlertDescription className="text-emerald-800">{model.noticeMessage}</AlertDescription>
        </Alert>
      ) : null}

      <div className="grid gap-6 lg:grid-cols-[22rem_minmax(0,1fr)]">
        <Card className="border-slate-200 bg-white/95 shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
          <CardHeader>
            <div className="flex items-center gap-3">
              <div className="flex size-12 items-center justify-center rounded-2xl bg-amber-100 text-amber-700">
                <Gauge className="size-5" />
              </div>
              <div>
                <CardTitle className="text-xl text-slate-950">{t('ratingManage.controlsTitle')}</CardTitle>
                <CardDescription>{t('ratingManage.controlsDescription')}</CardDescription>
              </div>
            </div>
          </CardHeader>
          <CardContent className="space-y-5">
            <form
              className="space-y-4"
              onSubmit={(event) => {
                event.preventDefault()
                void model.appendContest()
              }}
            >
              <div className="space-y-2">
                <Label htmlFor="rating-contest-slug">{t('ratingManage.contestSlugLabel')}</Label>
                <Input
                  id="rating-contest-slug"
                  value={model.draft.contestSlugInput}
                  onChange={(event) => model.setContestSlugInput(event.target.value)}
                  placeholder={t('ratingManage.contestSlugPlaceholder')}
                />
              </div>

              <div className="space-y-2">
                <Label htmlFor="rating-m">{t('ratingManage.mLabel')}</Label>
                <Input
                  id="rating-m"
                  inputMode="numeric"
                  min={2}
                  max={100}
                  type="number"
                  value={model.draft.mInput}
                  onChange={(event) => model.setMInput(event.target.value)}
                />
              </div>

              <Button
                className="w-full rounded-2xl bg-amber-300 text-amber-950 hover:bg-amber-400"
                disabled={model.isAppending || model.isLoading}
                type="submit"
              >
                <Plus className="size-4" />
                {model.isAppending ? t('ratingManage.appending') : t('ratingManage.append')}
              </Button>
            </form>

            <div className="rounded-3xl border border-slate-200 bg-slate-50 p-4">
              <p className="text-sm font-medium text-slate-900">{t('ratingManage.popTitle')}</p>
              <p className="mt-1 text-sm text-slate-500">{t('ratingManage.popDescription')}</p>
              <Button
                className="mt-4 w-full rounded-2xl"
                disabled={model.isPopping || model.isLoading || contests.length === 0}
                type="button"
                variant="destructive"
                onClick={() => {
                  void model.popContest()
                }}
              >
                <RotateCcw className="size-4" />
                {model.isPopping ? t('ratingManage.popping') : t('ratingManage.pop')}
              </Button>
            </div>
          </CardContent>
        </Card>

        <Card className="border-slate-200 bg-white/95 shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
          <CardHeader>
            <CardTitle className="text-xl text-slate-950">{t('ratingManage.sequenceTitle')}</CardTitle>
            <CardDescription>{t('ratingManage.sequenceDescription')}</CardDescription>
          </CardHeader>
          <CardContent>
            <RatingContestSequence contests={contests} isLoading={model.isLoading} />
          </CardContent>
        </Card>
      </div>
    </PageShell>
  )
}
