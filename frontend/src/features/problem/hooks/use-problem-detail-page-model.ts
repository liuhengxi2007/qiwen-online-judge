import { useState } from 'react'

import type { ProblemSlug } from '@/features/problem/domain/problem'
import { useProblemDeleteAction } from '@/features/problem/hooks/use-problem-delete-action'
import { useProblemDetailQuery } from '@/features/problem/hooks/use-problem-detail-query'
import { useProblemEditorState } from '@/features/problem/hooks/use-problem-editor-state'
import { useProblemUpdateAction } from '@/features/problem/hooks/use-problem-update-action'

export function useProblemDetailPageModel(problemSlug: ProblemSlug) {
  const detailQuery = useProblemDetailQuery(problemSlug)
  const editor = useProblemEditorState(detailQuery.problem)
  const updateAction = useProblemUpdateAction(problemSlug)
  const deleteAction = useProblemDeleteAction(problemSlug)
  const [messageState, setMessageState] = useState<{ errorMessage: string; successMessage: string }>({
    errorMessage: '',
    successMessage: '',
  })

  async function save() {
    const result = await updateAction.save({
      title: editor.title,
      statement: editor.statement,
      baseAccess: editor.baseAccess,
      grantedUsersInput: editor.grantedUsersInput,
      grantedGroupsInput: editor.grantedGroupsInput,
    })

    if (result.ok) {
      detailQuery.replaceProblem(result.problem)
      setMessageState({ errorMessage: '', successMessage: result.message })
    } else {
      setMessageState({ errorMessage: result.message, successMessage: '' })
    }
  }

  async function deleteCurrentProblem() {
    const result = await deleteAction.deleteCurrentProblem()
    if (result.ok) {
      setMessageState({ errorMessage: '', successMessage: result.message })
      return true
    }

    setMessageState({ errorMessage: result.message, successMessage: '' })
    return false
  }

  return {
    problem: detailQuery.problem,
    isLoading: detailQuery.isLoading,
    isSaving: updateAction.isSaving,
    isDeleting: deleteAction.isDeleting,
    title: editor.title,
    statement: editor.statement,
    accessPolicy: modelAccessPolicy(editor.baseAccess, editor.grantedUsersInput, editor.grantedGroupsInput),
    baseAccess: editor.baseAccess,
    grantedUsersInput: editor.grantedUsersInput,
    grantedGroupsInput: editor.grantedGroupsInput,
    errorMessage: detailQuery.errorMessage || messageState.errorMessage,
    successMessage: detailQuery.errorMessage ? '' : messageState.successMessage,
    setTitle: editor.setTitle,
    setStatement: editor.setStatement,
    setBaseAccess: editor.setBaseAccess,
    setGrantedUsersInput: editor.setGrantedUsersInput,
    setGrantedGroupsInput: editor.setGrantedGroupsInput,
    save,
    deleteCurrentProblem,
  }
}

function modelAccessPolicy(baseAccess: 'owner_only' | 'public', grantedUsersInput: string, grantedGroupsInput: string) {
  return {
    baseAccess,
    viewerGrants: [
      ...splitGrantInput(grantedGroupsInput).map((slug) => ({ kind: 'user_group' as const, slug })),
      ...splitGrantInput(grantedUsersInput).map((username) => ({ kind: 'user' as const, username })),
    ],
  }
}

function splitGrantInput(raw: string): string[] {
  return raw
    .split(/[\n,]/)
    .map((token) => token.trim())
    .filter((token) => token.length > 0)
}
