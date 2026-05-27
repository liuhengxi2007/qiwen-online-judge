package shared.objects.response

import shared.objects.ApiMessageParams

final case class SuccessResponse(
  code: Option[String],
  message: Option[String],
  params: ApiMessageParams
)
