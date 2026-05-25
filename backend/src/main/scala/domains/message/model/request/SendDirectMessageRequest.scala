package domains.message.model.request

import domains.message.model.*

final case class SendDirectMessageRequest(
  content: MessageContent
)
