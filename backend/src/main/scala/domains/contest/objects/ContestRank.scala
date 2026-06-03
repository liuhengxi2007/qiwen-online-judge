package domains.contest.objects

import io.circe.{Decoder, Encoder}

final case class ContestRank(value: Int)

object ContestRank:
  given Encoder[ContestRank] = Encoder.encodeInt.contramap(_.value)
  given Decoder[ContestRank] = Decoder.decodeInt.emap { value =>
    if value < 1 then Left("Contest rank must be positive.")
    else Right(ContestRank(value))
  }
