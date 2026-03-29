import { useEffect, useState } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { LayoutDashboard, LogOut, Settings, ShieldCheck, Users } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import {
  asSiteManagerSession,
  clearAuthSession,
  displayNameValue,
  persistAuthSession,
  readAuthSession,
  usernameValue,
  type SessionResponse,
} from '@/domain/auth'

export function DashboardPage() {
  const navigate = useNavigate()
  const [user, setUser] = useState(readAuthSession())
  const siteManagerUser = user ? asSiteManagerSession(user) : null

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

  return (
    <main className="min-h-screen bg-[linear-gradient(180deg,#f8fafc_0%,#eef2f7_100%)] px-6 py-12 sm:px-8">
      <section className="mx-auto max-w-4xl">
        <div className="mb-8 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="text-sm uppercase tracking-[0.25em] text-slate-500">Qiwen Online Judge</p>
            <h1 className="mt-2 font-['Georgia'] text-4xl font-semibold tracking-tight text-slate-950">
              Welcome back, {displayNameValue(user.displayName)}
            </h1>
          </div>
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

        <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
          <CardHeader>
            <div className="flex items-center gap-3">
              <div className="flex size-12 items-center justify-center rounded-2xl bg-emerald-100 text-emerald-700">
                <ShieldCheck className="size-5" />
              </div>
              <div>
                <CardTitle className="text-xl text-slate-950">Qiwen Online Judge Console</CardTitle>
                <CardDescription>
                  Your account is connected to the backend service and ready for management tasks.
                </CardDescription>
              </div>
            </div>
          </CardHeader>
          <CardContent className="grid gap-4 sm:grid-cols-2">
            <div className="rounded-2xl bg-slate-50 p-5">
              <p className="text-sm text-slate-500">Display name</p>
              <p className="mt-2 text-lg font-semibold text-slate-900">
                {displayNameValue(user.displayName)}
              </p>
            </div>
            <div className="rounded-2xl bg-slate-50 p-5">
              <p className="text-sm text-slate-500">Username</p>
              <p className="mt-2 text-lg font-semibold text-slate-900">{usernameValue(user.username)}</p>
            </div>
          </CardContent>
        </Card>

        <div className="mt-8 grid gap-5 md:grid-cols-2 xl:grid-cols-3">
          <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
            <CardHeader>
              <div className="flex items-center gap-3">
                <div className="flex size-12 items-center justify-center rounded-2xl bg-sky-100 text-sky-700">
                  <LayoutDashboard className="size-5" />
                </div>
                <div>
                  <CardTitle className="text-xl text-slate-950">Workspace</CardTitle>
                  <CardDescription>
                    Core judge console access for your signed-in account.
                  </CardDescription>
                </div>
              </div>
            </CardHeader>
            <CardContent>
              <p className="text-sm leading-7 text-slate-600">
                Your account is authenticated and ready to use the current console features.
              </p>
            </CardContent>
          </Card>

          <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
            <CardHeader>
              <div className="flex items-center gap-3">
                <div className="flex size-12 items-center justify-center rounded-2xl bg-violet-100 text-violet-700">
                  <Settings className="size-5" />
                </div>
                <div>
                  <CardTitle className="text-xl text-slate-950">User Settings</CardTitle>
                  <CardDescription>
                    Review your account route and permission summary.
                  </CardDescription>
                </div>
              </div>
            </CardHeader>
            <CardContent className="space-y-4">
              <p className="text-sm leading-7 text-slate-600">
                Open your dedicated settings page under the username-scoped route.
              </p>
              <Button asChild className="rounded-2xl bg-slate-950 text-white hover:bg-slate-800">
                <Link to={`/user/${usernameValue(user.username)}/settings`}>Open User Settings</Link>
              </Button>
            </CardContent>
          </Card>

          {siteManagerUser ? (
            <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
              <CardHeader>
                <div className="flex items-center gap-3">
                  <div className="flex size-12 items-center justify-center rounded-2xl bg-amber-100 text-amber-700">
                    <Users className="size-5" />
                  </div>
                  <div>
                    <CardTitle className="text-xl text-slate-950">Site Manage</CardTitle>
                    <CardDescription>
                      Review users and site-level permissions in a dedicated management page.
                    </CardDescription>
                  </div>
                </div>
              </CardHeader>
              <CardContent className="space-y-4">
                <p className="text-sm leading-7 text-slate-600">
                  User management has moved out of the dashboard into its own route.
                </p>
                <Button asChild className="rounded-2xl bg-slate-950 text-white hover:bg-slate-800">
                  <Link to="/site-manage">Open Site Management</Link>
                </Button>
              </CardContent>
            </Card>
          ) : null}
        </div>
      </section>
    </main>
  )
}
