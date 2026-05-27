import type { FormEvent } from 'react'
import { Link, Navigate, useSearchParams } from 'react-router-dom'
import { LockKeyhole, UserRound } from 'lucide-react'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { useI18n } from '@/system/i18n/use-i18n'
import { useLoginModel } from './hooks/use-login-model'
import { usePageTitle } from '@/pages/hooks/use-page-title'

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

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    await submit()
  }

  if (navigationIntent) {
    return <Navigate replace={navigationIntent.replace} to={navigationIntent.to} />
  }

  return (
    <main className="relative min-h-screen overflow-hidden bg-[radial-gradient(circle_at_top,rgba(237,127,16,0.16),transparent_28%),linear-gradient(180deg,#fff8f1_0%,#f3ede2_45%,#ebe4d8_100%)]">
      <div className="absolute inset-0 bg-[linear-gradient(140deg,rgba(255,255,255,0.74),transparent_40%,rgba(50,32,18,0.06)_100%)]" />
      <div className="absolute -top-24 right-[-10%] h-72 w-72 rounded-full bg-[radial-gradient(circle,rgba(147,197,253,0.32),transparent_65%)] blur-2xl" />
      <div className="absolute bottom-0 left-[-5%] h-80 w-80 rounded-full bg-[radial-gradient(circle,rgba(251,191,36,0.18),transparent_65%)] blur-3xl" />

      <section className="relative mx-auto flex min-h-screen max-w-6xl items-center px-6 py-14 sm:px-8 lg:px-12">
        <div className="grid w-full items-center gap-10 lg:grid-cols-[1.05fr_0.95fr]">
          <div className="space-y-6">
            <div className="max-w-xl space-y-4">
              <h1 className="font-['Georgia'] text-4xl leading-tight font-semibold tracking-tight text-stone-900 sm:text-5xl">
                {t('auth.login.heroTitle')}
              </h1>
              <p className="text-base leading-8 text-stone-600 sm:text-lg">
                {t('auth.login.heroDescription')}
              </p>
            </div>
          </div>

          <Card className="border-white/80 bg-white/82 py-0 shadow-[0_30px_80px_rgba(120,53,15,0.12)] backdrop-blur-xl">
            <CardHeader className="gap-3 border-b border-stone-200/80 px-7 py-7 sm:px-8">
              <CardTitle className="text-2xl text-stone-950">{t('auth.login.cardTitle')}</CardTitle>
              <CardDescription className="text-sm text-stone-500">
                {t('auth.login.cardDescription')}
              </CardDescription>
            </CardHeader>

            <CardContent className="px-7 py-7 sm:px-8">
              <form className="space-y-5" onSubmit={handleSubmit}>
                <div className="space-y-2">
                  <Label htmlFor="username" className="text-stone-700">
                    {t('auth.login.username')}
                  </Label>
                  <div className="relative">
                    <UserRound className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-stone-400" />
                    <Input
                      id="username"
                      type="text"
                      autoComplete="username"
                      value={username}
                      className="h-12 rounded-2xl border-stone-200 bg-white pl-10 text-stone-900 focus-visible:ring-orange-300"
                      onChange={(event) => setUsername(event.target.value.toLowerCase())}
                    />
                  </div>
                </div>

                <div className="space-y-2">
                  <Label htmlFor="password" className="text-stone-700">
                    {t('auth.login.password')}
                  </Label>
                  <div className="relative">
                    <LockKeyhole className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-stone-400" />
                    <Input
                      id="password"
                      type="password"
                      autoComplete="current-password"
                      value={password}
                      className="h-12 rounded-2xl border-stone-200 bg-white pl-10 text-stone-900 focus-visible:ring-orange-300"
                      onChange={(event) => setPassword(event.target.value)}
                    />
                  </div>
                </div>

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
        </div>
      </section>
    </main>
  )
}
