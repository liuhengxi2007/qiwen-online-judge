import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { SuccessResponse } from '@/objects/shared/response/SuccessResponse'
import { decodeSuccessResponse } from '@/system/api/http-client'

export class MarkAllMessagesRead implements APIWithSessionMessage<SuccessResponse> {
  declare readonly responseType?: SuccessResponse
  readonly method = 'POST'
  readonly decode = decodeSuccessResponse
  readonly apiPath = 'messages/mark-all-read'

  body(): undefined {
    return undefined
  }
}
