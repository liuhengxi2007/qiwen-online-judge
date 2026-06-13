package domains.contest.objects.response

import domains.contest.objects.*
import domains.user.objects.UserIdentity
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import shared.objects.access.ResourceAccessPolicy

import java.time.Instant
import scala.util.Try

/** 比赛列表摘要响应，包含详情可见标记但不携带赛题列表。 */
final case class ContestSummary(
  id: ContestId,
  slug: ContestSlug,
  title: ContestTitle,
  description: ContestDescription,
  startAt: Instant,
  endAt: Instant,
  accessPolicy: ResourceAccessPolicy,
  registrationStatus: ContestRegistrationStatus,
  canViewDetail: Boolean,
  author: Option[UserIdentity],
  createdAt: Instant,
  updatedAt: Instant
)

/** 提供比赛摘要 JSON codec，并显式处理 Instant 字符串格式。 */
object ContestSummary:
  private given Encoder[Instant] = Encoder.encodeString.contramap(_.toString)
  private given Decoder[Instant] = Decoder.decodeString.emap { value =>
    Try(Instant.parse(value)).toEither.left.map(_.getMessage)
  }

  given Encoder[ContestSummary] = deriveEncoder[ContestSummary]
  given Decoder[ContestSummary] = deriveDecoder[ContestSummary]
