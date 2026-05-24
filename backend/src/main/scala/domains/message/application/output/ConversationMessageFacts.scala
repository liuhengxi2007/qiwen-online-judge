package domains.message.application.output


final case class ConversationMessageFacts(
  viewerHasSentMessage: Boolean,
  otherParticipantMessageCount: Int
)
