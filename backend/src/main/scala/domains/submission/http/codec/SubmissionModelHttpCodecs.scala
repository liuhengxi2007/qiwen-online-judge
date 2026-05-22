package domains.submission.http.codec

import domains.submission.model.*
import io.circe.{Decoder, Encoder}

object SubmissionModelHttpCodecs:
  given Encoder[SubmissionId] = Encoder.encodeLong.contramap(_.value)
  given Decoder[SubmissionId] = Decoder.decodeLong.emap { value =>
    if value < 1 then Left("Submission id is invalid.") else Right(SubmissionId(value))
  }

  given Encoder[SubmissionLanguage] = Encoder.encodeString.contramap(SubmissionLanguage.toDatabase)
  given Decoder[SubmissionLanguage] = Decoder.decodeString.emap(SubmissionLanguage.parse)

  given Encoder[SubmissionSourceCode] = Encoder.encodeString.contramap(_.value)
  given Decoder[SubmissionSourceCode] = Decoder.decodeString.emap(SubmissionSourceCode.parse)

  given Encoder[SubmissionVerdict] = Encoder.encodeString.contramap(SubmissionVerdict.toDatabase)
  given Decoder[SubmissionVerdict] = Decoder.decodeString.emap(SubmissionVerdict.parse)

  given Encoder[SubmissionStatus] = Encoder.encodeString.contramap(SubmissionStatus.toDatabase)
  given Decoder[SubmissionStatus] = Decoder.decodeString.emap(SubmissionStatus.parse)
