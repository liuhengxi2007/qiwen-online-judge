import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { SessionResponse } from '@/objects/auth/response/SessionResponse'
import type { UpdateManagedUserProfileRequest } from '@/objects/user/request/UpdateManagedUserProfileRequest'
import type { UpdateOwnProfileRequest } from '@/objects/user/request/UpdateOwnProfileRequest'
import type { Username } from '@/objects/user/Username'
import { usernameValue } from '@/objects/user/Username'

type UpdateUserProfileRequest = UpdateOwnProfileRequest | UpdateManagedUserProfileRequest

export class UpdateUserProfile implements APIWithSessionMessage<SessionResponse> {
  declare readonly responseType?: SessionResponse
  readonly method = 'POST'
  readonly apiPath: string
  private readonly request: UpdateUserProfileRequest

  constructor(username: Username, request: UpdateUserProfileRequest) {
    this.apiPath = `users/${encodeURIComponent(usernameValue(username))}/settings/profile`
    this.request = request
  }

  body(): UpdateUserProfileRequest {
    return this.request
  }
}
