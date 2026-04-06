import { useState } from 'react'

import { problemSlugValue, type ProblemSlug } from '@/features/problem/domain/problem'
import {
  problemSetDescriptionValue,
  problemSetTitleValue,
  type ProblemSetSlug,
} from '@/features/problemset/domain/problemset'
import { useProblemSetDeleteAction } from '@/features/problemset/hooks/use-problemset-delete-action'
import { useProblemSetDetailQuery } from '@/features/problemset/hooks/use-problemset-detail-query'
import { useProblemSetEditorState } from '@/features/problemset/hooks/use-problemset-editor-state'
import { useProblemSetLinkProblemAction } from '@/features/problemset/hooks/use-problemset-link-problem-action'
import { useProblemSetRemoveProblemAction } from '@/features/problemset/hooks/use-problemset-remove-problem-action'
import { useProblemSetUpdateAction } from '@/features/problemset/hooks/use-problemset-update-action'

type SectionMessageState = {
  errorMessage: string
  successMessage: string
}

const emptySectionMessageState: SectionMessageState = {
  errorMessage: '',
  successMessage: '',
}

export function useProblemSetDetailPageModel(problemSetSlug: ProblemSetSlug, canManageProblems: boolean) {
  const detailQuery = useProblemSetDetailQuery(problemSetSlug)
  const editor = useProblemSetEditorState(detailQuery.problemSet)
  const updateAction = useProblemSetUpdateAction(problemSetSlug)
  const deleteAction = useProblemSetDeleteAction(problemSetSlug)
  const linkAction = useProblemSetLinkProblemAction(problemSetSlug)
  const removeAction = useProblemSetRemoveProblemAction(problemSetSlug)
  const [contentMessageState, setContentMessageState] = useState<SectionMessageState>(emptySectionMessageState)
  const [accessMessageState, setAccessMessageState] = useState<SectionMessageState>(emptySectionMessageState)
  const [linkMessageState, setLinkMessageState] = useState<SectionMessageState>(emptySectionMessageState)
  const [problemListMessageState, setProblemListMessageState] = useState<SectionMessageState>(emptySectionMessageState)

  async function saveContent() {
    if (!canManageProblems) {
      setContentMessageState({ errorMessage: 'Problem manager permission required.', successMessage: '' })
      return
    }

    const currentProblemSet = detailQuery.problemSet
    if (!currentProblemSet) {
      setContentMessageState({ errorMessage: 'Problem set detail is not loaded.', successMessage: '' })
      return
    }

    const result = await updateAction.save({
      title: editor.title,
      description: editor.description,
      baseAccess: currentProblemSet.accessPolicy.baseAccess,
      grantedUsersInput: grantInputFromPolicy(currentProblemSet.accessPolicy, 'user'),
      grantedGroupsInput: grantInputFromPolicy(currentProblemSet.accessPolicy, 'user_group'),
    })

    if (result.ok) {
      detailQuery.replaceProblemSet(result.problemSet)
      setContentMessageState({ errorMessage: '', successMessage: result.message })
    } else {
      setContentMessageState({ errorMessage: result.message, successMessage: '' })
    }
  }

  async function saveAccess() {
    if (!canManageProblems) {
      setAccessMessageState({ errorMessage: 'Problem manager permission required.', successMessage: '' })
      return
    }

    const currentProblemSet = detailQuery.problemSet
    if (!currentProblemSet) {
      setAccessMessageState({ errorMessage: 'Problem set detail is not loaded.', successMessage: '' })
      return
    }

    const result = await updateAction.save({
      title: problemSetTitleValue(currentProblemSet.title),
      description: problemSetDescriptionValue(currentProblemSet.description),
      baseAccess: editor.baseAccess,
      grantedUsersInput: editor.grantedUsersInput,
      grantedGroupsInput: editor.grantedGroupsInput,
    })

    if (result.ok) {
      detailQuery.replaceProblemSet(result.problemSet)
      setAccessMessageState({ errorMessage: '', successMessage: result.message })
    } else {
      setAccessMessageState({ errorMessage: result.message, successMessage: '' })
    }
  }

  async function attachProblem() {
    if (!canManageProblems) {
      setLinkMessageState({ errorMessage: 'Problem manager permission required.', successMessage: '' })
      return
    }

    const result = await linkAction.attachProblem(editor.linkProblemSlug)
    if (result.ok) {
      detailQuery.replaceProblemSet(result.problemSet)
      editor.clearLinkedProblemSlug()
      setLinkMessageState({ errorMessage: '', successMessage: result.message })
    } else {
      setLinkMessageState({ errorMessage: result.message, successMessage: '' })
    }
  }

  async function removeProblem(problemSlug: ProblemSlug) {
    if (!canManageProblems) {
      setProblemListMessageState({ errorMessage: 'Problem manager permission required.', successMessage: '' })
      return
    }

    const result = await removeAction.removeProblem(problemSlug)
    if (result.ok) {
      detailQuery.replaceProblemSet(result.problemSet)
      setProblemListMessageState({
        errorMessage: '',
        successMessage: `Removed ${problemSlugValue(problemSlug)} from the problem set.`,
      })
    } else {
      setProblemListMessageState({ errorMessage: result.message, successMessage: '' })
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
    accessPolicy: modelAccessPolicy(editor.baseAccess, editor.grantedUsersInput, editor.grantedGroupsInput),
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
