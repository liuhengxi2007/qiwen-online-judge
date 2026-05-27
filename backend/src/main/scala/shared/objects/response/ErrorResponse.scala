package shared.objects.response

import shared.objects.ApiMessageParams

final case class ErrorResponse(
  code: Option[String],
  message: Option[String],
  params: ApiMessageParams
)
