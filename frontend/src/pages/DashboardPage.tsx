import { Navigate, useNavigate } from 'react-router-dom'
import { LogOut, ShieldCheck } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'

type AuthUser = {
  displayName: string
  username: string
}

const readAuthUser = (): AuthUser | null => {
  const raw = window.localStorage.getItem('auth_user')
  if (!raw) {
    return null
  }

  try {
    return JSON.parse(raw) as AuthUser
  } catch {
    window.localStorage.removeItem('auth_user')
    return null
  }
}

export function DashboardPage() {
  const navigate = useNavigate()
  const user = readAuthUser()

  if (!user) {
    return <Navigate replace to="/login" />
  }

  return (
    <main className="min-h-screen bg-[linear-gradient(180deg,#f8fafc_0%,#eef2f7_100%)] px-6 py-12 sm:px-8">
      <section className="mx-auto max-w-4xl">
        <div className="mb-8 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="text-sm uppercase tracking-[0.25em] text-slate-500">Login Success</p>
            <h1 className="mt-2 font-['Georgia'] text-4xl font-semibold tracking-tight text-slate-950">
              Welcome back, {user.displayName}
            </h1>
          </div>
          <Button
            type="button"
            variant="outline"
            className="rounded-full border-slate-300 bg-white"
            onClick={() => {
              window.localStorage.removeItem('auth_user')
              navigate('/login')
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
                <CardTitle className="text-xl text-slate-950">You are signed in</CardTitle>
                <CardDescription>This website now focuses on login and logout only.</CardDescription>
              </div>
            </div>
          </CardHeader>
          <CardContent className="grid gap-4 sm:grid-cols-2">
            <div className="rounded-2xl bg-slate-50 p-5">
              <p className="text-sm text-slate-500">Display name</p>
              <p className="mt-2 text-lg font-semibold text-slate-900">{user.displayName}</p>
            </div>
            <div className="rounded-2xl bg-slate-50 p-5">
              <p className="text-sm text-slate-500">Username</p>
              <p className="mt-2 text-lg font-semibold text-slate-900">{user.username}</p>
            </div>
          </CardContent>
        </Card>
      </section>
    </main>
  )
}
