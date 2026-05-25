package shared.model.response

import shared.model.ApiMessageParams

final case class SuccessResponse(
  code: Option[String],
  message: Option[String],
  params: ApiMessageParams
)
