package domains.message.application.input

import domains.message.model.*

final case class SendDirectMessageRequest(
  content: MessageContent
)
