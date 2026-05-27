import type { ProblemTitleDisplayMode } from '@/objects/problem/ProblemTitleDisplayMode'
import type { UserDisplayMode } from '@/objects/user/UserDisplayMode'
import type { UserLocale } from '@/objects/user/UserLocale'

export type UserPreferences = {
  displayMode: UserDisplayMode
  locale: UserLocale
  problemTitleDisplayMode: ProblemTitleDisplayMode
  autoMarkMessageRead: boolean
}
