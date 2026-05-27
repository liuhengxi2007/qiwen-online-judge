package domains.message.objects.request

import domains.message.objects.*

final case class SendDirectMessageRequest(
  content: MessageContent
)
