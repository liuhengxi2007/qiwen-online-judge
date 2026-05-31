import { useReducer } from 'react'

import {
  emptySectionMessageState,
  reduceSectionMessageState,
} from '../functions/ProblemDetailPageState'
import {
  buildProblemAccessUpdateDraft,
  buildProblemContentUpdateDraft,
  buildProblemDetailAccessPolicy,
} from '../functions/ProblemForm'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import { useProblemDeleteAction } from './useProblemDeleteAction'
import { useProblemDetailQuery } from '@/pages/hooks/useProblemDetailQuery'
import { useProblemEditorState } from './useProblemEditorState'
import { useProblemUpdateAction } from './useProblemUpdateAction'

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
    authorUsername: editor.authorUsername,
    accessPolicy: buildProblemDetailAccessPolicy(editor),
    canManage: detailQuery.problem?.canManage ?? false,
    baseAccess: editor.baseAccess,
    grantedUsersInput: editor.grantedUsersInput,
    grantedGroupsInput: editor.grantedGroupsInput,
    managerUsersInput: editor.managerUsersInput,
    managerGroupsInput: editor.managerGroupsInput,
    otherUserSubmissionAccess: editor.otherUserSubmissionAccess,
    contentErrorMessage: contentMessageState.errorMessage,
    contentSuccessMessage: contentMessageState.successMessage,
    accessErrorMessage: accessMessageState.errorMessage,
    accessSuccessMessage: accessMessageState.successMessage,
    setTitle: editor.setTitle,
    setStatement: editor.setStatement,
    setAuthorUsername: editor.setAuthorUsername,
    setBaseAccess: editor.setBaseAccess,
    setGrantedUsersInput: editor.setGrantedUsersInput,
    setGrantedGroupsInput: editor.setGrantedGroupsInput,
    setManagerUsersInput: editor.setManagerUsersInput,
    setManagerGroupsInput: editor.setManagerGroupsInput,
    setOtherUserSubmissionAccess: editor.setOtherUserSubmissionAccess,
    saveContent,
    saveAccess,
    deleteCurrentProblem,
  }
}
