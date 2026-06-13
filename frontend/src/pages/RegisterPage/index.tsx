import type { FormEvent } from 'react'
import { Link, Navigate } from 'react-router-dom'
import { AtSign, IdCard, LockKeyhole, UserRound } from 'lucide-react'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { AuthPageShell } from '@/pages/components/AuthPageShell'
import { AuthTextField } from '@/pages/components/AuthTextField'
import { usePageTitle } from '@/pages/hooks/usePageTitle'
import { useRegisterModel } from './hooks/useRegisterModel'
import { useI18n } from '@/system/i18n/use-i18n'

const inputClassName = 'h-12 rounded-2xl border-slate-200 bg-white pl-10 text-slate-900'

/**
 * 注册页，组合注册表单模型和注册请求，并在成功后写入会话。
 */
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

  if (navigationIntent) {
    return <Navigate replace={navigationIntent.replace} to={navigationIntent.to} />
  }

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    await submit()
  }

  return (
    <AuthPageShell
      heroTitle={t('auth.register.heroTitle')}
      heroDescription={t('auth.register.heroDescription')}
      mainClassName="bg-[radial-gradient(circle_at_top,rgba(14,165,233,0.12),transparent_28%),linear-gradient(180deg,#f6fafb_0%,#edf3f7_42%,#e7edf2_100%)]"
      overlayClassName="bg-[linear-gradient(160deg,rgba(255,255,255,0.7),transparent_42%,rgba(15,23,42,0.06)_100%)]"
      firstAccentClassName="-top-20 left-[8%] h-72 w-72 rounded-full bg-[radial-gradient(circle,rgba(249,115,22,0.18),transparent_65%)] blur-2xl"
      secondAccentClassName="bottom-0 right-[4%] h-80 w-80 rounded-full bg-[radial-gradient(circle,rgba(14,165,233,0.16),transparent_65%)] blur-3xl"
      gridClassName="lg:grid-cols-[1.02fr_0.98fr]"
      titleClassName="text-slate-950"
      descriptionClassName="text-slate-600"
    >
      <Card className="border-white/80 bg-white/82 py-0 shadow-[0_30px_80px_rgba(15,23,42,0.12)] backdrop-blur-xl">
        <CardHeader className="gap-3 border-b border-slate-200/80 px-7 py-7 sm:px-8">
          <CardTitle className="text-2xl text-slate-950">{t('auth.register.cardTitle')}</CardTitle>
          <CardDescription className="text-sm text-slate-500">{t('auth.register.cardDescription')}</CardDescription>
        </CardHeader>

        <CardContent className="px-7 py-7 sm:px-8">
          <form className="space-y-5" onSubmit={handleSubmit}>
            <AuthTextField
              id="register-username"
              label={t('auth.register.username')}
              value={username}
              icon={UserRound}
              autoComplete="username"
              labelClassName="text-slate-700"
              iconClassName="text-slate-400"
              inputClassName={inputClassName}
              onValueChange={(value) => setUsername(value.toLowerCase())}
            />

            <AuthTextField
              id="register-display-name"
              label={t('auth.register.displayName')}
              value={displayName}
              icon={IdCard}
              labelClassName="text-slate-700"
              iconClassName="text-slate-400"
              inputClassName={inputClassName}
              onValueChange={setDisplayName}
            />

            <AuthTextField
              id="register-email"
              label={t('auth.register.email')}
              value={email}
              icon={AtSign}
              autoComplete="email"
              labelClassName="text-slate-700"
              iconClassName="text-slate-400"
              inputClassName={inputClassName}
              onValueChange={setEmail}
            />

            <div className="grid gap-5 sm:grid-cols-2">
              <AuthTextField
                id="register-password"
                label={t('auth.register.password')}
                value={password}
                icon={LockKeyhole}
                type="password"
                autoComplete="new-password"
                labelClassName="text-slate-700"
                iconClassName="text-slate-400"
                inputClassName={inputClassName}
                onValueChange={setPassword}
              />

              <AuthTextField
                id="register-password-repeat"
                label={t('auth.register.confirmPassword')}
                value={confirmPassword}
                icon={LockKeyhole}
                type="password"
                autoComplete="new-password"
                labelClassName="text-slate-700"
                iconClassName="text-slate-400"
                inputClassName={inputClassName}
                onValueChange={setConfirmPassword}
              />
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
    </AuthPageShell>
  )
}
