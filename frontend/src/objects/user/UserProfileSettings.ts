import type { ProblemTitleDisplayMode } from '@/objects/problem/ProblemTitleDisplayMode'
import { fromProblemTitleDisplayModeContract } from '@/objects/problem/ProblemTitleDisplayMode'
import type { DisplayName } from '@/objects/user/DisplayName'
import { fromDisplayNameContract } from '@/objects/user/DisplayName'
import type { UserDisplayMode } from '@/objects/user/UserDisplayMode'
import { fromUserDisplayModeContract } from '@/objects/user/UserDisplayMode'
import type { UserLocale } from '@/objects/user/UserLocale'
import { fromUserLocaleContract } from '@/objects/user/UserLocale'
import type { Username } from '@/objects/user/Username'
import { fromUsernameContract } from '@/objects/user/Username'
import { readBoolean, readRecord, readString } from '@/objects/shared/PageResponse'

export type UserProfileSettings = {
  username: Username
  displayName: DisplayName
  displayMode: UserDisplayMode
  locale: UserLocale
  problemTitleDisplayMode: ProblemTitleDisplayMode
  autoMarkMessageRead: boolean
}

export function fromUserProfileSettingsContract(value: unknown, label = 'user profile settings'): UserProfileSettings {
  const settings = readRecord(value, label)
  return {
    username: fromUsernameContract(readString(settings.username, `${label} username`), `${label} username`),
    displayName: fromDisplayNameContract(readString(settings.displayName, `${label} display name`), `${label} display name`),
    displayMode: fromUserDisplayModeContract(readString(settings.displayMode, `${label} display mode`), `${label} display mode`),
    locale: fromUserLocaleContract(readString(settings.locale, `${label} locale`), `${label} locale`),
    problemTitleDisplayMode: fromProblemTitleDisplayModeContract(
      readString(settings.problemTitleDisplayMode, `${label} problem title display mode`),
      `${label} problem title display mode`,
    ),
    autoMarkMessageRead: readBoolean(settings.autoMarkMessageRead, `${label} auto mark message read`),
  }
}
