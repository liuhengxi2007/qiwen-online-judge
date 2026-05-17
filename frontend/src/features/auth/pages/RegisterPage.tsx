import type { FormEvent } from 'react'
import { Link, Navigate } from 'react-router-dom'
import { AtSign, IdCard, LockKeyhole, UserRound } from 'lucide-react'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { usePageTitle } from '@/shared/hooks/use-page-title'
import { useRegisterModel } from '@/features/auth/hooks/use-register-model'
import { useI18n } from '@/shared/i18n/use-i18n'

export function RegisterPage() {
  const { t } = useI18n()
  usePageTitle(t('auth.register.pageTitle'))
  const {
    username,
    displayName,
    email,
    password,
    confirmPassword,
    errorMessage,
    isSubmitting,
    navigationIntent,
    setUsername,
    setDisplayName,
    setEmail,
    setPassword,
    setConfirmPassword,
    submit,
  } = useRegisterModel()

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    await submit()
  }

  if (navigationIntent) {
    return <Navigate replace={navigationIntent.replace} to={navigationIntent.to} />
  }

  return (
    <main className="relative min-h-screen overflow-hidden bg-[radial-gradient(circle_at_top,rgba(14,165,233,0.12),transparent_28%),linear-gradient(180deg,#f6fafb_0%,#edf3f7_42%,#e7edf2_100%)]">
      <div className="absolute inset-0 bg-[linear-gradient(160deg,rgba(255,255,255,0.7),transparent_42%,rgba(15,23,42,0.06)_100%)]" />
      <div className="absolute -top-20 left-[8%] h-72 w-72 rounded-full bg-[radial-gradient(circle,rgba(249,115,22,0.18),transparent_65%)] blur-2xl" />
      <div className="absolute bottom-0 right-[4%] h-80 w-80 rounded-full bg-[radial-gradient(circle,rgba(14,165,233,0.16),transparent_65%)] blur-3xl" />

      <section className="relative mx-auto flex min-h-screen max-w-6xl items-center px-6 py-14 sm:px-8 lg:px-12">
        <div className="grid w-full items-center gap-10 lg:grid-cols-[1.02fr_0.98fr]">
          <div className="space-y-6">
            <div className="max-w-xl space-y-4">
              <h1 className="font-['Georgia'] text-4xl leading-tight font-semibold tracking-tight text-slate-950 sm:text-5xl">
                {t('auth.register.heroTitle')}
              </h1>
              <p className="text-base leading-8 text-slate-600 sm:text-lg">
                {t('auth.register.heroDescription')}
              </p>
            </div>
          </div>

          <Card className="border-white/80 bg-white/82 py-0 shadow-[0_30px_80px_rgba(15,23,42,0.12)] backdrop-blur-xl">
            <CardHeader className="gap-3 border-b border-slate-200/80 px-7 py-7 sm:px-8">
              <CardTitle className="text-2xl text-slate-950">{t('auth.register.cardTitle')}</CardTitle>
              <CardDescription className="text-sm text-slate-500">
                {t('auth.register.cardDescription')}
              </CardDescription>
            </CardHeader>

            <CardContent className="px-7 py-7 sm:px-8">
              <form className="space-y-5" onSubmit={handleSubmit}>
                <div className="space-y-2">
                  <Label htmlFor="register-username" className="text-slate-700">
                    {t('auth.register.username')}
                  </Label>
                  <div className="relative">
                    <UserRound className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-slate-400" />
                    <Input
                      id="register-username"
                      type="text"
                      autoComplete="username"
                      value={username}
                      className="h-12 rounded-2xl border-slate-200 bg-white pl-10 text-slate-900"
                      onChange={(event) => setUsername(event.target.value.toLowerCase())}
                    />
                  </div>
                </div>

                <div className="space-y-2">
                  <Label htmlFor="register-display-name" className="text-slate-700">
                    {t('auth.register.displayName')}
                  </Label>
                  <div className="relative">
                    <IdCard className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-slate-400" />
                    <Input
                      id="register-display-name"
                      type="text"
                      value={displayName}
                      className="h-12 rounded-2xl border-slate-200 bg-white pl-10 text-slate-900"
                      onChange={(event) => setDisplayName(event.target.value)}
                    />
                  </div>
                </div>

                <div className="space-y-2">
                  <Label htmlFor="register-email" className="text-slate-700">
                    {t('auth.register.email')}
                  </Label>
                  <div className="relative">
                    <AtSign className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-slate-400" />
                    <Input
                      id="register-email"
                      type="text"
                      autoComplete="email"
                      value={email}
                      className="h-12 rounded-2xl border-slate-200 bg-white pl-10 text-slate-900"
                      onChange={(event) => setEmail(event.target.value)}
                    />
                  </div>
                </div>

                <div className="grid gap-5 sm:grid-cols-2">
                  <div className="space-y-2">
                    <Label htmlFor="register-password" className="text-slate-700">
                      {t('auth.register.password')}
                    </Label>
                    <div className="relative">
                      <LockKeyhole className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-slate-400" />
                      <Input
                        id="register-password"
                        type="password"
                      autoComplete="new-password"
                      value={password}
                        className="h-12 rounded-2xl border-slate-200 bg-white pl-10 text-slate-900"
                      onChange={(event) => setPassword(event.target.value)}
                      />
                    </div>
                  </div>

                  <div className="space-y-2">
                    <Label htmlFor="register-password-repeat" className="text-slate-700">
                      {t('auth.register.confirmPassword')}
                    </Label>
                    <div className="relative">
                      <LockKeyhole className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-slate-400" />
                      <Input
                        id="register-password-repeat"
                        type="password"
                      autoComplete="new-password"
                      value={confirmPassword}
                        className="h-12 rounded-2xl border-slate-200 bg-white pl-10 text-slate-900"
                      onChange={(event) => setConfirmPassword(event.target.value)}
                      />
                    </div>
                  </div>
                </div>

                <div className="min-h-14">
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
                  className="h-12 w-full rounded-2xl bg-slate-950 text-base text-white hover:bg-slate-800"
                >
                  {isSubmitting ? t('auth.register.submitting') : t('auth.register.submit')}
                </Button>

                <p className="text-center text-sm text-slate-500">
                  {t('auth.register.loginPrompt')}{' '}
                  <Link to="/login" className="font-medium text-slate-900 underline underline-offset-4">
                    {t('auth.register.loginLink')}
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
