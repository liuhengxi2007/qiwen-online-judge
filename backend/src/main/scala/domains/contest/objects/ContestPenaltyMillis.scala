package domains.contest.objects

import io.circe.{Decoder, Encoder}

/** 比赛罚时毫秒数领域值，用于榜单和评分快照。 */
final case class ContestPenaltyMillis(value: Long)

/** 提供罚时毫秒数 JSON codec，并拒绝负数输入。 */
object ContestPenaltyMillis:
  given Encoder[ContestPenaltyMillis] = Encoder.encodeLong.contramap(_.value)
  given Decoder[ContestPenaltyMillis] = Decoder.decodeLong.emap { value =>
    if value < 0 then Left("Contest penalty milliseconds must be non-negative.")
    else Right(ContestPenaltyMillis(value))
  }
