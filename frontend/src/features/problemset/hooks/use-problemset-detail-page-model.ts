import { useState } from 'react'

import type { ProblemSlug } from '@/features/problem/domain/problem'
import type { ProblemSetSlug } from '@/features/problemset/domain/problemset'
import { useProblemSetDeleteAction } from '@/features/problemset/hooks/use-problemset-delete-action'
import { useProblemSetDetailQuery } from '@/features/problemset/hooks/use-problemset-detail-query'
import { useProblemSetEditorState } from '@/features/problemset/hooks/use-problemset-editor-state'
import { useProblemSetLinkProblemAction } from '@/features/problemset/hooks/use-problemset-link-problem-action'
import { useProblemSetRemoveProblemAction } from '@/features/problemset/hooks/use-problemset-remove-problem-action'
import { useProblemSetUpdateAction } from '@/features/problemset/hooks/use-problemset-update-action'

export function useProblemSetDetailPageModel(problemSetSlug: ProblemSetSlug, canManageProblems: boolean) {
  const detailQuery = useProblemSetDetailQuery(problemSetSlug)
  const editor = useProblemSetEditorState(detailQuery.problemSet)
  const updateAction = useProblemSetUpdateAction(problemSetSlug)
  const deleteAction = useProblemSetDeleteAction(problemSetSlug)
  const linkAction = useProblemSetLinkProblemAction(problemSetSlug)
  const removeAction = useProblemSetRemoveProblemAction(problemSetSlug)
  const [messageState, setMessageState] = useState<{ errorMessage: string; successMessage: string }>({
    errorMessage: '',
    successMessage: '',
  })

  async function save() {
    if (!canManageProblems) {
      setMessageState({ errorMessage: 'Problem manager permission required.', successMessage: '' })
      return
    }

    const result = await updateAction.save({
      title: editor.title,
      description: editor.description,
      visibility: editor.visibility,
    })

    if (result.ok) {
      setMessageState({ errorMessage: '', successMessage: result.message })
    } else {
      setMessageState({ errorMessage: result.message, successMessage: '' })
    }
  }

  async function attachProblem() {
    if (!canManageProblems) {
      setMessageState({ errorMessage: 'Problem manager permission required.', successMessage: '' })
      return
    }

    const result = await linkAction.attachProblem(editor.linkProblemSlug)
    if (result.ok) {
      editor.clearLinkedProblemSlug()
      setMessageState({ errorMessage: '', successMessage: result.message })
    } else {
      setMessageState({ errorMessage: result.message, successMessage: '' })
    }
  }

  async function removeProblem(problemSlug: ProblemSlug) {
    if (!canManageProblems) {
      setMessageState({ errorMessage: 'Problem manager permission required.', successMessage: '' })
      return
    }

    const result = await removeAction.removeProblem(problemSlug)
    if (result.ok) {
      setMessageState({ errorMessage: '', successMessage: result.message })
    } else {
      setMessageState({ errorMessage: result.message, successMessage: '' })
    }
  }

  async function deleteCurrentProblemSet() {
    if (!canManageProblems) {
      setMessageState({ errorMessage: 'Problem manager permission required.', successMessage: '' })
      return false
    }

    const result = await deleteAction.deleteCurrentProblemSet()
    if (result.ok) {
      setMessageState({ errorMessage: '', successMessage: result.message })
      return true
    }

    setMessageState({ errorMessage: result.message, successMessage: '' })
    return false
  }

  return {
    problemSet: detailQuery.problemSet,
    isLoading: detailQuery.isLoading,
    activeLink: linkAction.activeLink,
    isSaving: updateAction.isSaving,
    isDeleting: deleteAction.isDeleting,
    activeRemovingProblemSlug: removeAction.activeRemovingProblemSlug,
    title: editor.title,
    description: editor.description,
    visibility: editor.visibility,
    linkProblemSlug: editor.linkProblemSlug,
    errorMessage: detailQuery.errorMessage || messageState.errorMessage,
    successMessage: detailQuery.errorMessage ? '' : messageState.successMessage,
    setTitle: editor.setTitle,
    setDescription: editor.setDescription,
    setVisibility: editor.setVisibility,
    setLinkProblemSlug: editor.setLinkProblemSlug,
    save,
    removeProblem,
    deleteCurrentProblemSet,
    attachProblem,
  }
}
