import { useCallback, useReducer } from 'react'

import { createUserGroup } from '@/features/usergroup/api/usergroup-client'
import {
  initialCreateUserGroupPageState,
  reduceCreateUserGroupPageState,
} from '@/features/usergroup/domain/create-usergroup-page-state'
import { validateUserGroupDraft } from '@/features/usergroup/domain/usergroup-form'
import { HttpClientError } from '@/shared/api/http-client'
import { useI18n } from '@/shared/i18n/i18n'

export function useCreateUserGroupPageModel() {
  const { t } = useI18n()
  const [state, dispatch] = useReducer(reduceCreateUserGroupPageState, initialCreateUserGroupPageState)

  const submit = useCallback(async () => {
    const validation = validateUserGroupDraft({
      slug: state.slug,
      name: state.name,
      description: state.description,
    })
    if (!validation.ok) {
      dispatch({ type: 'submit_failed', message: validation.message })
      return
    }

    dispatch({ type: 'submit_started' })

    try {
      await createUserGroup(validation.request)
      dispatch({ type: 'submit_succeeded', message: t('userGroup.message.createSuccess') })
    } catch (error) {
      const message = error instanceof HttpClientError ? error.message : t('userGroup.message.createFailed')
      dispatch({ type: 'submit_failed', message })
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
