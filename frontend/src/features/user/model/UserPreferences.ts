import type { ProblemTitleDisplayMode } from '@/features/problem/model/ProblemTitleDisplayMode'
import type { UserDisplayMode } from '@/features/user/model/UserDisplayMode'
import type { UserLocale } from '@/features/user/model/UserLocale'

export type UserPreferences = {
  displayMode: UserDisplayMode
  locale: UserLocale
  problemTitleDisplayMode: ProblemTitleDisplayMode
}
