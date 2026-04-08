import { useDeferredValue, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Link2, PencilLine, Trash2 } from 'lucide-react'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { Textarea } from '@/components/ui/textarea'
import { ConfirmActionDialog } from '@/shared/components/confirm-action-dialog'
import { MarkdownDocument } from '@/shared/components/markdown-document'

type EditProblemSetDialogProps = {
  open: boolean
  title: string
  description: string
  linkProblemSlug: string
  isSaving: boolean
  isDeleting: boolean
  activeLink: boolean
  contentErrorMessage: string
  contentSuccessMessage: string
  linkErrorMessage: string
  linkSuccessMessage: string
  onOpenChange: (open: boolean) => void
  onTitleChange: (value: string) => void
  onDescriptionChange: (value: string) => void
  onLinkProblemSlugChange: (value: string) => void
  onSaveContent: () => void
  onAttachProblem: () => void
  onDeleteProblemSet: () => Promise<boolean>
}

export function EditProblemSetDialog({
  open,
  title,
  description,
  linkProblemSlug,
  isSaving,
  isDeleting,
  activeLink,
  contentErrorMessage,
  contentSuccessMessage,
  linkErrorMessage,
  linkSuccessMessage,
  onOpenChange,
  onTitleChange,
  onDescriptionChange,
  onLinkProblemSlugChange,
  onSaveContent,
  onAttachProblem,
  onDeleteProblemSet,
}: EditProblemSetDialogProps) {
  const navigate = useNavigate()
  const [descriptionTab, setDescriptionTab] = useState<'write' | 'preview'>('write')
  const deferredDescription = useDeferredValue(description)

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
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
            <Input
              id="problem-set-title"
              value={title}
              onChange={(event) => {
                onTitleChange(event.target.value)
              }}
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
                  value={description}
                  className="min-h-48 !font-mono"
                  onChange={(event) => {
                    onDescriptionChange(event.target.value)
                  }}
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
            disabled={isSaving}
            onClick={onSaveContent}
          >
            {isSaving ? 'Saving content...' : 'Save content'}
          </Button>
          {contentErrorMessage ? (
            <Alert variant="destructive" className="rounded-2xl border-rose-200 bg-rose-50/95">
              <AlertDescription className="text-rose-700">{contentErrorMessage}</AlertDescription>
            </Alert>
          ) : null}
          {contentSuccessMessage ? (
            <Alert className="rounded-2xl border-emerald-200 bg-emerald-50/95">
              <AlertDescription className="text-emerald-700">{contentSuccessMessage}</AlertDescription>
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
              {linkErrorMessage ? (
                <Alert variant="destructive" className="rounded-2xl border-rose-200 bg-rose-50/95">
                  <AlertDescription className="text-rose-700">{linkErrorMessage}</AlertDescription>
                </Alert>
              ) : null}
              {linkSuccessMessage ? (
                <Alert className="rounded-2xl border-emerald-200 bg-emerald-50/95">
                  <AlertDescription className="text-emerald-700">{linkSuccessMessage}</AlertDescription>
                </Alert>
              ) : null}
              <div className="space-y-2">
                <Label htmlFor="link-problem-slug">Problem slug</Label>
                <Input
                  id="link-problem-slug"
                  value={linkProblemSlug}
                  placeholder="two-sum-intro"
                  onChange={(event) => {
                    onLinkProblemSlugChange(event.target.value)
                  }}
                />
              </div>
              <Button
                type="button"
                variant="outline"
                className="rounded-2xl border-slate-300 bg-white"
                disabled={activeLink}
                onClick={onAttachProblem}
              >
                {activeLink ? 'Linking problem...' : 'Link problem'}
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
                <p className="text-sm text-rose-900/80">This removes the problem set and all of its current problem links.</p>
              </div>
            </div>
            <div className="mt-5">
              <ConfirmActionDialog
                title="Delete problem set?"
                description="Delete this problem set and all of its current problem links. This action cannot be undone."
                confirmLabel={isDeleting ? 'Deleting...' : 'Delete problem set'}
                destructive
                onConfirm={() => {
                  void onDeleteProblemSet().then((deleted) => {
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
                    disabled={isDeleting}
                  >
                    {isDeleting ? 'Deleting...' : 'Delete problem set'}
                  </Button>
                }
              />
            </div>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  )
}
