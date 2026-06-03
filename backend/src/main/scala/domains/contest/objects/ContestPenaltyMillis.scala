package domains.contest.objects

import io.circe.{Decoder, Encoder}

final case class ContestPenaltyMillis(value: Long)

object ContestPenaltyMillis:
  given Encoder[ContestPenaltyMillis] = Encoder.encodeLong.contramap(_.value)
  given Decoder[ContestPenaltyMillis] = Decoder.decodeLong.emap { value =>
    if value < 0 then Left("Contest penalty milliseconds must be non-negative.")
    else Right(ContestPenaltyMillis(value))
  }
