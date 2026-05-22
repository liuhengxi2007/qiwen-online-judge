package domains.problemset.http.codec

import domains.problem.http.codec.ProblemModelHttpCodecs.given
import domains.problemset.model.*
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

import java.util.UUID
import scala.util.Try

object ProblemSetModelHttpCodecs:
  given Encoder[ProblemSetId] = Encoder.encodeString.contramap(_.value.toString)
  given Decoder[ProblemSetId] = Decoder.decodeString.emap { value =>
    Try(UUID.fromString(value)).toEither.left.map(_.getMessage).map(ProblemSetId(_))
  }

  given Encoder[ProblemSetSlug] = Encoder.encodeString.contramap(_.value)
  given Decoder[ProblemSetSlug] = Decoder.decodeString.emap(ProblemSetSlug.parse)

  given Encoder[ProblemSetTitle] = Encoder.encodeString.contramap(_.value)
  given Decoder[ProblemSetTitle] = Decoder.decodeString.emap(ProblemSetTitle.parse)

  given Encoder[ProblemSetDescription] = Encoder.encodeString.contramap(_.value)
  given Decoder[ProblemSetDescription] = Decoder.decodeString.emap(ProblemSetDescription.parse)

  given Encoder[ProblemSetProblemSummary] = deriveEncoder[ProblemSetProblemSummary]
  given Decoder[ProblemSetProblemSummary] = deriveDecoder[ProblemSetProblemSummary]
