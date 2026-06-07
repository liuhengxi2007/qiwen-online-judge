package domains.rating.objects

import io.circe.{Decoder, Encoder}

final case class RatingValue(value: BigDecimal)

object RatingValue:
  val initial: RatingValue = RatingValue(BigDecimal(1500))

  given Encoder[RatingValue] = Encoder.encodeBigDecimal.contramap(_.value)
  given Decoder[RatingValue] = Decoder.decodeBigDecimal.emap { value =>
    Right(RatingValue(value))
  }
