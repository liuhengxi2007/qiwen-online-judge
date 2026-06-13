package domains.rating.objects.response

import domains.contest.objects.{ContestSlug, ContestTitle}
import domains.user.objects.UserIdentity
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

import java.time.Instant
import scala.util.Try

/** 评分管理页中的已追加比赛条目，包含序列位置、快照元数据和追加人。 */
final case class RatingContestListItem(
  position: Int,
  contestSlug: ContestSlug,
  contestTitle: ContestTitle,
  contestStartAt: Instant,
  contestEndAt: Instant,
  m: Int,
  participantCount: Int,
  overlapWarning: Boolean,
  appendedBy: Option[UserIdentity],
  appendedAt: Instant
)

/** 提供评分比赛管理条目 JSON codec，并显式处理 Instant 字符串格式。 */
object RatingContestListItem:
  private given Encoder[Instant] = Encoder.encodeString.contramap(_.toString)
  private given Decoder[Instant] = Decoder.decodeString.emap(value => Try(Instant.parse(value)).toEither.left.map(_.getMessage))

  given Encoder[RatingContestListItem] = deriveEncoder[RatingContestListItem]
  given Decoder[RatingContestListItem] = deriveDecoder[RatingContestListItem]
