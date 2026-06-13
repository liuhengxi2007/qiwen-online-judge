package domains.submission.objects.response

import domains.submission.objects.*

import domains.user.objects.UserIdentity
import domains.problem.objects.{ProblemId, ProblemSlug, ProblemTitle}
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

import java.time.Instant
import scala.util.Try

/** 提交列表摘要；不包含源码和完整 JudgeResult，但标记当前用户能否看详情。 */
final case class SubmissionSummary(
  id: SubmissionId,
  problemId: ProblemId,
  problemSlug: ProblemSlug,
  problemTitle: ProblemTitle,
  resultDisplayMode: SubmissionResultDisplayMode,
  source: SubmissionSource,
  canViewDetail: Boolean,
  submitter: UserIdentity,
  language: SubmissionLanguage,
  status: SubmissionStatus,
  verdict: Option[SubmissionVerdict],
  timeUsedMs: Option[Long],
  memoryUsedKb: Option[Long],
  score: Option[BigDecimal],
  codeLength: Int,
  submittedAt: Instant,
  startedAt: Option[Instant],
  finishedAt: Option[Instant]
)

/** SubmissionSummary 的 JSON 编解码器，Instant 以 ISO-8601 字符串表示。 */
object SubmissionSummary:
  private given Encoder[Instant] = Encoder.encodeString.contramap(_.toString)
  private given Decoder[Instant] = Decoder.decodeString.emap { value =>
    Try(Instant.parse(value)).toEither.left.map(_.getMessage)
  }

  given Encoder[SubmissionSummary] = deriveEncoder[SubmissionSummary]
  given Decoder[SubmissionSummary] = deriveDecoder[SubmissionSummary]
