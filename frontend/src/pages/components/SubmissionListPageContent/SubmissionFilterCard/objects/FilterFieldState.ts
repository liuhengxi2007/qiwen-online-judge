import type { KeyboardEvent } from 'react'

import type { SubmissionPageModel } from '@/pages/hooks/submission/useSubmissionPageModel'

export type TextFilterInputState = {
  value: string
  onValueChange: (value: string) => void
  onFocusChange: (focused: boolean) => void
  onEnter: (event: KeyboardEvent<HTMLInputElement>) => void
}

export type UserSuggestionState = {
  enabled: boolean
  isLoading: boolean
  isOpen: boolean
  items: SubmissionPageModel['userSuggestions']
  onEnabledChange: (enabled: boolean) => void
  onSelect: (username: string) => void
}

export type ProblemSuggestionState = {
  enabled: boolean
  isLoading: boolean
  isOpen: boolean
  items: SubmissionPageModel['problemSuggestions']
  onEnabledChange: (enabled: boolean) => void
  onSelect: (slug: string) => void
}
