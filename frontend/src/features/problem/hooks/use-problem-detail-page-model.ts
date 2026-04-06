import { useState } from 'react'

import type { ProblemSlug } from '@/features/problem/domain/problem'
import { problemStatementTextValue, problemTitleValue } from '@/features/problem/domain/problem'
import { useProblemDeleteAction } from '@/features/problem/hooks/use-problem-delete-action'
import { useProblemDetailQuery } from '@/features/problem/hooks/use-problem-detail-query'
import { useProblemEditorState } from '@/features/problem/hooks/use-problem-editor-state'
import { useProblemUpdateAction } from '@/features/problem/hooks/use-problem-update-action'

type SectionMessageState = {
  errorMessage: string
  successMessage: string
}

const emptySectionMessageState: SectionMessageState = {
  errorMessage: '',
  successMessage: '',
}

export function useProblemDetailPageModel(problemSlug: ProblemSlug) {
  const detailQuery = useProblemDetailQuery(problemSlug)
  const editor = useProblemEditorState(detailQuery.problem)
  const updateAction = useProblemUpdateAction(problemSlug)
  const deleteAction = useProblemDeleteAction(problemSlug)
  const [contentMessageState, setContentMessageState] = useState<SectionMessageState>(emptySectionMessageState)
  const [accessMessageState, setAccessMessageState] = useState<SectionMessageState>(emptySectionMessageState)

  async function saveContent() {
    const currentProblem = detailQuery.problem
    if (!currentProblem) {
      setContentMessageState({ errorMessage: 'Problem detail is not loaded.', successMessage: '' })
      return
    }

    const result = await updateAction.save({
      title: editor.title,
      statement: editor.statement,
      baseAccess: currentProblem.accessPolicy.baseAccess,
      grantedUsersInput: grantInputFromPolicy(currentProblem.accessPolicy, 'user'),
      grantedGroupsInput: grantInputFromPolicy(currentProblem.accessPolicy, 'user_group'),
    })

    if (result.ok) {
      detailQuery.replaceProblem(result.problem)
      setContentMessageState({ errorMessage: '', successMessage: result.message })
    } else {
      setContentMessageState({ errorMessage: result.message, successMessage: '' })
    }
  }

  async function saveAccess() {
    const currentProblem = detailQuery.problem
    if (!currentProblem) {
      setAccessMessageState({ errorMessage: 'Problem detail is not loaded.', successMessage: '' })
      return
    }

    const result = await updateAction.save({
      title: problemTitleValue(currentProblem.title),
      statement: problemStatementTextValue(currentProblem.statement),
      baseAccess: editor.baseAccess,
      grantedUsersInput: editor.grantedUsersInput,
      grantedGroupsInput: editor.grantedGroupsInput,
    })

    if (result.ok) {
      detailQuery.replaceProblem(result.problem)
      setAccessMessageState({ errorMessage: '', successMessage: result.message })
    } else {
      setAccessMessageState({ errorMessage: result.message, successMessage: '' })
    }
  }

  async function deleteCurrentProblem() {
    const result = await deleteAction.deleteCurrentProblem()
    return result.ok
  }

  return {
    problem: detailQuery.problem,
    loadErrorMessage: detailQuery.errorMessage,
    isLoading: detailQuery.isLoading,
    isSaving: updateAction.isSaving,
    isDeleting: deleteAction.isDeleting,
    title: editor.title,
    statement: editor.statement,
    accessPolicy: modelAccessPolicy(editor.baseAccess, editor.grantedUsersInput, editor.grantedGroupsInput),
    baseAccess: editor.baseAccess,
    grantedUsersInput: editor.grantedUsersInput,
    grantedGroupsInput: editor.grantedGroupsInput,
    contentErrorMessage: contentMessageState.errorMessage,
    contentSuccessMessage: contentMessageState.successMessage,
    accessErrorMessage: accessMessageState.errorMessage,
    accessSuccessMessage: accessMessageState.successMessage,
    setTitle: editor.setTitle,
    setStatement: editor.setStatement,
    setBaseAccess: editor.setBaseAccess,
    setGrantedUsersInput: editor.setGrantedUsersInput,
    setGrantedGroupsInput: editor.setGrantedGroupsInput,
    saveContent,
    saveAccess,
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

function grantInputFromPolicy(
  accessPolicy: { viewerGrants: Array<{ kind: 'user'; username: string } | { kind: 'user_group'; slug: string }> },
  kind: 'user' | 'user_group',
): string {
  return accessPolicy.viewerGrants
    .filter((grant) => grant.kind === kind)
    .map((grant) => (grant.kind === 'user' ? grant.username : grant.slug))
    .join('\n')
}
