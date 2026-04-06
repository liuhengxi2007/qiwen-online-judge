import { useDeferredValue, useState } from 'react'
import { Link, Navigate, useNavigate, useParams } from 'react-router-dom'
import { ArrowLeft, LogOut, PencilLine, ScrollText, Trash2 } from 'lucide-react'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Label } from '@/components/ui/label'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { Textarea } from '@/components/ui/textarea'
import { Input } from '@/components/ui/input'
import { displayNameValue, usernameValue } from '@/features/auth/domain/auth'
import { useSessionGuard } from '@/features/auth/hooks/use-session-guard'
import {
  parseProblemSlug,
  problemSlugValue,
  problemStatementTextValue,
  problemTitleValue,
} from '@/features/problem/domain/problem'
import { useProblemDetailPageModel } from '@/features/problem/hooks/use-problem-detail-page-model'
import { ConfirmActionDialog } from '@/shared/components/confirm-action-dialog'
import { MarkdownDocument } from '@/shared/components/markdown-document'
import { ResourceAccessEditor } from '@/shared/components/resource-access-editor'
import { resourceAccessBadgeLabel, resourceAccessSummary } from '@/shared/domain/resource-lifecycle'
import { useBeforeUnloadPrompt } from '@/shared/hooks/use-before-unload-prompt'
import { usePageTitle } from '@/shared/hooks/use-page-title'

export function ProblemDetailPage() {
  usePageTitle('Qiwen Online Judge - Problem Detail')
  const { session: user, signOut, navigationIntent } = useSessionGuard()
  const navigate = useNavigate()
  const { slug } = useParams<{ slug: string }>()

  if (navigationIntent) {
    return <Navigate replace={navigationIntent.replace} to={navigationIntent.to} />
  }

  if (!user) {
    return <Navigate replace to="/login" />
  }

  const slugResult = parseProblemSlug(slug ?? '')
  if (!slugResult.ok) {
    return <Navigate replace to="/problems" />
  }

  const model = useProblemDetailPageModel(slugResult.value)
  const canManageProblem = user.siteManager || user.problemManager
  const [statementTab, setStatementTab] = useState<'write' | 'preview'>('write')
  const deferredStatement = useDeferredValue(model.statement)
  const hasUnsavedChanges =
    model.problem !== null &&
    (model.title !== problemTitleValue(model.problem.title) ||
      model.statement !== problemStatementTextValue(model.problem.statement) ||
      model.baseAccess !== model.problem.accessPolicy.baseAccess ||
      normalizeGrantInput(model.grantedUsersInput) !== extractGrantInput(model.problem.accessPolicy, 'user') ||
      normalizeGrantInput(model.grantedGroupsInput) !== extractGrantInput(model.problem.accessPolicy, 'user_group'))

  useBeforeUnloadPrompt(hasUnsavedChanges)

  return (
    <main className="min-h-screen bg-[linear-gradient(180deg,#f8fafc_0%,#edf5f1_100%)] px-6 py-12 sm:px-8">
      <section className="mx-auto max-w-5xl">
        <div className="mb-8 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="space-y-2">
            <p className="text-sm uppercase tracking-[0.25em] text-slate-500">Qiwen Online Judge</p>
            <h1 className="font-['Georgia'] text-4xl font-semibold tracking-tight text-slate-950">Problem Detail</h1>
            <p className="text-sm text-slate-600">
              Signed in as {displayNameValue(user.displayName)} ({usernameValue(user.username)}).
            </p>
          </div>

          <div className="flex flex-col gap-3 sm:flex-row">
            <Button asChild variant="outline" className="rounded-full border-slate-300 bg-white">
              <Link to="/problems">
                <ArrowLeft className="size-4" />
                Back to Problems
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

        {!model.isLoading && !model.problem && model.loadErrorMessage ? (
          <Alert variant="destructive" className="rounded-2xl border-rose-200 bg-rose-50/95">
            <AlertDescription className="text-rose-700">{model.loadErrorMessage}</AlertDescription>
          </Alert>
        ) : null}

        {model.isLoading ? (
          <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
            <CardContent className="py-10 text-sm text-slate-500">Loading problem detail...</CardContent>
          </Card>
        ) : model.problem ? (
          <div className="space-y-6">
            <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
              <CardHeader>
                <div className="flex items-center gap-3">
                  <div className="flex size-12 items-center justify-center rounded-2xl bg-emerald-100 text-emerald-700">
                    <ScrollText className="size-5" />
                  </div>
                  <div>
                    <CardTitle className="text-2xl text-slate-950">{problemTitleValue(model.problem.title)}</CardTitle>
                    <CardDescription className="mt-2 font-mono text-sm text-slate-500">
                      {problemSlugValue(model.problem.slug)}
                    </CardDescription>
                  </div>
                </div>
              </CardHeader>
              <CardContent className="space-y-5">
                <div className="flex flex-wrap items-center gap-3">
                  <Badge variant="secondary">{resourceAccessBadgeLabel(model.problem.accessPolicy)}</Badge>
                  <Badge variant="outline">{model.problem.status}</Badge>
                </div>
                <p className="text-sm text-slate-600">{resourceAccessSummary(model.problem.accessPolicy)}</p>
                <div className="rounded-3xl border border-slate-200 bg-slate-50 px-6 py-6">
                  <MarkdownDocument content={problemStatementTextValue(model.problem.statement)} />
                </div>
                <p className="text-xs uppercase tracking-[0.18em] text-slate-400">
                  Owner {usernameValue(model.problem.ownerUsername)}
                </p>
              </CardContent>
            </Card>

            {canManageProblem ? (
              <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
                <CardHeader>
                  <div className="flex items-center gap-3">
                    <div className="flex size-12 items-center justify-center rounded-2xl bg-amber-100 text-amber-700">
                      <PencilLine className="size-5" />
                    </div>
                    <div>
                      <CardTitle className="text-xl text-slate-950">Edit Problem Content</CardTitle>
                      <CardDescription>
                        Update the problem title and restricted Markdown statement.
                      </CardDescription>
                    </div>
                  </div>
                </CardHeader>
                <CardContent className="space-y-5">
                  <div className="space-y-2">
                    <Label htmlFor="problem-title">Title</Label>
                    <Input
                      id="problem-title"
                      value={model.title}
                      onChange={(event) => model.setTitle(event.target.value)}
                    />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="problem-statement">Statement</Label>
                    <Tabs value={statementTab} onValueChange={(value) => setStatementTab(value as 'write' | 'preview')}>
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
                          id="problem-statement"
                          className="min-h-64 !font-mono"
                          value={model.statement}
                          onChange={(event) => model.setStatement(event.target.value)}
                        />
                      </TabsContent>
                      <TabsContent value="preview" className="mt-3">
                        <div className="rounded-3xl border border-slate-200 bg-slate-50 px-6 py-6">
                          {deferredStatement.trim() ? (
                            <MarkdownDocument content={deferredStatement} />
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
                </CardContent>
              </Card>
            ) : null}

            {canManageProblem ? (
              <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
                <CardHeader>
                  <div className="flex items-center gap-3">
                    <div className="flex size-12 items-center justify-center rounded-2xl bg-teal-100 text-teal-700">
                      <PencilLine className="size-5" />
                    </div>
                    <div>
                      <CardTitle className="text-xl text-slate-950">Edit Problem Access</CardTitle>
                      <CardDescription>Update who can view this problem.</CardDescription>
                    </div>
                  </div>
                </CardHeader>
                <CardContent className="space-y-5">
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
                </CardContent>
              </Card>
            ) : null}

            {canManageProblem ? (
              <Card className="border-rose-200 bg-rose-50/60 shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
                <CardHeader>
                  <div className="flex items-center gap-3">
                    <div className="flex size-12 items-center justify-center rounded-2xl bg-rose-100 text-rose-700">
                      <Trash2 className="size-5" />
                    </div>
                    <div>
                      <CardTitle className="text-xl text-rose-950">Delete Problem</CardTitle>
                      <CardDescription>This removes the problem and any existing problem set links to it.</CardDescription>
                    </div>
                  </div>
                </CardHeader>
                <CardContent>
                  <ConfirmActionDialog
                    title="Delete problem?"
                    description="Delete this problem and remove it from all current problem sets. No problem set will be deleted. This action cannot be undone."
                    confirmLabel={model.isDeleting ? 'Deleting...' : 'Delete problem'}
                    destructive
                    onConfirm={() => {
                      void model.deleteCurrentProblem().then((deleted) => {
                        if (deleted) {
                          void navigate('/problems')
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
                        {model.isDeleting ? 'Deleting...' : 'Delete problem'}
                      </Button>
                    }
                  />
                </CardContent>
              </Card>
            ) : null}
          </div>
        ) : null}
      </section>
    </main>
  )
}

function normalizeGrantInput(raw: string): string {
  return raw
    .split(/[\n,]/)
    .map((token) => token.trim())
    .filter((token) => token.length > 0)
    .join('\n')
}

function extractGrantInput(
  accessPolicy: {
    viewerGrants: Array<{ kind: 'user'; username: string } | { kind: 'user_group'; slug: string }>
  },
  kind: 'user' | 'user_group',
): string {
  return accessPolicy.viewerGrants
    .filter((grant) => grant.kind === kind)
    .map((grant) => (grant.kind === 'user' ? grant.username : grant.slug))
    .join('\n')
}
