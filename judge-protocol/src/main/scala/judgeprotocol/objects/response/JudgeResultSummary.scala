package judgeprotocol.objects.response

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import judgeprotocol.objects.SubmissionVerdict

final case class JudgeResultSummary(
  score: BigDecimal,
  verdict: SubmissionVerdict,
  reason: Option[JudgeFailureReason],
  timeUsedMs: Option[Long],
  memoryUsedKb: Option[Long]
)

object JudgeResultSummary:
  def failed(reason: JudgeFailureReason = JudgeFailureReason.SystemError): JudgeResultSummary =
    JudgeResultSummary(BigDecimal(0), SubmissionVerdict.SystemError, Some(reason), None, None)

  def nonSystem(score: BigDecimal, verdict: SubmissionVerdict, timeUsedMs: Option[Long], memoryUsedKb: Option[Long]): JudgeResultSummary =
    JudgeResultSummary(score, normalizeNodeVerdict(verdict), None, timeUsedMs, memoryUsedKb)

  def systemError(score: BigDecimal, reason: JudgeFailureReason, timeUsedMs: Option[Long], memoryUsedKb: Option[Long]): JudgeResultSummary =
    JudgeResultSummary(score, SubmissionVerdict.SystemError, Some(reason), timeUsedMs, memoryUsedKb)

  def normalizeNodeVerdict(verdict: SubmissionVerdict): SubmissionVerdict =
    verdict match
      case SubmissionVerdict.AcceptedByProtocol => SubmissionVerdict.Accepted
      case other => other

  given Encoder[JudgeResultSummary] = deriveEncoder[JudgeResultSummary]
  given Decoder[JudgeResultSummary] = deriveDecoder[JudgeResultSummary]
