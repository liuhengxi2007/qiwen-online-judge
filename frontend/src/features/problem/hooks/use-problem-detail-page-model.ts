import { useReducer } from 'react'

import {
  emptySectionMessageState,
  reduceSectionMessageState,
} from '@/features/problem/domain/problem-detail-page-state'
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

export function useProblemDetailPageModel(problemSlug: ProblemSlug) {
  const detailQuery = useProblemDetailQuery(problemSlug)
  const editor = useProblemEditorState(detailQuery.problem)
  const updateAction = useProblemUpdateAction(problemSlug)
  const deleteAction = useProblemDeleteAction(problemSlug)
  const [contentMessageState, dispatchContentMessage] = useReducer(reduceSectionMessageState, emptySectionMessageState)
  const [accessMessageState, dispatchAccessMessage] = useReducer(reduceSectionMessageState, emptySectionMessageState)

  async function saveContent() {
    const currentProblem = detailQuery.problem
    if (!currentProblem) {
      dispatchContentMessage({ type: 'set_error', message: 'Problem detail is not loaded.' })
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
      othersSubmissionAccess: currentProblem.othersSubmissionAccess,
    })

    if (result.ok) {
      detailQuery.replaceProblem(result.problem)
      dispatchContentMessage({ type: 'set_success', message: result.message })
    } else {
      dispatchContentMessage({ type: 'set_error', message: result.message })
    }
  }

  async function saveAccess() {
    const currentProblem = detailQuery.problem
    if (!currentProblem) {
      dispatchAccessMessage({ type: 'set_error', message: 'Problem detail is not loaded.' })
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
      othersSubmissionAccess: editor.othersSubmissionAccess,
    })

    if (result.ok) {
      detailQuery.replaceProblem(result.problem)
      dispatchAccessMessage({ type: 'set_success', message: result.message })
    } else {
      dispatchAccessMessage({ type: 'set_error', message: result.message })
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
    othersSubmissionAccess: editor.othersSubmissionAccess,
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
    setOthersSubmissionAccess: editor.setOthersSubmissionAccess,
    saveContent,
    saveAccess,
    deleteCurrentProblem,
  }
}
