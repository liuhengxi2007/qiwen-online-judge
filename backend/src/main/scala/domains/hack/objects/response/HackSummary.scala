package domains.hack.objects.response

import domains.hack.objects.{HackId, HackStatus}
import domains.problem.objects.{ProblemId, ProblemSlug, ProblemTitle}
import domains.submission.objects.SubmissionId
import domains.user.objects.UserIdentity
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

import java.time.Instant
import scala.util.Try

/** hack 列表摘要；不包含输入和程序输出等详情字段。 */
final case class HackSummary(
  id: HackId,
  problemId: ProblemId,
  problemSlug: ProblemSlug,
  problemTitle: ProblemTitle,
  targetSubmissionId: SubmissionId,
  targetSubmitter: UserIdentity,
  author: UserIdentity,
  subtaskIndex: Int,
  subtaskLabel: Option[String],
  status: HackStatus,
  oldScore: BigDecimal,
  newScore: Option[BigDecimal],
  createdAt: Instant,
  finishedAt: Option[Instant]
)

/** HackSummary 的 JSON 编解码器，Instant 以 ISO-8601 字符串表示。 */
object HackSummary:
  private given Encoder[Instant] = Encoder.encodeString.contramap(_.toString)
  private given Decoder[Instant] = Decoder.decodeString.emap { value =>
    Try(Instant.parse(value)).toEither.left.map(_.getMessage)
  }

  given Encoder[HackSummary] = deriveEncoder[HackSummary]
  given Decoder[HackSummary] = deriveDecoder[HackSummary]
