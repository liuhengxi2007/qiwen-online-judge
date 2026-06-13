package judgeprotocol.objects.response

import io.circe.{Decoder, DecodingFailure, Encoder, HCursor}
import io.circe.generic.semiauto.deriveEncoder
import judgeprotocol.objects.SubmissionVerdict

/** 整次提交判题的结果树，分别保留基础结果、最差结果和各子任务明细。 */
final case class JudgeResult(
  baseResult: JudgeResultSummary,
  worstResult: JudgeResultSummary,
  subtasks: List[JudgeSubtaskResult]
)

/** 负责判题结果的协议编解码，并兼容旧格式中顶层 verdict/reason 的回填。 */
object JudgeResult:
  given Encoder[JudgeResult] = deriveEncoder[JudgeResult]
  given Decoder[JudgeResult] = Decoder.instance { cursor =>
    for
      legacyVerdict <- cursor.downField("verdict").as[Option[SubmissionVerdict]]
      legacyReason <- cursor.downField("reason").as[Option[JudgeFailureReason]]
      baseResult <- decodeSummaryField(cursor, "baseResult", legacyVerdict, legacyReason)
      worstResult <- decodeSummaryField(cursor, "worstResult", legacyVerdict, legacyReason)
      subtasks <- cursor.downField("subtasks").as[List[JudgeSubtaskResult]]
    yield JudgeResult(baseResult, worstResult, subtasks)
  }

  /** 解码 baseResult/worstResult 节点；缺失 verdict 时尝试从旧字段回退。 */
  private[response] def decodeSummaryField(
    cursor: HCursor,
    fieldName: String,
    fallbackVerdict: Option[SubmissionVerdict],
    fallbackReason: Option[JudgeFailureReason]
  ): Decoder.Result[JudgeResultSummary] =
    val summary = cursor.downField(fieldName)
    for
      score <- summary.downField("score").as[BigDecimal]
      rawVerdict <- summary.downField("verdict").as[Option[SubmissionVerdict]]
      verdict <- rawVerdict.orElse(fallbackVerdict).toRight(DecodingFailure(s"$fieldName.verdict is required", summary.history))
      rawReason <- summary.downField("reason").as[Option[JudgeFailureReason]]
      timeUsedMs <- summary.downField("timeUsedMs").as[Option[Long]]
      memoryUsedKb <- summary.downField("memoryUsedKb").as[Option[Long]]
    yield
      JudgeResultSummary(
        score = score,
        verdict = JudgeResultSummary.normalizeNodeVerdict(verdict),
        reason = rawReason.orElse(fallbackReason),
        timeUsedMs = timeUsedMs,
        memoryUsedKb = memoryUsedKb
      )
