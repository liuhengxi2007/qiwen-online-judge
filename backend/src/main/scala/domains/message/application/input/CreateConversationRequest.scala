package domains.message.application.input


import domains.user.model.Username

final case class CreateConversationRequest(
  targetUsername: Username
)
