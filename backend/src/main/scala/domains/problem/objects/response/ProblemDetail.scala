package domains.problem.objects.response

import domains.problem.objects.*

import domains.user.objects.UserIdentity
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import shared.objects.access.ResourceAccessPolicy

import java.time.Instant
import scala.util.Try

/** 题目详情响应；包含题面、数据状态、访问策略和当前调用者的管理权限。 */
final case class ProblemDetail(
  id: ProblemId,
  slug: ProblemSlug,
  title: ProblemTitle,
  statement: ProblemStatementText,
  data: ProblemData,
  ready: Boolean,
  accessPolicy: ResourceAccessPolicy,
  otherUserSubmissionAccess: OtherUserSubmissionAccess,
  author: Option[UserIdentity],
  canManage: Boolean,
  createdAt: Instant,
  updatedAt: Instant
)

/** ProblemDetail 的 JSON 编解码器，Instant 以 ISO-8601 字符串表示。 */
object ProblemDetail:
  private given Encoder[Instant] = Encoder.encodeString.contramap(_.toString)
  private given Decoder[Instant] = Decoder.decodeString.emap { value =>
    Try(Instant.parse(value)).toEither.left.map(_.getMessage)
  }

  given Encoder[ProblemDetail] = deriveEncoder[ProblemDetail]
  given Decoder[ProblemDetail] = deriveDecoder[ProblemDetail]
