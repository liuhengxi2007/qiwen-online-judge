import type { UserDisplayMode } from '@/features/auth/model/UserDisplayMode'
import type { UserLocale } from '@/features/auth/model/UserLocale'
import type { ProblemTitleDisplayMode } from '@/features/problem/model/ProblemTitleDisplayMode'

export type UserPreferences = {
  displayMode: UserDisplayMode
  locale: UserLocale
  problemTitleDisplayMode: ProblemTitleDisplayMode
}
