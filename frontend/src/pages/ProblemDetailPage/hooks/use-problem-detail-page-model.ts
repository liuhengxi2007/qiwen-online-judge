import { useReducer } from 'react'

import {
  emptySectionMessageState,
  reduceSectionMessageState,
} from '../functions/problem-detail-page-state'
import {
  buildProblemAccessUpdateDraft,
  buildProblemContentUpdateDraft,
  buildProblemDetailAccessPolicy,
} from '@/objects/problem/problem-form'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import { useProblemDeleteAction } from './use-problem-delete-action'
import { useProblemDetailQuery } from '@/pages/hooks/use-problem-detail-query'
import { useProblemEditorState } from './use-problem-editor-state'
import { useProblemUpdateAction } from './use-problem-update-action'

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
      ...buildProblemContentUpdateDraft(currentProblem, editor),
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
      ...buildProblemAccessUpdateDraft(currentProblem, editor),
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
    accessPolicy: buildProblemDetailAccessPolicy(editor),
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
