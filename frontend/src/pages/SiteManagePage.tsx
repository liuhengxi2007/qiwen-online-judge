import { useEffect, useState } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { ArrowLeft, LogOut, Settings2 } from 'lucide-react'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Checkbox } from '@/components/ui/checkbox'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import {
  asSiteManagerSession,
  clearAuthSession,
  displayNameValue,
  emailAddressValue,
  persistAuthSession,
  readAuthSession,
  usernameValue,
  type AuthUserListItem,
  type SessionResponse,
  type SiteManagerSession,
  type UpdateUserPermissionsRequest,
} from '@/domain/auth'

export function SiteManagePage() {
  const navigate = useNavigate()
  const [user, setUser] = useState(readAuthSession())
  const [users, setUsers] = useState<AuthUserListItem[]>([])
  const [userListError, setUserListError] = useState('')
  const [statusMessage, setStatusMessage] = useState('')
  const [isLoadingUsers, setIsLoadingUsers] = useState(false)
  const [updatingUsername, setUpdatingUsername] = useState<string | null>(null)
  const siteManagerUser = user ? asSiteManagerSession(user) : null

  if (!user) {
    return <Navigate replace to="/login" />
  }

  const isProtectedAdmin = (listedUser: AuthUserListItem) => usernameValue(listedUser.username).toLowerCase() === 'admin'

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
            navigate('/login?notice=session-expired')
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

          if (!asSiteManagerSession(session)) {
            navigate('/?notice=site-manage-denied', { replace: true })
          }
        }
      } catch {
        if (!isCancelled) {
          clearAuthSession()
          setUser(null)
          navigate('/login?notice=session-expired')
        }
      }
    }

    void syncSession()

    return () => {
      isCancelled = true
    }
  }, [navigate])

  useEffect(() => {
    if (!siteManagerUser) {
      navigate('/')
      return
    }

    let isCancelled = false

    const loadUsers = async () => {
      setIsLoadingUsers(true)
      setUserListError('')
      setStatusMessage('')

      try {
        const response = await fetch('/api/auth/users', {
          credentials: 'same-origin',
        })

        if (response.status === 401) {
          if (!isCancelled) {
            clearAuthSession()
            setUser(null)
            navigate('/login?notice=session-expired')
          }
          return
        }

        if (response.status === 403) {
          if (!isCancelled) {
            navigate('/?notice=site-manage-denied', { replace: true })
          }
          return
        }

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
  }, [navigate, siteManagerUser, user])

  const updatePermissions = async (
    actor: SiteManagerSession,
    listedUser: AuthUserListItem,
    nextPermissions: UpdateUserPermissionsRequest,
  ) => {
    void actor
    const targetUsername = usernameValue(listedUser.username)
    setUpdatingUsername(targetUsername)
    setUserListError('')
    setStatusMessage('')

    try {
      const response = await fetch(`/api/auth/users/${encodeURIComponent(targetUsername)}/permissions`, {
        method: 'POST',
        credentials: 'same-origin',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(nextPermissions),
      })

      if (response.status === 401) {
        clearAuthSession()
        setUser(null)
        navigate('/login?notice=session-expired')
        return
      }

      if (response.status === 403) {
        navigate('/?notice=site-manage-denied', { replace: true })
        return
      }

      if (!response.ok) {
        throw new Error('Unable to update permissions.')
      }

      const updatedUser = (await response.json()) as AuthUserListItem

      setUsers((currentUsers) =>
        currentUsers.map((currentUser) =>
          usernameValue(currentUser.username) === usernameValue(updatedUser.username) ? updatedUser : currentUser,
        ),
      )
      setStatusMessage(`Permissions updated for ${usernameValue(updatedUser.username)}.`)
    } catch {
      setUserListError('Unable to update user permissions.')
    } finally {
      setUpdatingUsername(null)
    }
  }

  return (
    <main className="min-h-screen bg-[linear-gradient(180deg,#fffaf4_0%,#f4efe5_100%)] px-6 py-12 sm:px-8">
      <section className="mx-auto max-w-6xl">
        <div className="mb-8 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="space-y-2">
            <p className="text-sm uppercase tracking-[0.25em] text-stone-500">Qiwen Online Judge</p>
            <h1 className="font-['Georgia'] text-4xl font-semibold tracking-tight text-stone-950">
              Site Management
            </h1>
            <p className="text-sm text-stone-600">
              Signed in as {displayNameValue(user.displayName)} ({usernameValue(user.username)}).
            </p>
          </div>

          <div className="flex flex-col gap-3 sm:flex-row">
            <Button asChild variant="outline" className="rounded-full border-stone-300 bg-white">
              <Link to="/">
                <ArrowLeft className="size-4" />
                Back to Dashboard
              </Link>
            </Button>
            <Button
              type="button"
              variant="outline"
              className="rounded-full border-stone-300 bg-white"
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

        <Card className="border-stone-200 bg-white shadow-[0_24px_60px_rgba(28,25,23,0.08)]">
          <CardHeader>
            <div className="flex items-center gap-3">
              <div className="flex size-12 items-center justify-center rounded-2xl bg-orange-100 text-orange-700">
                <Settings2 className="size-5" />
              </div>
              <div>
                <CardTitle className="text-xl text-stone-950">User Management</CardTitle>
                <CardDescription>
                  Site managers can review all registered users and current permission flags.
                </CardDescription>
              </div>
            </div>
          </CardHeader>
          <CardContent className="space-y-4">
            {statusMessage ? (
              <Alert className="rounded-2xl border-emerald-200 bg-emerald-50/95">
                <AlertDescription className="text-emerald-700">{statusMessage}</AlertDescription>
              </Alert>
            ) : null}
            {userListError ? (
              <Alert variant="destructive" className="rounded-2xl border-rose-200 bg-rose-50/95">
                <AlertDescription className="text-rose-700">{userListError}</AlertDescription>
              </Alert>
            ) : null}
            {isLoadingUsers ? (
              <p className="text-sm text-stone-500">Loading users...</p>
            ) : users.length === 0 ? (
              <div className="rounded-3xl border border-dashed border-stone-300 bg-stone-50 px-6 py-10 text-center">
                <p className="text-base font-medium text-stone-900">No users are available yet.</p>
                <p className="mt-2 text-sm leading-7 text-stone-600">
                  This management flow is still valid: the empty state is explicit, and you can return to the dashboard.
                </p>
                <Button asChild variant="outline" className="mt-5 rounded-full border-stone-300 bg-white">
                  <Link to="/">Back to Dashboard</Link>
                </Button>
              </div>
            ) : (
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Username</TableHead>
                    <TableHead>Display name</TableHead>
                    <TableHead>Email</TableHead>
                    <TableHead>Settings</TableHead>
                    <TableHead>Site manager</TableHead>
                    <TableHead>Problem manager</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {users.map((listedUser) => (
                    <TableRow key={usernameValue(listedUser.username)}>
                      <TableCell className="font-medium text-stone-900">
                        {usernameValue(listedUser.username)}
                      </TableCell>
                      <TableCell>{displayNameValue(listedUser.displayName)}</TableCell>
                      <TableCell>{emailAddressValue(listedUser.email)}</TableCell>
                      <TableCell>
                        <Button asChild variant="outline" size="sm" className="rounded-full border-stone-300 bg-white">
                          <Link to={`/user/${usernameValue(listedUser.username)}/settings`}>
                            Open settings
                          </Link>
                        </Button>
                      </TableCell>
                      <TableCell>
                        <Checkbox
                          checked={listedUser.siteManager}
                          disabled={
                            updatingUsername === usernameValue(listedUser.username) || isProtectedAdmin(listedUser)
                          }
                          aria-label="Site manager"
                          onCheckedChange={(checked) => {
                            if (siteManagerUser) {
                              void updatePermissions(siteManagerUser, listedUser, {
                                siteManager: checked === true,
                                problemManager: listedUser.problemManager,
                              })
                            }
                          }}
                        />
                      </TableCell>
                      <TableCell>
                        <Checkbox
                          checked={listedUser.problemManager}
                          disabled={
                            updatingUsername === usernameValue(listedUser.username) || isProtectedAdmin(listedUser)
                          }
                          aria-label="Problem manager"
                          onCheckedChange={(checked) => {
                            if (siteManagerUser) {
                              void updatePermissions(siteManagerUser, listedUser, {
                                siteManager: listedUser.siteManager,
                                problemManager: checked === true,
                              })
                            }
                          }}
                        />
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            )}
          </CardContent>
        </Card>
      </section>
    </main>
  )
}
