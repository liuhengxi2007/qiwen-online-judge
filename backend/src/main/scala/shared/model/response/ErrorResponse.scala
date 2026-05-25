package shared.model.response

import shared.model.ApiMessageParams

final case class ErrorResponse(
  code: Option[String],
  message: Option[String],
  params: ApiMessageParams
)
