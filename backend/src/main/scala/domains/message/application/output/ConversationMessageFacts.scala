package domains.message.application.output

import domains.message.model.*

final case class ConversationMessageFacts(
  viewerHasSentMessage: Boolean,
  otherParticipantMessageCount: Int
)
