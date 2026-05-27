package domains.message.objects.response


final case class ConversationMessageFacts(
  viewerHasSentMessage: Boolean,
  otherParticipantMessageCount: Int
)
