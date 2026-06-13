package domains.contest.objects

import io.circe.{Decoder, Encoder}

/** 比赛分数领域值，封装可为小数的累计得分。 */
final case class ContestScore(value: BigDecimal)

/** 提供比赛分数 JSON codec，不在此处限制具体分数范围。 */
object ContestScore:
  given Encoder[ContestScore] = Encoder.encodeBigDecimal.contramap(_.value)
  given Decoder[ContestScore] = Decoder.decodeBigDecimal.map(ContestScore(_))
