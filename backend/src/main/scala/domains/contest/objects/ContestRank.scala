package domains.contest.objects

import io.circe.{Decoder, Encoder}

/** 比赛排名领域值，一基排名用于榜单和评分快照。 */
final case class ContestRank(value: Int)

/** 提供比赛排名 JSON codec，并拒绝非正排名。 */
object ContestRank:
  given Encoder[ContestRank] = Encoder.encodeInt.contramap(_.value)
  given Decoder[ContestRank] = Decoder.decodeInt.emap { value =>
    if value < 1 then Left("Contest rank must be positive.")
    else Right(ContestRank(value))
  }
