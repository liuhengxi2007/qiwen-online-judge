import { useState } from 'react'

import type { ProblemSlug } from '@/features/problem/domain/problem'
import { problemStatementTextValue, problemTitleValue } from '@/features/problem/domain/problem'
import { useProblemDeleteAction } from '@/features/problem/hooks/use-problem-delete-action'
import { useProblemDetailQuery } from '@/features/problem/hooks/use-problem-detail-query'
import { useProblemEditorState } from '@/features/problem/hooks/use-problem-editor-state'
import { useProblemUpdateAction } from '@/features/problem/hooks/use-problem-update-action'
import {
  buildResourceAccessPolicy,
  grantedGroupsInputFromAccessPolicy,
  grantedManagerGroupsInputFromAccessPolicy,
  grantedManagerUsersInputFromAccessPolicy,
  grantedUsersInputFromAccessPolicy,
} from '@/shared/domain/resource-access-input'

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
      timeLimitMs: editor.timeLimitMs,
      spaceLimitMb: editor.spaceLimitMb,
      baseAccess: currentProblem.accessPolicy.baseAccess,
      grantedUsersInput: grantedUsersInputFromAccessPolicy(currentProblem.accessPolicy),
      grantedGroupsInput: grantedGroupsInputFromAccessPolicy(currentProblem.accessPolicy),
      managerUsersInput: grantedManagerUsersInputFromAccessPolicy(currentProblem.accessPolicy),
      managerGroupsInput: grantedManagerGroupsInputFromAccessPolicy(currentProblem.accessPolicy),
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
      timeLimitMs: currentProblem.timeLimitMs,
      spaceLimitMb: currentProblem.spaceLimitMb,
      baseAccess: editor.baseAccess,
      grantedUsersInput: editor.grantedUsersInput,
      grantedGroupsInput: editor.grantedGroupsInput,
      managerUsersInput: editor.managerUsersInput,
      managerGroupsInput: editor.managerGroupsInput,
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

  const accessPolicyResult = buildResourceAccessPolicy(
    editor.baseAccess,
    editor.grantedUsersInput,
    editor.grantedGroupsInput,
    editor.managerUsersInput,
    editor.managerGroupsInput,
  )

  return {
    problem: detailQuery.problem,
    loadErrorMessage: detailQuery.errorMessage,
    isLoading: detailQuery.isLoading,
    isSaving: updateAction.isSaving,
    isDeleting: deleteAction.isDeleting,
    title: editor.title,
    statement: editor.statement,
    timeLimitMs: editor.timeLimitMs,
    spaceLimitMb: editor.spaceLimitMb,
    accessPolicy: accessPolicyResult.ok
      ? accessPolicyResult.value
      : { baseAccess: editor.baseAccess, viewerGrants: [], managerGrants: [] },
    canManage: detailQuery.problem?.canManage ?? false,
    baseAccess: editor.baseAccess,
    grantedUsersInput: editor.grantedUsersInput,
    grantedGroupsInput: editor.grantedGroupsInput,
    managerUsersInput: editor.managerUsersInput,
    managerGroupsInput: editor.managerGroupsInput,
    contentErrorMessage: contentMessageState.errorMessage,
    contentSuccessMessage: contentMessageState.successMessage,
    accessErrorMessage: accessMessageState.errorMessage,
    accessSuccessMessage: accessMessageState.successMessage,
    setTitle: editor.setTitle,
    setStatement: editor.setStatement,
    setTimeLimitMs: editor.setTimeLimitMs,
    setSpaceLimitMb: editor.setSpaceLimitMb,
    setBaseAccess: editor.setBaseAccess,
    setGrantedUsersInput: editor.setGrantedUsersInput,
    setGrantedGroupsInput: editor.setGrantedGroupsInput,
    setManagerUsersInput: editor.setManagerUsersInput,
    setManagerGroupsInput: editor.setManagerGroupsInput,
    saveContent,
    saveAccess,
    deleteCurrentProblem,
  }
}
