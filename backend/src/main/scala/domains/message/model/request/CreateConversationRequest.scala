package domains.message.model.request


import domains.user.model.Username

final case class CreateConversationRequest(
  targetUsername: Username
)
