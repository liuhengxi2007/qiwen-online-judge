import { useDeferredValue, useState } from 'react'
import { Link, Navigate, useNavigate, useParams } from 'react-router-dom'
import { ArrowLeft, Link2, LogOut, PencilLine, Rows3, ShieldCheck, Trash2 } from 'lucide-react'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { Textarea } from '@/components/ui/textarea'
import { displayNameValue, usernameValue } from '@/features/auth/domain/auth'
import { useSessionGuard } from '@/features/auth/hooks/use-session-guard'
import {
  parseProblemSetSlug,
  problemSetDescriptionValue,
  problemSetProblemPositionValue,
  problemSetSlugValue,
  problemSetTitleValue,
} from '@/features/problemset/domain/problemset'
import { useProblemSetDetailPageModel } from '@/features/problemset/hooks/use-problemset-detail-page-model'
import { ConfirmActionDialog } from '@/shared/components/confirm-action-dialog'
import { MarkdownDocument } from '@/shared/components/markdown-document'
import { ResourceAccessEditor } from '@/shared/components/resource-access-editor'
import {
  grantedGroupsInputFromAccessPolicy,
  grantedUsersInputFromAccessPolicy,
  normalizeAccessSubjectInput,
} from '@/shared/domain/resource-access-input'
import { resourceAccessBadgeLabel, resourceAccessSummary } from '@/shared/domain/resource-lifecycle'
import { useBeforeUnloadPrompt } from '@/shared/hooks/use-before-unload-prompt'
import { usePageTitle } from '@/shared/hooks/use-page-title'

export function ProblemSetDetailPage() {
  usePageTitle('Qiwen Online Judge - Problem Set Detail')
  const { session: user, signOut, navigationIntent } = useSessionGuard()
  const navigate = useNavigate()
  const { slug } = useParams<{ slug: string }>()

  if (navigationIntent) {
    return <Navigate replace={navigationIntent.replace} to={navigationIntent.to} />
  }

  if (!user) {
    return <Navigate replace to="/login" />
  }

  const slugResult = parseProblemSetSlug(slug ?? '')
  if (!slugResult.ok) {
    return <Navigate replace to="/problem-sets" />
  }

  const canManageProblems = user.siteManager || user.problemManager
  const model = useProblemSetDetailPageModel(slugResult.value, canManageProblems)
  const [managementPanel, setManagementPanel] = useState<'edit' | 'access' | null>(null)
  const [descriptionTab, setDescriptionTab] = useState<'write' | 'preview'>('write')
  const deferredDescription = useDeferredValue(model.description)
  const hasUnsavedChanges =
    model.problemSet !== null &&
    (model.title !== problemSetTitleValue(model.problemSet.title) ||
      model.description !== problemSetDescriptionValue(model.problemSet.description) ||
      model.baseAccess !== model.problemSet.accessPolicy.baseAccess ||
      normalizeAccessSubjectInput(model.grantedUsersInput) !==
        grantedUsersInputFromAccessPolicy(model.problemSet.accessPolicy) ||
      normalizeAccessSubjectInput(model.grantedGroupsInput) !==
        grantedGroupsInputFromAccessPolicy(model.problemSet.accessPolicy) ||
      model.linkProblemSlug.trim().length > 0)

  useBeforeUnloadPrompt(hasUnsavedChanges)

  return (
    <main className="min-h-screen bg-[linear-gradient(180deg,#f8fafc_0%,#ecf3fb_100%)] px-6 py-12 sm:px-8">
      <section className="mx-auto max-w-5xl">
        <div className="mb-8 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="space-y-2">
            <p className="text-sm uppercase tracking-[0.25em] text-slate-500">Qiwen Online Judge</p>
            <h1 className="font-['Georgia'] text-4xl font-semibold tracking-tight text-slate-950">Problem Set Detail</h1>
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

        {!model.isLoading && !model.problemSet && model.loadErrorMessage ? (
          <Alert variant="destructive" className="rounded-2xl border-rose-200 bg-rose-50/95">
            <AlertDescription className="text-rose-700">{model.loadErrorMessage}</AlertDescription>
          </Alert>
        ) : null}

        {model.isLoading ? (
          <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
            <CardContent className="py-10 text-sm text-slate-500">Loading problem set detail...</CardContent>
          </Card>
        ) : model.problemSet ? (
          <div className="space-y-6">
            <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
              <CardHeader>
                <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
                  <div>
                    <CardTitle className="text-2xl text-slate-950">{problemSetTitleValue(model.problemSet.title)}</CardTitle>
                    <CardDescription className="mt-2 font-mono text-sm text-slate-500">
                      {problemSetSlugValue(model.problemSet.slug)}
                    </CardDescription>
                  </div>

                  {canManageProblems ? (
                    <div className="flex flex-wrap gap-3">
                      <Button
                        type="button"
                        variant={managementPanel === 'edit' ? 'default' : 'outline'}
                        className={
                          managementPanel === 'edit'
                            ? 'rounded-2xl bg-amber-600 text-white hover:bg-amber-700'
                            : 'rounded-2xl border-amber-300 bg-white text-amber-800 hover:bg-amber-50'
                        }
                        onClick={() => {
                          setManagementPanel((currentPanel) => (currentPanel === 'edit' ? null : 'edit'))
                        }}
                      >
                        <PencilLine className="size-4" />
                        Edit problem set
                      </Button>
                      <Button
                        type="button"
                        variant={managementPanel === 'access' ? 'default' : 'outline'}
                        className={
                          managementPanel === 'access'
                            ? 'rounded-2xl bg-teal-700 text-white hover:bg-teal-800'
                            : 'rounded-2xl border-teal-300 bg-white text-teal-800 hover:bg-teal-50'
                        }
                        onClick={() => {
                          setManagementPanel((currentPanel) => (currentPanel === 'access' ? null : 'access'))
                        }}
                      >
                        <ShieldCheck className="size-4" />
                        Access management
                      </Button>
                    </div>
                  ) : null}
                </div>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="flex flex-wrap items-center gap-3">
                  <Badge variant="secondary">{resourceAccessBadgeLabel(model.problemSet.accessPolicy)}</Badge>
                </div>
                <div className="rounded-3xl border border-slate-200 bg-slate-50 px-6 py-6">
                  {problemSetDescriptionValue(model.problemSet.description) ? (
                    <MarkdownDocument content={problemSetDescriptionValue(model.problemSet.description)} />
                  ) : (
                    <p className="text-sm text-slate-500">No description provided.</p>
                  )}
                </div>
                <p className="text-xs uppercase tracking-[0.18em] text-slate-400">
                  Owner {usernameValue(model.problemSet.ownerUsername)}
                </p>
              </CardContent>
            </Card>

            <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
              <CardHeader>
                <div className="flex items-center gap-3">
                  <div className="flex size-12 items-center justify-center rounded-2xl bg-violet-100 text-violet-700">
                    <Rows3 className="size-5" />
                  </div>
                  <div>
                    <CardTitle className="text-xl text-slate-950">Linked Problems</CardTitle>
                    <CardDescription>Detailed linked problem information lives here instead of the list page.</CardDescription>
                  </div>
                </div>
              </CardHeader>
              <CardContent className="space-y-4">
                {model.problemListErrorMessage ? (
                  <Alert variant="destructive" className="rounded-2xl border-rose-200 bg-rose-50/95">
                    <AlertDescription className="text-rose-700">{model.problemListErrorMessage}</AlertDescription>
                  </Alert>
                ) : null}
                {model.problemListSuccessMessage ? (
                  <Alert className="rounded-2xl border-emerald-200 bg-emerald-50/95">
                    <AlertDescription className="text-emerald-700">{model.problemListSuccessMessage}</AlertDescription>
                  </Alert>
                ) : null}
                {model.problemSet.problems.length === 0 ? (
                  <p className="text-sm text-slate-500">No problems linked yet.</p>
                ) : (
                  model.problemSet.problems.map((problem) => (
                    <div key={problem.id} className="rounded-2xl border border-slate-200 bg-slate-50 p-4">
                      <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
                        <div>
                          <div className="flex flex-wrap items-center gap-2">
                            <Badge variant="outline">#{problemSetProblemPositionValue(problem.position)}</Badge>
                            <Link className="text-sm font-medium text-slate-900 hover:underline" to={`/problems/${problem.slug}`}>
                              {problem.title}
                            </Link>
                          </div>
                          <p className="mt-1 font-mono text-xs text-slate-500">{problem.slug}</p>
                        </div>
                        {canManageProblems ? (
                          <Button
                            type="button"
                            variant="outline"
                            className="rounded-2xl border-rose-200 bg-white text-rose-700 hover:bg-rose-50 hover:text-rose-800"
                            disabled={model.activeRemovingProblemSlug === problem.slug}
                            onClick={() => {
                              void model.removeProblem(problem.slug)
                            }}
                          >
                            {model.activeRemovingProblemSlug === problem.slug ? 'Removing...' : 'Remove'}
                          </Button>
                        ) : null}
                      </div>
                    </div>
                  ))
                )}
              </CardContent>
            </Card>

          </div>
        ) : null}
      </section>

      <Dialog
        open={canManageProblems && managementPanel === 'edit'}
        onOpenChange={(open) => {
          setManagementPanel(open ? 'edit' : null)
        }}
      >
        <DialogContent className="max-h-[calc(100vh-2rem)] max-w-4xl overflow-y-auto rounded-[2rem] border-slate-200 bg-white p-0 shadow-[0_28px_90px_rgba(15,23,42,0.22)]">
          <DialogHeader className="border-b border-slate-200 px-7 py-6 sm:px-8">
            <DialogTitle className="flex items-center gap-3 text-2xl text-slate-950">
              <span className="flex size-12 items-center justify-center rounded-2xl bg-amber-100 text-amber-700">
                <PencilLine className="size-5" />
              </span>
              Edit Problem Set
            </DialogTitle>
            <DialogDescription className="text-sm leading-7 text-slate-600">
              Edit content, manage linked problems, and handle destructive actions in a front-layer card.
            </DialogDescription>
          </DialogHeader>

          <div className="space-y-6 px-7 py-7 sm:px-8">
            <div className="space-y-2">
              <Label htmlFor="problem-set-title">Title</Label>
              <Input id="problem-set-title" value={model.title} onChange={(event) => model.setTitle(event.target.value)} />
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
            <Button
              type="button"
              className="rounded-2xl bg-slate-950 text-white hover:bg-slate-800"
              disabled={model.isSaving}
              onClick={() => {
                void model.saveContent()
              }}
            >
              {model.isSaving ? 'Saving content...' : 'Save content'}
            </Button>
            {model.contentErrorMessage ? (
              <Alert variant="destructive" className="rounded-2xl border-rose-200 bg-rose-50/95">
                <AlertDescription className="text-rose-700">{model.contentErrorMessage}</AlertDescription>
              </Alert>
            ) : null}
            {model.contentSuccessMessage ? (
              <Alert className="rounded-2xl border-emerald-200 bg-emerald-50/95">
                <AlertDescription className="text-emerald-700">{model.contentSuccessMessage}</AlertDescription>
              </Alert>
            ) : null}

            <div className="rounded-[1.75rem] border border-slate-200 bg-slate-50 p-6">
              <div className="flex items-center gap-3">
                <div className="flex size-12 items-center justify-center rounded-2xl bg-sky-100 text-sky-700">
                  <Link2 className="size-5" />
                </div>
                <div>
                  <h2 className="text-xl font-semibold text-slate-950">Link Problem</h2>
                  <p className="text-sm text-slate-600">Add an existing problem into this problem set by slug.</p>
                </div>
              </div>
              <div className="mt-5 space-y-3">
                {model.linkErrorMessage ? (
                  <Alert variant="destructive" className="rounded-2xl border-rose-200 bg-rose-50/95">
                    <AlertDescription className="text-rose-700">{model.linkErrorMessage}</AlertDescription>
                  </Alert>
                ) : null}
                {model.linkSuccessMessage ? (
                  <Alert className="rounded-2xl border-emerald-200 bg-emerald-50/95">
                    <AlertDescription className="text-emerald-700">{model.linkSuccessMessage}</AlertDescription>
                  </Alert>
                ) : null}
                <div className="space-y-2">
                  <Label htmlFor="link-problem-slug">Problem slug</Label>
                  <Input
                    id="link-problem-slug"
                    value={model.linkProblemSlug}
                    placeholder="two-sum-intro"
                    onChange={(event) => model.setLinkProblemSlug(event.target.value)}
                  />
                </div>
                <Button
                  type="button"
                  variant="outline"
                  className="rounded-2xl border-slate-300 bg-white"
                  disabled={model.activeLink}
                  onClick={() => {
                    void model.attachProblem()
                  }}
                >
                  {model.activeLink ? 'Linking problem...' : 'Link problem'}
                </Button>
              </div>
            </div>

            <div className="rounded-[1.75rem] border border-rose-200 bg-rose-50/60 p-6">
              <div className="flex items-center gap-3">
                <div className="flex size-12 items-center justify-center rounded-2xl bg-rose-100 text-rose-700">
                  <Trash2 className="size-5" />
                </div>
                <div>
                  <h2 className="text-xl font-semibold text-rose-950">Delete Problem Set</h2>
                  <p className="text-sm text-rose-900/80">
                    This removes the problem set and all of its current problem links.
                  </p>
                </div>
              </div>
              <div className="mt-5">
                <ConfirmActionDialog
                  title="Delete problem set?"
                  description="Delete this problem set and all of its current problem links. This action cannot be undone."
                  confirmLabel={model.isDeleting ? 'Deleting...' : 'Delete problem set'}
                  destructive
                  onConfirm={() => {
                    void model.deleteCurrentProblemSet().then((deleted) => {
                      if (deleted) {
                        void navigate('/problem-sets')
                      }
                    })
                  }}
                  trigger={
                    <Button
                      type="button"
                      variant="outline"
                      className="rounded-2xl border-rose-300 bg-white text-rose-700 hover:bg-rose-100 hover:text-rose-800"
                      disabled={model.isDeleting}
                    >
                      {model.isDeleting ? 'Deleting...' : 'Delete problem set'}
                    </Button>
                  }
                />
              </div>
            </div>
          </div>
        </DialogContent>
      </Dialog>

      <Dialog
        open={canManageProblems && managementPanel === 'access'}
        onOpenChange={(open) => {
          setManagementPanel(open ? 'access' : null)
        }}
      >
        <DialogContent className="max-h-[calc(100vh-2rem)] max-w-3xl overflow-y-auto rounded-[2rem] border-slate-200 bg-white p-0 shadow-[0_28px_90px_rgba(15,23,42,0.22)]">
          <DialogHeader className="border-b border-slate-200 px-7 py-6 sm:px-8">
            <DialogTitle className="flex items-center gap-3 text-2xl text-slate-950">
              <span className="flex size-12 items-center justify-center rounded-2xl bg-teal-100 text-teal-700">
                <ShieldCheck className="size-5" />
              </span>
              Access Management
            </DialogTitle>
            <DialogDescription className="text-sm leading-7 text-slate-600">
              Update who can view this problem set.
            </DialogDescription>
          </DialogHeader>

          <div className="space-y-5 px-7 py-7 sm:px-8">
            <p className="text-sm text-slate-600">
              {resourceAccessSummary(model.problemSet?.accessPolicy ?? model.accessPolicy)}
            </p>
            <ResourceAccessEditor
              accessPolicy={model.accessPolicy}
              grantedUsersInput={model.grantedUsersInput}
              grantedGroupsInput={model.grantedGroupsInput}
              onBaseAccessChange={model.setBaseAccess}
              onGrantedUsersInputChange={model.setGrantedUsersInput}
              onGrantedGroupsInputChange={model.setGrantedGroupsInput}
            />
            <Button
              type="button"
              className="rounded-2xl bg-slate-950 text-white hover:bg-slate-800"
              disabled={model.isSaving}
              onClick={() => {
                void model.saveAccess()
              }}
            >
              {model.isSaving ? 'Saving access...' : 'Save access'}
            </Button>
            {model.accessErrorMessage ? (
              <Alert variant="destructive" className="rounded-2xl border-rose-200 bg-rose-50/95">
                <AlertDescription className="text-rose-700">{model.accessErrorMessage}</AlertDescription>
              </Alert>
            ) : null}
            {model.accessSuccessMessage ? (
              <Alert className="rounded-2xl border-emerald-200 bg-emerald-50/95">
                <AlertDescription className="text-emerald-700">{model.accessSuccessMessage}</AlertDescription>
              </Alert>
            ) : null}
          </div>
        </DialogContent>
      </Dialog>
    </main>
  )
}
