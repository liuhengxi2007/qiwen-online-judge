package judgeprotocol.objects

import io.circe.{Decoder, Encoder}

final case class SubmissionId(value: Long)

object SubmissionId:
  given Encoder[SubmissionId] = Encoder.encodeLong.contramap(_.value)
  given Decoder[SubmissionId] = Decoder.decodeLong.emap { value =>
    if value < 1 then Left("Submission id is invalid.") else Right(SubmissionId(value))
  }
