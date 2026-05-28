package domains.user.objects

import io.circe.{Decoder, Encoder}


final case class UserContribution(value: BigDecimal)

object UserContribution:
  given Encoder[UserContribution] = Encoder.encodeBigDecimal.contramap(_.value)
  given Decoder[UserContribution] = Decoder.decodeBigDecimal.map(UserContribution(_))
