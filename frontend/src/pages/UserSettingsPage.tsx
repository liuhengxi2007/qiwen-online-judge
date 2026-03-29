import { useEffect, useState } from 'react'
import { Link, Navigate, useNavigate, useParams } from 'react-router-dom'
import { ArrowLeft, LockKeyhole, LogOut, Settings, ShieldCheck } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Checkbox } from '@/components/ui/checkbox'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import {
  clearAuthSession,
  createDisplayName,
  createEmailAddress,
  createPlaintextPassword,
  displayNameValue,
  emailAddressValue,
  normalizeDisplayName,
  normalizeEmailAddress,
  normalizePlaintextPassword,
  plaintextPasswordValue,
  persistAuthSession,
  readAuthSession,
  toAuthSession,
  usernameValue,
  type ErrorResponse,
  type SessionResponse,
  type UpdateOwnSettingsRequest,
} from '@/domain/auth'

export function UserSettingsPage() {
  const navigate = useNavigate()
  const { username: routeUsername } = useParams<{ username: string }>()
  const [user, setUser] = useState(readAuthSession())
  const [displayName, setDisplayName] = useState(createDisplayName(''))
  const [email, setEmail] = useState(createEmailAddress(''))
  const [currentPassword, setCurrentPassword] = useState(createPlaintextPassword(''))
  const [newPassword, setNewPassword] = useState(createPlaintextPassword(''))
  const [confirmNewPassword, setConfirmNewPassword] = useState(createPlaintextPassword(''))
  const [errorMessage, setErrorMessage] = useState('')
  const [successMessage, setSuccessMessage] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  if (!user) {
    return <Navigate replace to="/login" />
  }

  useEffect(() => {
    let isCancelled = false

    const syncSession = async () => {
      try {
        const response = await fetch('/api/auth/session', {
          credentials: 'same-origin',
        })

        if (response.status === 401) {
          if (!isCancelled) {
            clearAuthSession()
            setUser(null)
            navigate('/login')
          }
          return
        }

        if (!response.ok) {
          throw new Error('Unable to refresh session.')
        }

        const session = (await response.json()) as SessionResponse

        if (!isCancelled) {
          persistAuthSession(session)
          setUser(session)
          setDisplayName(session.displayName)
          setEmail(session.email)
        }
      } catch {
        if (!isCancelled) {
          clearAuthSession()
          setUser(null)
          navigate('/login')
        }
      }
    }

    void syncSession()

    return () => {
      isCancelled = true
    }
  }, [navigate])

  const canonicalUsername = usernameValue(user.username)

  useEffect(() => {
    setDisplayName(user.displayName)
  }, [user.displayName])

  useEffect(() => {
    setEmail(user.email)
  }, [user.email])

  useEffect(() => {
    if (!routeUsername) {
      navigate(`/user/${canonicalUsername}/settings`, { replace: true })
      return
    }

    if (routeUsername.toLowerCase() !== canonicalUsername.toLowerCase()) {
      navigate(`/user/${canonicalUsername}/settings`, { replace: true })
    }
  }, [canonicalUsername, navigate, routeUsername])

  const handleSubmit = async () => {
    const normalizedDisplayName = normalizeDisplayName(displayName)
    const normalizedEmail = normalizeEmailAddress(email)
    const normalizedCurrentPassword = normalizePlaintextPassword(currentPassword)
    const normalizedNewPassword = normalizePlaintextPassword(newPassword)
    const normalizedConfirmNewPassword = normalizePlaintextPassword(confirmNewPassword)

    if (!displayNameValue(normalizedDisplayName) || !emailAddressValue(normalizedEmail)) {
      setErrorMessage('Display name and email are required.')
      setSuccessMessage('')
      return
    }

    if (!plaintextPasswordValue(normalizedCurrentPassword)) {
      setErrorMessage('Please enter your current password to confirm changes.')
      setSuccessMessage('')
      return
    }

    if (
      plaintextPasswordValue(normalizedNewPassword) ||
      plaintextPasswordValue(normalizedConfirmNewPassword)
    ) {
      if (plaintextPasswordValue(normalizedNewPassword) !== plaintextPasswordValue(normalizedConfirmNewPassword)) {
        setErrorMessage('New passwords do not match.')
        setSuccessMessage('')
        return
      }
    }

    setIsSubmitting(true)
    setErrorMessage('')
    setSuccessMessage('')

    try {
      const response = await fetch(`/api/auth/users/${encodeURIComponent(canonicalUsername)}/settings`, {
        method: 'POST',
        credentials: 'same-origin',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          displayName: normalizedDisplayName,
          email: normalizedEmail,
          currentPassword: normalizedCurrentPassword,
          newPassword: plaintextPasswordValue(normalizedNewPassword) ? normalizedNewPassword : null,
        } satisfies UpdateOwnSettingsRequest),
      })

      if (response.status === 401) {
        const errorData = (await response.json().catch(() => null)) as ErrorResponse | null
        if (errorData?.message === 'Authentication required.') {
          clearAuthSession()
          setUser(null)
          navigate('/login')
          return
        }

        setErrorMessage(errorData?.message ?? 'Current password is incorrect.')
        return
      }

      if (response.status === 403) {
        navigate(`/user/${canonicalUsername}/settings`, { replace: true })
        return
      }

      if (!response.ok) {
        const errorData = (await response.json().catch(() => null)) as ErrorResponse | null
        setErrorMessage(errorData?.message ?? 'Unable to update settings.')
        return
      }

      const updatedSession = (await response.json()) as SessionResponse
      persistAuthSession(toAuthSession(updatedSession))
      setUser(updatedSession)
      setCurrentPassword(createPlaintextPassword(''))
      setNewPassword(createPlaintextPassword(''))
      setConfirmNewPassword(createPlaintextPassword(''))
      setSuccessMessage('Settings updated successfully.')
    } catch {
      setErrorMessage('Unable to update settings.')
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <main className="min-h-screen bg-[linear-gradient(180deg,#f7fafc_0%,#edf2f7_100%)] px-6 py-12 sm:px-8">
      <section className="mx-auto max-w-4xl">
        <div className="mb-8 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="space-y-2">
            <p className="text-sm uppercase tracking-[0.25em] text-slate-500">Qiwen Online Judge</p>
            <h1 className="font-['Georgia'] text-4xl font-semibold tracking-tight text-slate-950">
              User Settings
            </h1>
            <p className="text-sm text-slate-600">
              Manage settings for {displayNameValue(user.displayName)} ({canonicalUsername}).
            </p>
          </div>

          <div className="flex flex-col gap-3 sm:flex-row">
            <Button asChild variant="outline" className="rounded-full border-slate-300 bg-white">
              <Link to="/">
                <ArrowLeft className="size-4" />
                Back to Dashboard
              </Link>
            </Button>
            <Button
              type="button"
              variant="outline"
              className="rounded-full border-slate-300 bg-white"
              onClick={() => {
                void fetch('/api/auth/logout', {
                  method: 'POST',
                  credentials: 'same-origin',
                }).finally(() => {
                  clearAuthSession()
                  setUser(null)
                  navigate('/login')
                })
              }}
            >
              <LogOut className="size-4" />
              Sign out
            </Button>
          </div>
        </div>

        <div className="grid gap-6 lg:grid-cols-[1.05fr_0.95fr]">
          <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
            <CardHeader>
              <div className="flex items-center gap-3">
                <div className="flex size-12 items-center justify-center rounded-2xl bg-sky-100 text-sky-700">
                  <Settings className="size-5" />
                </div>
                <div>
                  <CardTitle className="text-xl text-slate-950">Profile Settings</CardTitle>
                  <CardDescription>
                    This page is scoped to the signed-in account route.
                  </CardDescription>
                </div>
              </div>
            </CardHeader>
            <CardContent className="space-y-5">
              <div className="grid gap-4 sm:grid-cols-2">
                <div className="rounded-2xl bg-slate-50 p-5">
                  <p className="text-sm text-slate-500">Username</p>
                  <p className="mt-2 text-lg font-semibold text-slate-900">{canonicalUsername}</p>
                </div>
                <div className="rounded-2xl bg-slate-50 p-5">
                  <p className="text-sm text-slate-500">Current route</p>
                  <p className="mt-2 text-sm font-medium text-slate-900">/user/{canonicalUsername}/settings</p>
                </div>
              </div>

              <div className="space-y-2">
                <Label htmlFor="settings-display-name">Display name</Label>
                <Input
                  id="settings-display-name"
                  value={displayName}
                  onChange={(event) => setDisplayName(createDisplayName(event.target.value))}
                />
              </div>

              <div className="space-y-2">
                <Label htmlFor="settings-email">Email</Label>
                <Input
                  id="settings-email"
                  value={email}
                  onChange={(event) => setEmail(createEmailAddress(event.target.value))}
                />
              </div>

              <div className="grid gap-4 sm:grid-cols-2">
                <div className="space-y-2">
                  <Label htmlFor="settings-new-password">New password</Label>
                  <Input
                    id="settings-new-password"
                    type="password"
                    value={newPassword}
                    onChange={(event) => setNewPassword(createPlaintextPassword(event.target.value))}
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="settings-confirm-password">Confirm new password</Label>
                  <Input
                    id="settings-confirm-password"
                    type="password"
                    value={confirmNewPassword}
                    onChange={(event) => setConfirmNewPassword(createPlaintextPassword(event.target.value))}
                  />
                </div>
              </div>

              <div className="rounded-2xl border border-slate-200 bg-slate-50 p-5">
                <div className="mb-3 flex items-center gap-2 text-slate-800">
                  <LockKeyhole className="size-4" />
                  <p className="font-medium">Confirm with current password</p>
                </div>
                <div className="space-y-2">
                  <Label htmlFor="settings-current-password">Current password</Label>
                  <Input
                    id="settings-current-password"
                    type="password"
                    value={currentPassword}
                    onChange={(event) => setCurrentPassword(createPlaintextPassword(event.target.value))}
                  />
                </div>
              </div>

              {errorMessage ? <p className="text-sm text-rose-600">{errorMessage}</p> : null}
              {successMessage ? <p className="text-sm text-emerald-600">{successMessage}</p> : null}

              <Button
                type="button"
                disabled={isSubmitting}
                className="rounded-2xl bg-slate-950 text-white hover:bg-slate-800"
                onClick={() => {
                  void handleSubmit()
                }}
              >
                {isSubmitting ? 'Saving settings...' : 'Save settings'}
              </Button>
            </CardContent>
          </Card>

          <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
            <CardHeader>
              <div className="flex items-center gap-3">
                <div className="flex size-12 items-center justify-center rounded-2xl bg-emerald-100 text-emerald-700">
                  <ShieldCheck className="size-5" />
                </div>
                <div>
                  <CardTitle className="text-xl text-slate-950">Permissions</CardTitle>
                  <CardDescription>
                    Current permission flags attached to your signed-in account.
                  </CardDescription>
                </div>
              </div>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="flex items-center justify-between rounded-2xl bg-slate-50 px-5 py-4">
                <div>
                  <p className="font-medium text-slate-900">Site manager</p>
                  <p className="text-sm text-slate-500">Access to site-level management pages.</p>
                </div>
                <Checkbox checked={user.siteManager} disabled aria-label="Site manager permission" />
              </div>
              <div className="flex items-center justify-between rounded-2xl bg-slate-50 px-5 py-4">
                <div>
                  <p className="font-medium text-slate-900">Problem manager</p>
                  <p className="text-sm text-slate-500">Reserved for future problem administration tools.</p>
                </div>
                <Checkbox checked={user.problemManager} disabled aria-label="Problem manager permission" />
              </div>
            </CardContent>
          </Card>
        </div>
      </section>
    </main>
  )
}
