package judgeprotocol.objects.response

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import judgeprotocol.objects.SubmissionVerdict

/** 判题结果树中一个节点的分数、结论、系统原因和资源用量摘要。 */
final case class JudgeResultSummary(
  score: BigDecimal,
  verdict: SubmissionVerdict,
  reason: Option[JudgeFailureReason],
  timeUsedMs: Option[Long],
  memoryUsedKb: Option[Long]
)

/** 提供结果摘要的安全构造函数，统一 SystemError 和 AcceptedByProtocol 的节点语义。 */
object JudgeResultSummary:
  /** 构造系统失败摘要；用于任务无法正常执行或工具失败时上报。 */
  def failed(reason: JudgeFailureReason = JudgeFailureReason.SystemError): JudgeResultSummary =
    JudgeResultSummary(BigDecimal(0), SubmissionVerdict.SystemError, Some(reason), None, None)

  /** 构造非系统错误摘要；会把节点级 AcceptedByProtocol 归一为 Accepted。 */
  def nonSystem(score: BigDecimal, verdict: SubmissionVerdict, timeUsedMs: Option[Long], memoryUsedKb: Option[Long]): JudgeResultSummary =
    JudgeResultSummary(score, normalizeNodeVerdict(verdict), None, timeUsedMs, memoryUsedKb)

  /** 构造带分数和资源用量的系统错误摘要，保留具体 failure reason。 */
  def systemError(score: BigDecimal, reason: JudgeFailureReason, timeUsedMs: Option[Long], memoryUsedKb: Option[Long]): JudgeResultSummary =
    JudgeResultSummary(score, SubmissionVerdict.SystemError, Some(reason), timeUsedMs, memoryUsedKb)

  /** 将只在测试点层有业务意义的 AcceptedByProtocol 归一化，避免子任务/整题节点泄漏内部策略状态。 */
  def normalizeNodeVerdict(verdict: SubmissionVerdict): SubmissionVerdict =
    verdict match
      case SubmissionVerdict.AcceptedByProtocol => SubmissionVerdict.Accepted
      case other => other

  given Encoder[JudgeResultSummary] = deriveEncoder[JudgeResultSummary]
  given Decoder[JudgeResultSummary] = deriveDecoder[JudgeResultSummary]
