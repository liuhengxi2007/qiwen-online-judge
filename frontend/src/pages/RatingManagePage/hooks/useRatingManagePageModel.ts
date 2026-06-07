import { useCallback, useEffect, useReducer } from 'react'

import { AppendRatingContest } from '@/apis/rating/AppendRatingContest'
import { GetRatingManageState } from '@/apis/rating/GetRatingManageState'
import { PopRatingContest } from '@/apis/rating/PopRatingContest'
import { sendAPI } from '@/system/api/api-message'
import { HttpClientError } from '@/system/api/http-client'
import { useI18n } from '@/system/i18n/use-i18n'

import {
  initialRatingManagePageState,
  ratingManagePageReducer,
  validateRatingAppendDraft,
} from '../functions/RatingManagePageState'

export function useRatingManagePageModel(canManage: boolean) {
  const { t } = useI18n()
  const [state, dispatch] = useReducer(ratingManagePageReducer, initialRatingManagePageState)

  useEffect(() => {
    if (!canManage) {
      return
    }

    let isCancelled = false
    dispatch({ type: 'load_started' })

    void sendAPI(new GetRatingManageState())
      .then((response) => {
        if (!isCancelled) {
          dispatch({ type: 'load_succeeded', state: response })
        }
      })
      .catch((error) => {
        if (isCancelled) {
          return
        }

        const message = error instanceof HttpClientError ? error.message : t('ratingManage.loadFailed')
        dispatch({ type: 'load_failed', message })
      })

    return () => {
      isCancelled = true
    }
  }, [canManage, t])

  const appendContest = useCallback(async () => {
    if (!canManage) {
      dispatch({ type: 'append_failed', message: t('ratingManage.permissionRequired') })
      return
    }

    const validation = validateRatingAppendDraft(state.draft)
    if (!validation.ok) {
      dispatch({ type: 'append_failed', message: validation.message })
      return
    }

    dispatch({ type: 'append_started' })

    try {
      const response = await sendAPI(new AppendRatingContest(validation.request))
      dispatch({ type: 'append_succeeded', state: response, message: t('ratingManage.appendSuccess') })
    } catch (error) {
      const message = error instanceof HttpClientError ? error.message : t('ratingManage.appendFailed')
      dispatch({ type: 'append_failed', message })
    }
  }, [canManage, state.draft, t])

  const popContest = useCallback(async () => {
    if (!canManage) {
      dispatch({ type: 'pop_failed', message: t('ratingManage.permissionRequired') })
      return
    }

    dispatch({ type: 'pop_started' })

    try {
      const response = await sendAPI(new PopRatingContest())
      dispatch({ type: 'pop_succeeded', state: response, message: t('ratingManage.popSuccess') })
    } catch (error) {
      const message = error instanceof HttpClientError ? error.message : t('ratingManage.popFailed')
      dispatch({ type: 'pop_failed', message })
    }
  }, [canManage, t])

  return {
    ...state,
    appendContest,
    popContest,
    setContestSlugInput: (value: string) => dispatch({ type: 'set_contest_slug_input', value }),
    setMInput: (value: string) => dispatch({ type: 'set_m_input', value }),
  }
}
