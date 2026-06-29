import { useEffect, useReducer } from 'react'

import { GetUserGroup } from '@/apis/usergroup/GetUserGroup'
import type { UserGroupDetail } from '@/objects/usergroup/response/UserGroupDetail'
import type { UserGroupSlug } from '@/objects/usergroup/UserGroupSlug'
import { sendAPI } from '@/system/api/api-message'
import { isHttpClientError } from '@/system/api/http-client'
import { translateMessage } from '@/system/i18n/messages'

/**
 * 用户组详情查询状态，包含详情快照、加载标记和本地化错误消息。
 */
type UserGroupDetailQueryState = {
  userGroup: UserGroupDetail | null
  isLoading: boolean
  errorMessage: string
}

/**
 * 用户组详情查询 reducer 动作，支持初始加载、失败和写操作后的详情替换。
 */
type UserGroupDetailQueryAction =
  | { type: 'load_started' }
  | { type: 'load_succeeded'; userGroup: UserGroupDetail }
  | { type: 'replace'; userGroup: UserGroupDetail }
  | { type: 'load_failed'; message: string }

const initialUserGroupDetailQueryState: UserGroupDetailQueryState = {
  userGroup: null,
  isLoading: true,
  errorMessage: '',
}

/**
 * 用户组详情查询 reducer，保持查询状态和替换状态拥有一致的加载/错误语义。
 */
function reduceUserGroupDetailQueryState(
  state: UserGroupDetailQueryState,
  action: UserGroupDetailQueryAction,
): UserGroupDetailQueryState {
  switch (action.type) {
    case 'load_started':
      return { ...state, isLoading: true, errorMessage: '' }
    case 'load_succeeded':
      return { userGroup: action.userGroup, isLoading: false, errorMessage: '' }
    case 'replace':
      return { userGroup: action.userGroup, isLoading: false, errorMessage: '' }
    case 'load_failed':
      return { userGroup: null, isLoading: false, errorMessage: action.message }
  }
}

/**
 * 用户组详情查询 hook，按 slug 拉取详情并把 403/404 映射为不暴露资源状态的通用未找到消息。
 */
export function useUserGroupDetailQuery(userGroupSlug: UserGroupSlug) {
  const [state, dispatch] = useReducer(reduceUserGroupDetailQueryState, initialUserGroupDetailQueryState)

  useEffect(() => {
    let cancelled = false
    dispatch({ type: 'load_started' })
    void sendAPI(new GetUserGroup(userGroupSlug))
      .then((userGroup) => {
        if (cancelled) {
          return
        }
        dispatch({ type: 'load_succeeded', userGroup })
      })
      .catch((error: unknown) => {
        if (cancelled) {
          return
        }
        dispatch({
          type: 'load_failed',
          message:
            isHttpClientError(error) && (error.kind === 'not-found' || error.kind === 'forbidden')
              ? translateMessage('common.error.notFound')
              : translateMessage('userGroup.detailLoadFailed'),
        })
      })

    return () => {
      cancelled = true
    }
  }, [userGroupSlug])

  function replaceUserGroup(userGroup: UserGroupDetail) {
    dispatch({ type: 'replace', userGroup })
  }

  return {
    ...state,
    replaceUserGroup,
  }
}
