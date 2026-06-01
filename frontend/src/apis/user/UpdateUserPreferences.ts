import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { SessionResponse } from '@/objects/auth/response/SessionResponse'
import type { UpdateManagedUserPreferencesRequest } from '@/objects/user/request/UpdateManagedUserPreferencesRequest'
import type { UpdateOwnPreferencesRequest } from '@/objects/user/request/UpdateOwnPreferencesRequest'
import type { Username } from '@/objects/user/Username'
import { usernameValue } from '@/objects/user/Username'
import { fromSessionResponseContract } from '@/objects/auth/response/SessionResponse'

type UpdateUserPreferencesRequest = UpdateOwnPreferencesRequest | UpdateManagedUserPreferencesRequest

export class UpdateUserPreferences implements APIWithSessionMessage<SessionResponse> {
  declare readonly responseType?: SessionResponse
  readonly method = 'POST'
  readonly decode = fromSessionResponseContract
  readonly apiPath: string
  private readonly request: UpdateUserPreferencesRequest

  constructor(username: Username, request: UpdateUserPreferencesRequest) {
    this.apiPath = `users/${encodeURIComponent(usernameValue(username))}/settings/preferences`
    this.request = request
  }

  body(): UpdateUserPreferencesRequest {
    return this.request
  }
}
