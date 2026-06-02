import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { UserSettingsResponse } from '@/objects/user/response/UserSettingsResponse'
import type { Username } from '@/objects/user/Username'
import { usernameValue } from '@/objects/user/Username'

export class UploadUserAvatar implements APIWithSessionMessage<UserSettingsResponse> {
  declare readonly responseType?: UserSettingsResponse
  readonly method = 'POST'
  readonly apiPath: string
  private readonly file: File

  constructor(targetUsername: Username, file: File) {
    this.apiPath = `users/${usernameValue(targetUsername)}/avatar`
    this.file = file
  }

  body(): undefined {
    return undefined
  }

  formData(): FormData {
    const formData = new FormData()
    formData.set('file', this.file)
    return formData
  }
}
