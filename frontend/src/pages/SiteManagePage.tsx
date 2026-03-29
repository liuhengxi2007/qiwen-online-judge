import { useEffect, useState } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { ArrowLeft, LogOut, Settings2 } from 'lucide-react'

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

          if (!asSiteManagerSession(session)) {
            navigate('/')
          }
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

  useEffect(() => {
    if (!siteManagerUser) {
      navigate('/')
      return
    }

    let isCancelled = false

    const loadUsers = async () => {
      setIsLoadingUsers(true)
      setUserListError('')

      try {
        const response = await fetch('/api/auth/users', {
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

        if (response.status === 403) {
          if (!isCancelled) {
            navigate('/')
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
        navigate('/login')
        return
      }

      if (response.status === 403) {
        navigate('/')
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
            {userListError ? <p className="text-sm text-rose-600">{userListError}</p> : null}
            {isLoadingUsers ? (
              <p className="text-sm text-stone-500">Loading users...</p>
            ) : (
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Username</TableHead>
                    <TableHead>Display name</TableHead>
                    <TableHead>Email</TableHead>
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
