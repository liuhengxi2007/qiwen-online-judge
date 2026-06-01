import type { APIMessage } from '@/system/api/api-message'
import type { ProblemTitleDisplayMode } from '@/objects/problem/ProblemTitleDisplayMode'
import type { DisplayName } from '@/objects/user/DisplayName'
import type { UserDisplayMode } from '@/objects/user/UserDisplayMode'
import type { UserLocale } from '@/objects/user/UserLocale'
import type { Username } from '@/objects/user/Username'
import type { UserProfileSettings } from '@/objects/user/UserProfileSettings'
import { fromUserProfileSettingsContract } from '@/objects/user/UserProfileSettings'

type CreateUserProfileSettingsBody = {
  username: Username
  displayName: DisplayName
  displayMode: UserDisplayMode
  locale: UserLocale
  problemTitleDisplayMode: ProblemTitleDisplayMode
  autoMarkMessageRead: boolean
}

export class CreateUserProfileSettings implements APIMessage<UserProfileSettings> {
  declare readonly responseType?: UserProfileSettings
  readonly method = 'POST'
  readonly decode = fromUserProfileSettingsContract
  readonly apiPath = 'internal/users/profile-settings'
  private readonly request: CreateUserProfileSettingsBody

  constructor(request: CreateUserProfileSettingsBody) {
    this.request = request
  }

  body(): CreateUserProfileSettingsBody {
    return this.request
  }
}
