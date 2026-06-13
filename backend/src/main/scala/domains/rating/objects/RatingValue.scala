package domains.rating.objects

import io.circe.{Decoder, Encoder}

/** 用户评分领域值，封装当前可展示的 rating 数值。 */
final case class RatingValue(value: BigDecimal)

/** 提供评分初始值和 JSON codec。 */
object RatingValue:
  val initial: RatingValue = RatingValue(BigDecimal(1500))

  given Encoder[RatingValue] = Encoder.encodeBigDecimal.contramap(_.value)
  given Decoder[RatingValue] = Decoder.decodeBigDecimal.emap { value =>
    Right(RatingValue(value))
  }
