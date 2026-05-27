import { useReducer } from 'react'

import { problemSlugValue } from '@/objects/problem/ProblemSlug'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import type { ProblemSetSlug } from '@/objects/problemset/ProblemSetSlug'
import {
  emptyProblemSetSectionMessageState,
  reduceProblemSetSectionMessageState,
} from '../functions/problemset-detail-page-state'
import {
  buildProblemSetAccessUpdateDraft,
  buildProblemSetContentUpdateDraft,
  buildProblemSetDetailAccessPolicy,
} from '../functions/problemset-form'
import { useProblemSetDeleteAction } from './use-problemset-delete-action'
import { useProblemSetDetailQuery } from './use-problemset-detail-query'
import { useProblemSetEditorState } from './use-problemset-editor-state'
import { useProblemSetLinkProblemAction } from './use-problemset-link-problem-action'
import { useProblemSetRemoveProblemAction } from './use-problemset-remove-problem-action'
import { useProblemSetUpdateAction } from './use-problemset-update-action'
import { useI18n } from '@/system/i18n/use-i18n'

export function useProblemSetDetailPageModel(problemSetSlug: ProblemSetSlug, canManageProblems: boolean) {
  const { t } = useI18n()
  const detailQuery = useProblemSetDetailQuery(problemSetSlug)
  const editor = useProblemSetEditorState(detailQuery.problemSet)
  const updateAction = useProblemSetUpdateAction(problemSetSlug)
  const deleteAction = useProblemSetDeleteAction(problemSetSlug)
  const linkAction = useProblemSetLinkProblemAction(problemSetSlug)
  const removeAction = useProblemSetRemoveProblemAction(problemSetSlug)
  const [contentMessageState, dispatchContentMessage] = useReducer(
    reduceProblemSetSectionMessageState,
    emptyProblemSetSectionMessageState,
  )
  const [accessMessageState, dispatchAccessMessage] = useReducer(
    reduceProblemSetSectionMessageState,
    emptyProblemSetSectionMessageState,
  )
  const [linkMessageState, dispatchLinkMessage] = useReducer(
    reduceProblemSetSectionMessageState,
    emptyProblemSetSectionMessageState,
  )
  const [problemListMessageState, dispatchProblemListMessage] = useReducer(
    reduceProblemSetSectionMessageState,
    emptyProblemSetSectionMessageState,
  )

  async function saveContent() {
    if (!canManageProblems) {
      dispatchContentMessage({ type: 'set_error', message: t('problemSet.message.managerPermissionRequired') })
      return
    }

    const currentProblemSet = detailQuery.problemSet
    if (!currentProblemSet) {
      dispatchContentMessage({ type: 'set_error', message: t('problemSet.message.detailNotLoaded') })
      return
    }

    const result = await updateAction.save(buildProblemSetContentUpdateDraft(currentProblemSet, editor))

    if (result.ok) {
      detailQuery.replaceProblemSet(result.problemSet)
      dispatchContentMessage({ type: 'set_success', message: result.message })
    } else {
      dispatchContentMessage({ type: 'set_error', message: result.message })
    }
  }

  async function saveAccess() {
    if (!canManageProblems) {
      dispatchAccessMessage({ type: 'set_error', message: t('problemSet.message.managerPermissionRequired') })
      return
    }

    const currentProblemSet = detailQuery.problemSet
    if (!currentProblemSet) {
      dispatchAccessMessage({ type: 'set_error', message: t('problemSet.message.detailNotLoaded') })
      return
    }

    const result = await updateAction.save(buildProblemSetAccessUpdateDraft(currentProblemSet, editor))

    if (result.ok) {
      detailQuery.replaceProblemSet(result.problemSet)
      dispatchAccessMessage({ type: 'set_success', message: result.message })
    } else {
      dispatchAccessMessage({ type: 'set_error', message: result.message })
    }
  }

  async function attachProblem() {
    if (!canManageProblems) {
      dispatchLinkMessage({ type: 'set_error', message: t('problemSet.message.managerPermissionRequired') })
      return
    }

    const result = await linkAction.attachProblem(editor.linkProblemSlug)
    if (result.ok) {
      detailQuery.replaceProblemSet(result.problemSet)
      editor.clearLinkedProblemSlug()
      dispatchLinkMessage({ type: 'set_success', message: result.message })
    } else {
      dispatchLinkMessage({ type: 'set_error', message: result.message })
    }
  }

  async function removeProblem(problemSlug: ProblemSlug) {
    if (!canManageProblems) {
      dispatchProblemListMessage({ type: 'set_error', message: t('problemSet.message.managerPermissionRequired') })
      return
    }

    const result = await removeAction.removeProblem(problemSlug)
    if (result.ok) {
      detailQuery.replaceProblemSet(result.problemSet)
      dispatchProblemListMessage({
        type: 'set_success',
        message: t('problemSet.message.removeNamed', { slug: problemSlugValue(problemSlug) }),
      })
    } else {
      dispatchProblemListMessage({ type: 'set_error', message: result.message })
    }
  }

  async function deleteCurrentProblemSet() {
    if (!canManageProblems) {
      return false
    }

    const result = await deleteAction.deleteCurrentProblemSet()
    return result.ok
  }

  return {
    problemSet: detailQuery.problemSet,
    loadErrorMessage: detailQuery.errorMessage,
    isLoading: detailQuery.isLoading,
    activeLink: linkAction.activeLink,
    isSaving: updateAction.isSaving,
    isDeleting: deleteAction.isDeleting,
    activeRemovingProblemSlug: removeAction.activeRemovingProblemSlug,
    title: editor.title,
    description: editor.description,
    accessPolicy: buildProblemSetDetailAccessPolicy(editor),
    baseAccess: editor.baseAccess,
    grantedUsersInput: editor.grantedUsersInput,
    grantedGroupsInput: editor.grantedGroupsInput,
    linkProblemSlug: editor.linkProblemSlug,
    contentErrorMessage: contentMessageState.errorMessage,
    contentSuccessMessage: contentMessageState.successMessage,
    accessErrorMessage: accessMessageState.errorMessage,
    accessSuccessMessage: accessMessageState.successMessage,
    linkErrorMessage: linkMessageState.errorMessage,
    linkSuccessMessage: linkMessageState.successMessage,
    problemListErrorMessage: problemListMessageState.errorMessage,
    problemListSuccessMessage: problemListMessageState.successMessage,
    setTitle: editor.setTitle,
    setDescription: editor.setDescription,
    setBaseAccess: editor.setBaseAccess,
    setGrantedUsersInput: editor.setGrantedUsersInput,
    setGrantedGroupsInput: editor.setGrantedGroupsInput,
    setLinkProblemSlug: editor.setLinkProblemSlug,
    saveContent,
    saveAccess,
    removeProblem,
    deleteCurrentProblemSet,
    attachProblem,
  }
}
