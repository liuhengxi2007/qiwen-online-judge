import type { APIMessage } from '@/system/api/api-message'
import type { ProblemTitleDisplayMode } from '@/objects/problem/ProblemTitleDisplayMode'
import type { DisplayName } from '@/objects/user/DisplayName'
import type { UserDisplayMode } from '@/objects/user/UserDisplayMode'
import type { UserLocale } from '@/objects/user/UserLocale'
import type { Username } from '@/objects/user/Username'
import type { UserProfileSettings } from '@/objects/user/UserProfileSettings'

/** 内部创建用户资料设置请求体；用于账号创建后初始化资料和偏好。 */
type CreateUserProfileSettingsBody = {
  username: Username
  displayName: DisplayName
  displayMode: UserDisplayMode
  locale: UserLocale
  problemTitleDisplayMode: ProblemTitleDisplayMode
  autoMarkMessageRead: boolean
}

/** 创建用户资料设置的内部 API；输出可用于设置页的完整资料设置快照。 */
export class CreateUserProfileSettings implements APIMessage<UserProfileSettings> {
  declare readonly responseType?: UserProfileSettings
  readonly method = 'POST'
  readonly apiPath = 'internal/users/profile-settings'
  private readonly request: CreateUserProfileSettingsBody

  constructor(request: CreateUserProfileSettingsBody) {
    this.request = request
  }

  body(): CreateUserProfileSettingsBody {
    return this.request
  }
}
