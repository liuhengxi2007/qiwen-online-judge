package domains.message.objects.request


import domains.user.objects.Username

final case class CreateConversationRequest(
  targetUsername: Username
)
