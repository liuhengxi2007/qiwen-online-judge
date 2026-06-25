import { useCallback, useEffect, useReducer, useState } from 'react'

import { GetContest } from '@/apis/contest/GetContest'
import { UpdateContest } from '@/apis/contest/UpdateContest'
import type { ContestSlug } from '@/objects/contest/ContestSlug'
import type { ContestDetail } from '@/objects/contest/response/ContestDetail'
import type { BaseAccess } from '@/objects/shared/access/BaseAccess'
import { createRestrictedAccessPolicy } from '@/objects/shared/access/ResourceAccessPolicy'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import { buildResourceAccessPolicy } from '@/pages/components/ResourceAccessEditorInput'
import { sendAPI } from '@/system/api/api-message'
import { isHttpClientError } from '@/system/api/http-client'
import { useI18n } from '@/system/i18n/use-i18n'
import type { ContestManageDraft } from '../functions/ContestManageForm'
import { contestManageDraftFromDetail, validateContestManageDraft } from '../functions/ContestManageForm'
import { useContestProblemWorkflow } from './useContestProblemWorkflow'

/**
 * 比赛管理页状态，保存详情、表单草稿、题目操作状态和反馈消息。
 */
type ContestManagePageState = {
  contest: ContestDetail | null
  draft: ContestManageDraft | null
  isLoading: boolean
  isSaving: boolean
  isAttachingProblem: boolean
  attachWarningProblemSlug: ProblemSlug | null
  removingProblemSlug: string
  loadErrorMessage: string
  saveErrorMessage: string
  saveSuccessMessage: string
  problemErrorMessage: string
  problemSuccessMessage: string
}

/**
 * 比赛管理页动作集合，覆盖加载、表单编辑、保存和题目关联操作。
 */
export type ContestManagePageAction =
  | { type: 'load_started' }
  | { type: 'load_succeeded'; contest: ContestDetail }
  | { type: 'load_failed'; message: string }
  | { type: 'set_title'; value: string }
  | { type: 'set_description'; value: string }
  | { type: 'set_start_at'; value: string }
  | { type: 'set_end_at'; value: string }
  | { type: 'set_base_access'; value: BaseAccess }
  | { type: 'set_granted_users_input'; value: string }
  | { type: 'set_granted_groups_input'; value: string }
  | { type: 'set_granted_manager_users_input'; value: string }
  | { type: 'set_granted_manager_groups_input'; value: string }
  | { type: 'save_started' }
  | { type: 'save_succeeded'; contest: ContestDetail; message: string }
  | { type: 'save_failed'; message: string }
  | { type: 'attach_problem_started' }
  | { type: 'attach_problem_warning_opened'; problemSlug: ProblemSlug }
  | { type: 'attach_problem_warning_closed' }
  | { type: 'attach_problem_succeeded'; contest: ContestDetail; message: string }
  | { type: 'attach_problem_failed'; message: string }
  | { type: 'remove_problem_started'; problemSlug: string }
  | { type: 'remove_problem_succeeded'; contest: ContestDetail; message: string }
  | { type: 'remove_problem_failed'; message: string }

const initialState: ContestManagePageState = {
  contest: null,
  draft: null,
  isLoading: true,
  isSaving: false,
  isAttachingProblem: false,
  attachWarningProblemSlug: null,
  removingProblemSlug: '',
  loadErrorMessage: '',
  saveErrorMessage: '',
  saveSuccessMessage: '',
  problemErrorMessage: '',
  problemSuccessMessage: '',
}

/**
 * 比赛管理页 reducer；纯函数维护管理状态、草稿和题目操作反馈。
 */
function reducer(state: ContestManagePageState, action: ContestManagePageAction): ContestManagePageState {
  switch (action.type) {
    case 'load_started':
      return { ...state, isLoading: true, loadErrorMessage: '' }
    case 'load_succeeded':
      return { ...state, contest: action.contest, draft: contestManageDraftFromDetail(action.contest), isLoading: false, loadErrorMessage: '' }
    case 'load_failed':
      return { ...state, isLoading: false, loadErrorMessage: action.message }
    case 'set_title':
      return state.draft ? { ...state, draft: { ...state.draft, title: action.value } } : state
    case 'set_description':
      return state.draft ? { ...state, draft: { ...state.draft, description: action.value } } : state
    case 'set_start_at':
      return state.draft ? { ...state, draft: { ...state.draft, startAt: action.value } } : state
    case 'set_end_at':
      return state.draft ? { ...state, draft: { ...state.draft, endAt: action.value } } : state
    case 'set_base_access':
      return state.draft ? { ...state, draft: { ...state.draft, baseAccess: action.value } } : state
    case 'set_granted_users_input':
      return state.draft ? { ...state, draft: { ...state.draft, grantedUsersInput: action.value } } : state
    case 'set_granted_groups_input':
      return state.draft ? { ...state, draft: { ...state.draft, grantedGroupsInput: action.value } } : state
    case 'set_granted_manager_users_input':
      return state.draft ? { ...state, draft: { ...state.draft, grantedManagerUsersInput: action.value } } : state
    case 'set_granted_manager_groups_input':
      return state.draft ? { ...state, draft: { ...state.draft, grantedManagerGroupsInput: action.value } } : state
    case 'save_started':
      return { ...state, isSaving: true, saveErrorMessage: '', saveSuccessMessage: '' }
    case 'save_succeeded':
      return { ...state, contest: action.contest, draft: contestManageDraftFromDetail(action.contest), isSaving: false, saveErrorMessage: '', saveSuccessMessage: action.message }
    case 'save_failed':
      return { ...state, isSaving: false, saveErrorMessage: action.message, saveSuccessMessage: '' }
    case 'attach_problem_started':
      return { ...state, isAttachingProblem: true, problemErrorMessage: '', problemSuccessMessage: '' }
    case 'attach_problem_warning_opened':
      return { ...state, isAttachingProblem: false, attachWarningProblemSlug: action.problemSlug, problemErrorMessage: '', problemSuccessMessage: '' }
    case 'attach_problem_warning_closed':
      return { ...state, attachWarningProblemSlug: null }
    case 'attach_problem_succeeded':
      return { ...state, contest: action.contest, isAttachingProblem: false, attachWarningProblemSlug: null, problemErrorMessage: '', problemSuccessMessage: action.message }
    case 'attach_problem_failed':
      return { ...state, isAttachingProblem: false, attachWarningProblemSlug: null, problemErrorMessage: action.message, problemSuccessMessage: '' }
    case 'remove_problem_started':
      return { ...state, removingProblemSlug: action.problemSlug, problemErrorMessage: '', problemSuccessMessage: '' }
    case 'remove_problem_succeeded':
      return { ...state, contest: action.contest, removingProblemSlug: '', problemErrorMessage: '', problemSuccessMessage: action.message }
    case 'remove_problem_failed':
      return { ...state, removingProblemSlug: '', problemErrorMessage: action.message, problemSuccessMessage: '' }
  }
}

/**
 * 比赛管理页模型 hook；加载比赛详情并提供保存基础信息的动作。
 */
export function useContestManagePageModel(contestSlug: ContestSlug) {
  const { t } = useI18n()
  const [state, dispatch] = useReducer(reducer, initialState)
  const [descriptionTab, setDescriptionTab] = useState<'write' | 'preview'>('write')
  const problemWorkflow = useContestProblemWorkflow({
    attachWarningProblemSlug: state.attachWarningProblemSlug,
    contestSlug,
    dispatch,
  })

  useEffect(() => {
    let cancelled = false
    dispatch({ type: 'load_started' })
    void sendAPI(new GetContest(contestSlug))
      .then((contest) => {
        if (!cancelled) {
          dispatch({ type: 'load_succeeded', contest })
        }
      })
      .catch(() => {
        if (!cancelled) {
          dispatch({ type: 'load_failed', message: t('contest.manage.loadFailed') })
        }
      })

    return () => {
      cancelled = true
    }
  }, [contestSlug, t])

  const accessPolicyResult = state.draft
    ? buildResourceAccessPolicy({
      baseAccess: state.draft.baseAccess,
      viewer: {
        usersInput: state.draft.grantedUsersInput,
        groupsInput: state.draft.grantedGroupsInput,
      },
      manager: {
        usersInput: state.draft.grantedManagerUsersInput,
        groupsInput: state.draft.grantedManagerGroupsInput,
      },
    })
    : { ok: true as const, value: createRestrictedAccessPolicy() }

  const save = useCallback(async () => {
    if (!state.draft) {
      return
    }

    const validation = validateContestManageDraft(state.draft)
    if (!validation.ok) {
      dispatch({ type: 'save_failed', message: validation.message })
      return
    }

    dispatch({ type: 'save_started' })
    try {
      const contest = await sendAPI(new UpdateContest(contestSlug, validation.request))
      dispatch({ type: 'save_succeeded', contest, message: t('contest.manage.saveSuccess') })
    } catch (error) {
      const message = isHttpClientError(error) ? error.message : t('contest.manage.saveFailed')
      dispatch({ type: 'save_failed', message })
    }
  }, [contestSlug, state.draft, t])

  return {
    ...state,
    descriptionTab,
    ...problemWorkflow,
    accessPolicy: accessPolicyResult.ok ? accessPolicyResult.value : createRestrictedAccessPolicy(),
    isAttachWarningOpen: state.attachWarningProblemSlug !== null,
    setDescriptionTab,
    setTitle: (value: string) => dispatch({ type: 'set_title', value }),
    setDescription: (value: string) => dispatch({ type: 'set_description', value }),
    setStartAt: (value: string) => dispatch({ type: 'set_start_at', value }),
    setEndAt: (value: string) => dispatch({ type: 'set_end_at', value }),
    setBaseAccess: (value: BaseAccess) => dispatch({ type: 'set_base_access', value }),
    setGrantedUsersInput: (value: string) => dispatch({ type: 'set_granted_users_input', value }),
    setGrantedGroupsInput: (value: string) => dispatch({ type: 'set_granted_groups_input', value }),
    setGrantedManagerUsersInput: (value: string) => dispatch({ type: 'set_granted_manager_users_input', value }),
    setGrantedManagerGroupsInput: (value: string) => dispatch({ type: 'set_granted_manager_groups_input', value }),
    save,
  }
}
