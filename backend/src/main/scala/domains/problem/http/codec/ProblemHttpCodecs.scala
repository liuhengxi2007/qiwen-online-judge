package domains.problem.http.codec

import domains.problem.application.input.*
import domains.problem.application.output.*
import domains.problem.http.codec.ProblemModelHttpCodecs.given
import domains.problem.http.ProblemHttpPlans.SetProblemReadyRequest
import domains.user.http.codec.UserModelHttpCodecs.given
import shared.model.PageRequest
import shared.http.codec.SharedHttpCodecs
import shared.http.codec.SharedHttpCodecs.given
import io.circe.{Decoder, Encoder, Json}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.syntax.*

import java.time.Instant
import scala.util.Try

object ProblemHttpCodecs:
  export ProblemModelHttpCodecs.given
  export SharedHttpCodecs.given

  private given Encoder[Instant] = Encoder.encodeString.contramap(_.toString)
  private given Decoder[Instant] = Decoder.decodeString.emap { value =>
    Try(Instant.parse(value)).toEither.left.map(_.getMessage)
  }

  given Encoder[ProblemSearchQuery] = Encoder.encodeString.contramap(_.value)
  given Decoder[ProblemSearchQuery] = Decoder.decodeString.emap(ProblemSearchQuery.parse)

  given Encoder[ProblemListRequest] = Encoder.instance(request =>
    Json.obj(
      "query" -> request.query.asJson,
      "page" -> request.pageRequest.page.asJson,
      "pageSize" -> request.pageRequest.pageSize.asJson
    )
  )

  given Decoder[ProblemListRequest] = Decoder.instance { cursor =>
    for
      query <- cursor.downField("query").as[Option[ProblemSearchQuery]]
      page <- cursor.downField("page").as[Int]
      pageSize <- cursor.downField("pageSize").as[Int]
    yield ProblemListRequest(query = query, pageRequest = PageRequest(page = page, pageSize = pageSize))
  }

  given Encoder[CreateProblemRequest] = deriveEncoder[CreateProblemRequest]
  given Decoder[CreateProblemRequest] = deriveDecoder[CreateProblemRequest]
  given Encoder[UpdateProblemRequest] = deriveEncoder[UpdateProblemRequest]
  given Decoder[UpdateProblemRequest] = deriveDecoder[UpdateProblemRequest]
  given Encoder[DeleteProblemDataPathRequest] = deriveEncoder[DeleteProblemDataPathRequest]
  given Decoder[DeleteProblemDataPathRequest] = deriveDecoder[DeleteProblemDataPathRequest]
  given Encoder[SetProblemReadyRequest] = deriveEncoder[SetProblemReadyRequest]
  given Decoder[SetProblemReadyRequest] = deriveDecoder[SetProblemReadyRequest]

  given Encoder[ProblemSummary] = deriveEncoder[ProblemSummary]
  given Decoder[ProblemSummary] = deriveDecoder[ProblemSummary]
  given Encoder[ProblemDetail] = deriveEncoder[ProblemDetail]
  given Decoder[ProblemDetail] = deriveDecoder[ProblemDetail]
  given Encoder[ProblemSuggestion] = deriveEncoder[ProblemSuggestion]
  given Decoder[ProblemSuggestion] = deriveDecoder[ProblemSuggestion]
  given Encoder[ProblemDataFileListResponse] = deriveEncoder[ProblemDataFileListResponse]
  given Decoder[ProblemDataFileListResponse] = deriveDecoder[ProblemDataFileListResponse]
  given Encoder[ProblemDataTreeResponse] = deriveEncoder[ProblemDataTreeResponse]
  given Decoder[ProblemDataTreeResponse] = deriveDecoder[ProblemDataTreeResponse]
  given Encoder[ProblemDataUploadResult] = deriveEncoder[ProblemDataUploadResult]
  given Decoder[ProblemDataUploadResult] = deriveDecoder[ProblemDataUploadResult]
