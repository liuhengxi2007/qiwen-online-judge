package domains.user.objects

import io.circe.{Decoder, Encoder}


/** 用户贡献值，当前由博客和评论投票分数聚合得到。 */
final case class UserContribution(value: BigDecimal)

/** 提供贡献值的 JSON 数值编解码。 */
object UserContribution:
  given Encoder[UserContribution] = Encoder.encodeBigDecimal.contramap(_.value)
  given Decoder[UserContribution] = Decoder.decodeBigDecimal.map(UserContribution(_))
