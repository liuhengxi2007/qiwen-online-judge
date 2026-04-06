import { Link, Navigate } from 'react-router-dom'
import { ArrowLeft, LogOut, Users } from 'lucide-react'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { displayNameValue, usernameValue } from '@/features/auth/domain/auth'
import { useSessionGuard } from '@/features/auth/hooks/use-session-guard'
import { useCreateUserGroupPageModel } from '@/features/usergroup/hooks/use-create-usergroup-page-model'
import { useBeforeUnloadPrompt } from '@/shared/hooks/use-before-unload-prompt'
import { usePageTitle } from '@/shared/hooks/use-page-title'

export function CreateUserGroupPage() {
  usePageTitle('Qiwen Online Judge - Create User Group')
  const { session: user, signOut, navigationIntent } = useSessionGuard()
  const model = useCreateUserGroupPageModel()
  const hasUnsavedChanges =
    model.slug.trim().length > 0 || model.name.trim().length > 0 || model.description.trim().length > 0

  useBeforeUnloadPrompt(hasUnsavedChanges)

  if (navigationIntent) {
    return <Navigate replace={navigationIntent.replace} to={navigationIntent.to} />
  }

  if (!user) {
    return <Navigate replace to="/login" />
  }

  return (
    <main className="min-h-screen bg-[linear-gradient(180deg,#f8fafc_0%,#edf4fb_100%)] px-6 py-12 sm:px-8">
      <section className="mx-auto max-w-3xl">
        <div className="mb-8 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="space-y-2">
            <p className="text-sm uppercase tracking-[0.25em] text-slate-500">Qiwen Online Judge</p>
            <h1 className="font-['Georgia'] text-4xl font-semibold tracking-tight text-slate-950">Create User Group</h1>
            <p className="text-sm text-slate-600">
              Signed in as {displayNameValue(user.displayName)} ({usernameValue(user.username)}).
            </p>
          </div>

          <div className="flex flex-col gap-3 sm:flex-row">
            <Button asChild variant="outline" className="rounded-full border-slate-300 bg-white">
              <Link to="/user-groups">
                <ArrowLeft className="size-4" />
                Back to User Groups
              </Link>
            </Button>
            <Button
              type="button"
              variant="outline"
              className="rounded-full border-slate-300 bg-white"
              onClick={() => {
                void signOut()
              }}
            >
              <LogOut className="size-4" />
              Sign out
            </Button>
          </div>
        </div>

        <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
          <CardHeader>
            <div className="flex items-center gap-3">
              <div className="flex size-12 items-center justify-center rounded-2xl bg-sky-100 text-sky-700">
                <Users className="size-5" />
              </div>
              <div>
                <CardTitle className="text-xl text-slate-950">Group Metadata</CardTitle>
                <CardDescription>Create a new collaborative group and become its owner automatically.</CardDescription>
              </div>
            </div>
          </CardHeader>
          <CardContent className="space-y-5">
            <div className="space-y-2">
              <Label htmlFor="user-group-slug">Slug</Label>
              <Input
                id="user-group-slug"
                value={model.slug}
                placeholder="round123-testers"
                onChange={(event) => model.setSlug(event.target.value.toLowerCase())}
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="user-group-name">Name</Label>
              <Input
                id="user-group-name"
                value={model.name}
                placeholder="Round 123 Testers"
                onChange={(event) => model.setName(event.target.value)}
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="user-group-description">Description</Label>
              <Textarea
                id="user-group-description"
                value={model.description}
                placeholder="Internal testing group for Round 123 problem preparation."
                onChange={(event) => model.setDescription(event.target.value)}
              />
            </div>

            {model.errorMessage ? (
              <Alert variant="destructive" className="rounded-2xl border-rose-200 bg-rose-50/95">
                <AlertDescription className="text-rose-700">{model.errorMessage}</AlertDescription>
              </Alert>
            ) : null}
            {model.successMessage ? (
              <Alert className="rounded-2xl border-emerald-200 bg-emerald-50/95">
                <AlertDescription className="text-emerald-700">{model.successMessage}</AlertDescription>
              </Alert>
            ) : null}
            <Button
              type="button"
              disabled={model.isSubmitting}
              className="rounded-2xl bg-slate-950 text-white hover:bg-slate-800"
              onClick={() => {
                void model.submit()
              }}
            >
              {model.isSubmitting ? 'Creating user group...' : 'Create user group'}
            </Button>
          </CardContent>
        </Card>
      </section>
    </main>
  )
}
