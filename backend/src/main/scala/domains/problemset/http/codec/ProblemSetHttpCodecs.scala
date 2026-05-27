package domains.problemset.http.codec

import domains.problem.http.codec.ProblemModelHttpCodecs.given
import domains.problemset.objects.request.*
import domains.problemset.objects.response.*
import domains.problemset.http.codec.ProblemSetModelHttpCodecs.given
import domains.user.http.codec.UserModelHttpCodecs.given
import shared.http.codec.SharedHttpCodecs
import shared.http.codec.SharedHttpCodecs.given
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

import java.time.Instant
import scala.util.Try

object ProblemSetHttpCodecs:
  export ProblemSetModelHttpCodecs.given
  export SharedHttpCodecs.given

  private given Encoder[Instant] = Encoder.encodeString.contramap(_.toString)
  private given Decoder[Instant] = Decoder.decodeString.emap { value =>
    Try(Instant.parse(value)).toEither.left.map(_.getMessage)
  }

  given Encoder[CreateProblemSetRequest] = deriveEncoder[CreateProblemSetRequest]
  given Decoder[CreateProblemSetRequest] = deriveDecoder[CreateProblemSetRequest]
  given Encoder[UpdateProblemSetRequest] = deriveEncoder[UpdateProblemSetRequest]
  given Decoder[UpdateProblemSetRequest] = deriveDecoder[UpdateProblemSetRequest]
  given Encoder[AddProblemToProblemSetRequest] = deriveEncoder[AddProblemToProblemSetRequest]
  given Decoder[AddProblemToProblemSetRequest] = deriveDecoder[AddProblemToProblemSetRequest]

  given Encoder[ProblemSetSummary] = deriveEncoder[ProblemSetSummary]
  given Decoder[ProblemSetSummary] = deriveDecoder[ProblemSetSummary]
  given Encoder[ProblemSetDetail] = deriveEncoder[ProblemSetDetail]
  given Decoder[ProblemSetDetail] = deriveDecoder[ProblemSetDetail]
