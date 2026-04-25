package domains.submission.model

import domains.auth.model.Username
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class SubmissionListRequest(
  username: Option[Username],
  problemQuery: Option[String],
  verdict: SubmissionVerdictFilter,
  sort: SubmissionSort,
  direction: SubmissionSortDirection,
  page: Int,
  pageSize: Int
)

object SubmissionListRequest:
  given Encoder[SubmissionListRequest] = deriveEncoder[SubmissionListRequest]
  given Decoder[SubmissionListRequest] = deriveDecoder[SubmissionListRequest]
