package domains.problem.objects.response

import domains.problem.objects.*

import domains.user.objects.UserIdentity
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import shared.objects.access.ResourceAccessPolicy

import java.time.Instant
import scala.util.Try

/** 题目列表摘要；不包含题面正文，但保留访问策略用于管理界面展示。 */
final case class ProblemSummary(
  id: ProblemId,
  slug: ProblemSlug,
  title: ProblemTitle,
  data: ProblemData,
  ready: Boolean,
  accessPolicy: ResourceAccessPolicy,
  otherUserSubmissionAccess: OtherUserSubmissionAccess,
  author: Option[UserIdentity],
  createdAt: Instant,
  updatedAt: Instant
)

/** ProblemSummary 的 JSON 编解码器，Instant 以 ISO-8601 字符串表示。 */
object ProblemSummary:
  private given Encoder[Instant] = Encoder.encodeString.contramap(_.toString)
  private given Decoder[Instant] = Decoder.decodeString.emap { value =>
    Try(Instant.parse(value)).toEither.left.map(_.getMessage)
  }

  given Encoder[ProblemSummary] = deriveEncoder[ProblemSummary]
  given Decoder[ProblemSummary] = deriveDecoder[ProblemSummary]
