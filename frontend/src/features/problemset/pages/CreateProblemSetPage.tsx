import { useDeferredValue, useState } from 'react'
import { Link, Navigate } from 'react-router-dom'
import { ArrowLeft, BookPlus, LogOut } from 'lucide-react'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { Textarea } from '@/components/ui/textarea'
import { displayNameValue, usernameValue } from '@/features/auth/domain/auth'
import { useSessionGuard } from '@/features/auth/hooks/use-session-guard'
import { useCreateProblemSetPageModel } from '@/features/problemset/hooks/use-create-problemset-page-model'
import { MarkdownDocument } from '@/shared/components/markdown-document'
import { ResourceAccessEditor } from '@/shared/components/resource-access-editor'
import { usePageTitle } from '@/shared/hooks/use-page-title'

export function CreateProblemSetPage() {
  usePageTitle('Qiwen Online Judge - Create Problem Set')
  const { session: user, signOut, navigationIntent } = useSessionGuard()

  if (navigationIntent) {
    return <Navigate replace={navigationIntent.replace} to={navigationIntent.to} />
  }

  if (!user) {
    return <Navigate replace to="/login" />
  }

  const canCreate = user.siteManager || user.problemManager
  const model = useCreateProblemSetPageModel(canCreate)
  const [descriptionTab, setDescriptionTab] = useState<'write' | 'preview'>('write')
  const deferredDescription = useDeferredValue(model.description)

  return (
    <main className="min-h-screen bg-[linear-gradient(180deg,#f8fafc_0%,#ecf3fb_100%)] px-6 py-12 sm:px-8">
      <section className="mx-auto max-w-3xl">
        <div className="mb-8 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="space-y-2">
            <p className="text-sm uppercase tracking-[0.25em] text-slate-500">Qiwen Online Judge</p>
            <h1 className="font-['Georgia'] text-4xl font-semibold tracking-tight text-slate-950">Create Problem Set</h1>
            <p className="text-sm text-slate-600">
              Signed in as {displayNameValue(user.displayName)} ({usernameValue(user.username)}).
            </p>
          </div>

          <div className="flex flex-col gap-3 sm:flex-row">
            <Button asChild variant="outline" className="rounded-full border-slate-300 bg-white">
              <Link to="/problem-sets">
                <ArrowLeft className="size-4" />
                Back to Problem Sets
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
                <BookPlus className="size-5" />
              </div>
              <div>
                <CardTitle className="text-xl text-slate-950">Problem Set Metadata</CardTitle>
                <CardDescription>
                  This form creates a draft problem set with Markdown and LaTeX description support.
                </CardDescription>
              </div>
            </div>
          </CardHeader>
          <CardContent className="space-y-5">
            {!canCreate ? (
              <Alert className="rounded-2xl border-amber-200 bg-amber-50/95">
                <AlertDescription className="text-amber-900">
                  Problem manager or site manager permission is required to create problem sets.
                </AlertDescription>
              </Alert>
            ) : null}
            <div className="space-y-2">
              <Label htmlFor="problem-set-slug">Slug</Label>
              <Input
                id="problem-set-slug"
                value={model.slug}
                placeholder="graph-theory-ladder"
                onChange={(event) => model.setSlug(event.target.value.toLowerCase())}
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="problem-set-title">Title</Label>
              <Input
                id="problem-set-title"
                value={model.title}
                placeholder="Graph Theory Ladder"
                onChange={(event) => model.setTitle(event.target.value)}
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="problem-set-description">Description</Label>
              <Tabs value={descriptionTab} onValueChange={(value) => setDescriptionTab(value as 'write' | 'preview')}>
                <TabsList className="grid w-full grid-cols-2 rounded-2xl bg-slate-100">
                  <TabsTrigger value="write" className="rounded-xl">
                    Write
                  </TabsTrigger>
                  <TabsTrigger value="preview" className="rounded-xl">
                    Preview
                  </TabsTrigger>
                </TabsList>
                <TabsContent value="write" className="mt-3">
                  <Textarea
                    id="problem-set-description"
                    value={model.description}
                    className="min-h-48 !font-mono"
                    placeholder={
                      '## Graph Theory Ladder\n\nPractice BFS, shortest paths, and spanning tree basics.\n\n- Warm-up graph traversal\n- Weighted shortest path\n\n$$O((n + m) \\log n)$$'
                    }
                    onChange={(event) => model.setDescription(event.target.value)}
                  />
                </TabsContent>
                <TabsContent value="preview" className="mt-3">
                  <div className="rounded-3xl border border-slate-200 bg-slate-50 px-6 py-6">
                    {deferredDescription.trim() ? (
                      <MarkdownDocument content={deferredDescription} />
                    ) : (
                      <p className="text-sm text-slate-500">Nothing to preview yet.</p>
                    )}
                  </div>
                </TabsContent>
              </Tabs>
              <p className="text-xs text-slate-500">
                Supported: headings, lists, emphasis, tables, fenced code blocks, links, images, and LaTeX with
                <code className="mx-1 rounded bg-slate-100 px-1 py-0.5">$...$</code>
                or
                <code className="mx-1 rounded bg-slate-100 px-1 py-0.5">$$...$$</code>.
                Raw HTML is ignored.
              </p>
            </div>

            <ResourceAccessEditor
              accessPolicy={model.accessPolicy}
              grantedUsersInput={model.grantedUsersInput}
              grantedGroupsInput={model.grantedGroupsInput}
              onBaseAccessChange={model.setBaseAccess}
              onGrantedUsersInputChange={model.setGrantedUsersInput}
              onGrantedGroupsInputChange={model.setGrantedGroupsInput}
            />

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
              disabled={model.isSubmitting || !canCreate}
              className="rounded-2xl bg-slate-950 text-white hover:bg-slate-800"
              onClick={() => {
                void model.submit()
              }}
            >
              {model.isSubmitting ? 'Creating problem set...' : 'Create problem set'}
            </Button>
          </CardContent>
        </Card>
      </section>
    </main>
  )
}
