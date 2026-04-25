package domains.submission.model

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class SubmissionListRequest(
  userQuery: Option[String],
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
