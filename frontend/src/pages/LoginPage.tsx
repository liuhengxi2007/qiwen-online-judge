import { useState, type FormEvent } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { LockKeyhole, UserRound } from 'lucide-react'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import {
  createPlaintextPassword,
  createUsername,
  normalizePlaintextPassword,
  normalizeUsername,
  persistAuthSession,
  plaintextPasswordValue,
  toAuthSession,
  usernameValue,
  type ErrorResponse,
  type LoginRequest,
  type LoginResponse,
} from '@/domain/auth'

export function LoginPage() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const [username, setUsername] = useState(createUsername('admin'))
  const [password, setPassword] = useState(createPlaintextPassword('password123'))
  const [errorMessage, setErrorMessage] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  const notice = searchParams.get('notice')
  const noticeMessage =
    notice === 'session-expired'
      ? 'Your session expired. Sign in again to continue.'
      : notice === 'signed-out'
        ? 'You have been signed out.'
        : null

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()

    const normalizedUsername = normalizeUsername(username)
    const normalizedPassword = normalizePlaintextPassword(password)

    if (!usernameValue(normalizedUsername) || !plaintextPasswordValue(normalizedPassword)) {
      setErrorMessage('Please enter both username and password.')
      return
    }

    setIsSubmitting(true)
    setErrorMessage('')

    try {
      const response = await fetch('/api/auth/login', {
        method: 'POST',
        credentials: 'same-origin',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          username: normalizedUsername,
          password: normalizedPassword,
        } satisfies LoginRequest),
      })

      if (!response.ok) {
        const errorData = (await response.json().catch(() => null)) as ErrorResponse | null
        setErrorMessage(errorData?.message ?? 'Login failed. Please try again.')
        return
      }

      const data = (await response.json()) as LoginResponse
      persistAuthSession(toAuthSession(data))
      navigate('/')
    } catch {
      setErrorMessage('Unable to reach the server. Please start the backend service.')
    } finally {
      setIsSubmitting(false)
    }
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
                Qiwen Online Judge
              </h1>
              <p className="text-base leading-8 text-stone-600 sm:text-lg">
                Sign in to manage the judge platform, review submissions, and access the
                administrator workspace.
              </p>
            </div>
            <div className="grid gap-4 sm:grid-cols-2">
              <div className="rounded-3xl border border-white/75 bg-white/75 p-5 shadow-[0_20px_45px_rgba(120,53,15,0.08)] backdrop-blur">
                <p className="text-sm font-medium text-stone-500">Demo account</p>
                <p className="mt-2 text-lg font-semibold text-stone-900">admin</p>
              </div>
              <div className="rounded-3xl border border-white/75 bg-stone-950 p-5 text-stone-50 shadow-[0_20px_45px_rgba(28,25,23,0.24)]">
                <p className="text-sm font-medium text-stone-300">Demo password</p>
                <p className="mt-2 text-lg font-semibold">password123</p>
              </div>
            </div>
          </div>

          <Card className="border-white/80 bg-white/82 py-0 shadow-[0_30px_80px_rgba(120,53,15,0.12)] backdrop-blur-xl">
            <CardHeader className="gap-3 border-b border-stone-200/80 px-7 py-7 sm:px-8">
              <CardTitle className="text-2xl text-stone-950">Sign in to Qiwen Online Judge</CardTitle>
              <CardDescription className="text-sm text-stone-500">
                Submit your administrator credentials to the live backend service.
              </CardDescription>
            </CardHeader>

            <CardContent className="px-7 py-7 sm:px-8">
              <form className="space-y-5" onSubmit={handleSubmit}>
                <div className="space-y-2">
                  <Label htmlFor="username" className="text-stone-700">
                    Username
                  </Label>
                  <div className="relative">
                    <UserRound className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-stone-400" />
                    <Input
                      id="username"
                      type="text"
                      autoComplete="username"
                      value={username}
                      className="h-12 rounded-2xl border-stone-200 bg-white pl-10 text-stone-900 placeholder:text-stone-400 focus-visible:ring-orange-300"
                      placeholder="Enter your username"
                      onChange={(event) => setUsername(createUsername(event.target.value))}
                    />
                  </div>
                </div>

                <div className="space-y-2">
                  <Label htmlFor="password" className="text-stone-700">
                    Password
                  </Label>
                  <div className="relative">
                    <LockKeyhole className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-stone-400" />
                    <Input
                      id="password"
                      type="password"
                      autoComplete="current-password"
                      value={password}
                      className="h-12 rounded-2xl border-stone-200 bg-white pl-10 text-stone-900 placeholder:text-stone-400 focus-visible:ring-orange-300"
                      placeholder="Enter your password"
                      onChange={(event) => setPassword(createPlaintextPassword(event.target.value))}
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
                  {isSubmitting ? 'Signing in...' : 'Sign in'}
                </Button>

                <p className="text-center text-sm text-stone-500">
                  Need an account?{' '}
                  <Link to="/register" className="font-medium text-stone-900 underline underline-offset-4">
                    Register
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
