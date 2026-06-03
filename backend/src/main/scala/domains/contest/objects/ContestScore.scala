package domains.contest.objects

import io.circe.{Decoder, Encoder}

final case class ContestScore(value: BigDecimal)

object ContestScore:
  given Encoder[ContestScore] = Encoder.encodeBigDecimal.contramap(_.value)
  given Decoder[ContestScore] = Decoder.decodeBigDecimal.map(ContestScore(_))
