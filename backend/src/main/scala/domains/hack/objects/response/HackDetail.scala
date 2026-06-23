package domains.hack.objects.response

import domains.hack.objects.{HackId, HackStatus}
import domains.problem.objects.{ProblemId, ProblemSlug, ProblemTitle}
import domains.submission.objects.SubmissionId
import domains.user.objects.UserIdentity
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import judgeprotocol.objects.response.JudgeResult

import java.time.Instant
import scala.util.Try

/** hack 详情响应；包含输入、可选策略源码、worker 输出和结果快照。 */
final case class HackDetail(
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
  input: String,
  strategyProviderSource: Option[String],
  answer: Option[String],
  oldScore: BigDecimal,
  newResult: Option[JudgeResult],
  validatorMessage: Option[String],
  standardMessage: Option[String],
  targetMessage: Option[String],
  createdAt: Instant,
  startedAt: Option[Instant],
  finishedAt: Option[Instant]
)

/** HackDetail 的 JSON 编解码器，Instant 以 ISO-8601 字符串表示。 */
object HackDetail:
  private given Encoder[Instant] = Encoder.encodeString.contramap(_.toString)
  private given Decoder[Instant] = Decoder.decodeString.emap { value =>
    Try(Instant.parse(value)).toEither.left.map(_.getMessage)
  }

  given Encoder[HackDetail] = deriveEncoder[HackDetail]
  given Decoder[HackDetail] = deriveDecoder[HackDetail]
