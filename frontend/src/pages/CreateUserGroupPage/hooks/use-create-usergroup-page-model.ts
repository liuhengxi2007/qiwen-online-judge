import { useCallback, useReducer } from 'react'

import { createUserGroup } from '@/apis/usergroup/CreateUserGroup'
import type { UserGroupDetail } from '@/objects/usergroup/response/UserGroupDetail'
import {
  initialCreateUserGroupPageState,
  reduceCreateUserGroupPageState,
} from '../functions/create-usergroup-page-state'
import { validateUserGroupDraft } from '@/objects/usergroup/usergroup-form'
import { HttpClientError } from '@/system/api/http-client'
import { useI18n } from '@/system/i18n/use-i18n'

export function useCreateUserGroupPageModel() {
  const { t } = useI18n()
  const [state, dispatch] = useReducer(reduceCreateUserGroupPageState, initialCreateUserGroupPageState)

  const submit = useCallback(async (): Promise<UserGroupDetail | null> => {
    const validation = validateUserGroupDraft({
      slug: state.slug,
      name: state.name,
      description: state.description,
    })
    if (!validation.ok) {
      dispatch({ type: 'submit_failed', message: validation.message })
      return null
    }

    dispatch({ type: 'submit_started' })

    try {
      const createdGroup = await createUserGroup(validation.request)
      dispatch({ type: 'submit_succeeded', message: t('userGroup.message.createSuccess') })
      return createdGroup
    } catch (error) {
      const message = error instanceof HttpClientError ? error.message : t('userGroup.message.createFailed')
      dispatch({ type: 'submit_failed', message })
      return null
    }
  }, [state.description, state.name, state.slug, t])

  return {
    ...state,
    setSlug: (value: string) => dispatch({ type: 'set_slug', value }),
    setName: (value: string) => dispatch({ type: 'set_name', value }),
    setDescription: (value: string) => dispatch({ type: 'set_description', value }),
    submit,
  }
}
