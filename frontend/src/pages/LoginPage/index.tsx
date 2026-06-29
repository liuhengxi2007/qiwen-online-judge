import type { FormEvent } from 'react'
import { Link, Navigate, useSearchParams } from 'react-router-dom'
import { LockKeyhole, UserRound } from 'lucide-react'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { AuthPageShell } from '@/pages/components/AuthPageShell'
import { AuthTextField } from '@/pages/components/AuthTextField'
import { useI18n } from '@/system/i18n/use-i18n'
import { useLoginModel } from './hooks/useLoginModel'
import { usePageTitle } from '@/pages/hooks/usePageTitle'

/**
 * 登录页，组合登录表单模型和登录请求，并根据 notice 参数展示提示。
 */
export function LoginPage() {
  const { t } = useI18n()
  usePageTitle(t('auth.login.pageTitle'))
  const [searchParams] = useSearchParams()
  const { username, password, errorMessage, isSubmitting, navigationIntent, setUsername, setPassword, submit } =
    useLoginModel()

  const notice = searchParams.get('notice')
  const noticeMessage =
    notice === 'session-expired'
      ? t('auth.login.notice.sessionExpired')
      : notice === 'signed-out'
        ? t('auth.login.notice.signedOut')
        : notice === 'password-changed'
          ? t('auth.login.notice.passwordChanged')
          : null

  if (navigationIntent) {
    return <Navigate replace={navigationIntent.replace} to={navigationIntent.to} />
  }

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    await submit()
  }

  return (
    <AuthPageShell
      heroTitle={t('auth.login.heroTitle')}
      heroDescription={t('auth.login.heroDescription')}
      mainClassName="bg-[radial-gradient(circle_at_top,rgba(237,127,16,0.16),transparent_28%),linear-gradient(180deg,#fff8f1_0%,#f3ede2_45%,#ebe4d8_100%)]"
      overlayClassName="bg-[linear-gradient(140deg,rgba(255,255,255,0.74),transparent_40%,rgba(50,32,18,0.06)_100%)]"
      firstAccentClassName="-top-24 right-[-10%] h-72 w-72 rounded-full bg-[radial-gradient(circle,rgba(147,197,253,0.32),transparent_65%)] blur-2xl"
      secondAccentClassName="bottom-0 left-[-5%] h-80 w-80 rounded-full bg-[radial-gradient(circle,rgba(251,191,36,0.18),transparent_65%)] blur-3xl"
      gridClassName="lg:grid-cols-[1.05fr_0.95fr]"
      titleClassName="text-stone-900"
      descriptionClassName="text-stone-600"
    >
      <Card className="border-white/80 bg-white/82 py-0 shadow-[0_30px_80px_rgba(120,53,15,0.12)] backdrop-blur-xl">
        <CardHeader className="gap-3 border-b border-stone-200/80 px-7 py-7 sm:px-8">
          <CardTitle className="text-2xl text-stone-950">{t('auth.login.cardTitle')}</CardTitle>
          <CardDescription className="text-sm text-stone-500">{t('auth.login.cardDescription')}</CardDescription>
        </CardHeader>

        <CardContent className="px-7 py-7 sm:px-8">
          <form className="space-y-5" onSubmit={handleSubmit}>
            <AuthTextField
              id="username"
              label={t('auth.login.username')}
              value={username}
              icon={UserRound}
              autoComplete="username"
              labelClassName="text-stone-700"
              iconClassName="text-stone-400"
              inputClassName="h-12 rounded-2xl border-stone-200 bg-white pl-10 text-stone-900 focus-visible:ring-orange-300"
              onValueChange={(value) => setUsername(value.toLowerCase())}
            />

            <AuthTextField
              id="password"
              label={t('auth.login.password')}
              value={password}
              icon={LockKeyhole}
              type="password"
              autoComplete="current-password"
              labelClassName="text-stone-700"
              iconClassName="text-stone-400"
              inputClassName="h-12 rounded-2xl border-stone-200 bg-white pl-10 text-stone-900 focus-visible:ring-orange-300"
              onValueChange={setPassword}
            />

            <div className="min-h-14">
              {noticeMessage ? (
                <Alert className="mb-3 rounded-2xl border-sky-200 bg-sky-50/95">
                  <AlertDescription className="text-sky-700">{noticeMessage}</AlertDescription>
                </Alert>
              ) : null}
              {errorMessage ? (
                <Alert variant="destructive" className="rounded-2xl border-rose-200 bg-rose-50/95">
                  <AlertDescription className="text-rose-700">{errorMessage}</AlertDescription>
                </Alert>
              ) : null}
            </div>

            <Button
              type="submit"
              size="lg"
              disabled={isSubmitting}
              className="h-12 w-full rounded-2xl bg-stone-950 text-base text-white hover:bg-stone-800"
            >
              {isSubmitting ? t('auth.login.submitting') : t('auth.login.submit')}
            </Button>

            <p className="text-center text-sm text-stone-500">
              {t('auth.login.registerPrompt')}{' '}
              <Link to="/register" className="font-medium text-stone-900 underline underline-offset-4">
                {t('auth.login.registerLink')}
              </Link>
            </p>
          </form>
        </CardContent>
      </Card>
    </AuthPageShell>
  )
}
