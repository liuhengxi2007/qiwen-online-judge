package domains.submission.http.codec

import domains.problem.http.codec.ProblemModelHttpCodecs.given
import domains.submission.application.input.*
import domains.submission.application.output.*
import domains.submission.http.codec.SubmissionModelHttpCodecs.given
import domains.user.http.codec.UserModelHttpCodecs.given
import shared.model.PageRequest
import shared.http.codec.SharedHttpCodecs
import shared.http.codec.SharedHttpCodecs.given
import io.circe.{Decoder, Encoder, Json}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.syntax.*

import java.time.Instant
import scala.util.Try

object SubmissionHttpCodecs:
  export SubmissionModelHttpCodecs.given
  export SharedHttpCodecs.given

  private given Encoder[Instant] = Encoder.encodeString.contramap(_.toString)
  private given Decoder[Instant] = Decoder.decodeString.emap { value =>
    Try(Instant.parse(value)).toEither.left.map(_.getMessage)
  }

  given Encoder[SubmissionUserQuery] = Encoder.encodeString.contramap(_.value)
  given Decoder[SubmissionUserQuery] = Decoder.decodeString.emap(SubmissionUserQuery.parse)
  given Encoder[SubmissionProblemQuery] = Encoder.encodeString.contramap(_.value)
  given Decoder[SubmissionProblemQuery] = Decoder.decodeString.emap(SubmissionProblemQuery.parse)
  given Encoder[SubmissionVerdictFilter] = Encoder.encodeString.contramap(SubmissionVerdictFilter.toDatabase)
  given Decoder[SubmissionVerdictFilter] = Decoder.decodeString.emap(SubmissionVerdictFilter.parse)
  given Encoder[SubmissionSort] = Encoder.encodeString.contramap(SubmissionSort.toDatabase)
  given Decoder[SubmissionSort] = Decoder.decodeString.emap(SubmissionSort.parse)
  given Encoder[SubmissionSortDirection] = Encoder.encodeString.contramap(SubmissionSortDirection.toDatabase)
  given Decoder[SubmissionSortDirection] = Decoder.decodeString.emap(SubmissionSortDirection.parse)

  given Encoder[SubmissionListRequest] = Encoder.instance(request =>
    Json.obj(
      "userQuery" -> request.userQuery.asJson,
      "problemQuery" -> request.problemQuery.asJson,
      "verdict" -> request.verdict.asJson,
      "sort" -> request.sort.asJson,
      "direction" -> request.direction.asJson,
      "page" -> request.pageRequest.page.asJson,
      "pageSize" -> request.pageRequest.pageSize.asJson
    )
  )

  given Decoder[SubmissionListRequest] = Decoder.instance { cursor =>
    for
      userQuery <- cursor.downField("userQuery").as[Option[SubmissionUserQuery]]
      problemQuery <- cursor.downField("problemQuery").as[Option[SubmissionProblemQuery]]
      verdict <- cursor.downField("verdict").as[SubmissionVerdictFilter]
      sort <- cursor.downField("sort").as[SubmissionSort]
      direction <- cursor.downField("direction").as[SubmissionSortDirection]
      page <- cursor.downField("page").as[Int]
      pageSize <- cursor.downField("pageSize").as[Int]
    yield SubmissionListRequest(
      userQuery = userQuery,
      problemQuery = problemQuery,
      verdict = verdict,
      sort = sort,
      direction = direction,
      pageRequest = PageRequest(page = page, pageSize = pageSize)
    )
  }

  given Encoder[CreateSubmissionRequest] = deriveEncoder[CreateSubmissionRequest]
  given Decoder[CreateSubmissionRequest] = deriveDecoder[CreateSubmissionRequest]

  given Encoder[SubmissionSummary] = deriveEncoder[SubmissionSummary]
  given Decoder[SubmissionSummary] = deriveDecoder[SubmissionSummary]
  given Encoder[SubmissionDetail] = deriveEncoder[SubmissionDetail]
  given Decoder[SubmissionDetail] = deriveDecoder[SubmissionDetail]
