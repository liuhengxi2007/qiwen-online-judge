import { Navigate, useNavigate } from 'react-router-dom'
import { useEffect, useState } from 'react'
import { LogOut, ShieldCheck } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'

type AuthUser = {
  displayName: string
  username: string
}

type AuthUserListItem = {
  username: string
  displayName: string
  email: string
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
  const [users, setUsers] = useState<AuthUserListItem[]>([])
  const [userListError, setUserListError] = useState('')
  const [isLoadingUsers, setIsLoadingUsers] = useState(false)

  if (!user) {
    return <Navigate replace to="/login" />
  }

  useEffect(() => {
    if (user.username.toLowerCase() !== 'admin') {
      return
    }

    let isCancelled = false

    const loadUsers = async () => {
      setIsLoadingUsers(true)
      setUserListError('')

      try {
        const response = await fetch('/api/auth/users')

        if (!response.ok) {
          throw new Error('Unable to load users.')
        }

        const data = (await response.json()) as AuthUserListItem[]

        if (!isCancelled) {
          setUsers(data)
        }
      } catch {
        if (!isCancelled) {
          setUserListError('Unable to load the user list.')
        }
      } finally {
        if (!isCancelled) {
          setIsLoadingUsers(false)
        }
      }
    }

    void loadUsers()

    return () => {
      isCancelled = true
    }
  }, [user.username])

  return (
    <main className="min-h-screen bg-[linear-gradient(180deg,#f8fafc_0%,#eef2f7_100%)] px-6 py-12 sm:px-8">
      <section className="mx-auto max-w-4xl">
        <div className="mb-8 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="text-sm uppercase tracking-[0.25em] text-slate-500">Qiwen Online Judge</p>
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
              <p className="mt-2 text-lg font-semibold text-slate-900">{user.displayName}</p>
            </div>
            <div className="rounded-2xl bg-slate-50 p-5">
              <p className="text-sm text-slate-500">Username</p>
              <p className="mt-2 text-lg font-semibold text-slate-900">{user.username}</p>
            </div>
          </CardContent>
        </Card>

        {user.username.toLowerCase() === 'admin' ? (
          <Card className="mt-8 border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
            <CardHeader>
              <CardTitle className="text-xl text-slate-950">User Management</CardTitle>
              <CardDescription>
                The admin account can review all registered users from the backend database.
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              {userListError ? <p className="text-sm text-rose-600">{userListError}</p> : null}
              {isLoadingUsers ? (
                <p className="text-sm text-slate-500">Loading users...</p>
              ) : (
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>Username</TableHead>
                      <TableHead>Display name</TableHead>
                      <TableHead>Email</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {users.map((listedUser) => (
                      <TableRow key={listedUser.username}>
                        <TableCell className="font-medium text-slate-900">{listedUser.username}</TableCell>
                        <TableCell>{listedUser.displayName}</TableCell>
                        <TableCell>{listedUser.email}</TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              )}
            </CardContent>
          </Card>
        ) : null}
      </section>
    </main>
  )
}
