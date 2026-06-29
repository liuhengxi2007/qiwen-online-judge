package judgeprotocol.objects.response

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.deriveEncoder
import judgeprotocol.objects.SubmissionVerdict

/** 一个子任务的聚合结果，包含基础结果、最差结果和所有测试点明细。 */
final case class JudgeSubtaskResult(
  index: Int,
  label: Option[String],
  baseResult: JudgeResultSummary,
  worstResult: JudgeResultSummary,
  testcases: List[JudgeTestcaseResult]
)

/** 负责子任务结果的协议编解码，并兼容旧格式 verdict/reason 的回填。 */
object JudgeSubtaskResult:
  given Encoder[JudgeSubtaskResult] = deriveEncoder[JudgeSubtaskResult]
  given Decoder[JudgeSubtaskResult] = Decoder.instance { cursor =>
    for
      index <- cursor.downField("index").as[Int]
      label <- cursor.downField("label").as[Option[String]]
      legacyVerdict <- cursor.downField("verdict").as[Option[SubmissionVerdict]]
      legacyReason <- cursor.downField("reason").as[Option[JudgeFailureReason]]
      baseResult <- JudgeResult.decodeSummaryField(cursor, "baseResult", legacyVerdict, legacyReason)
      worstResult <- JudgeResult.decodeSummaryField(cursor, "worstResult", legacyVerdict, legacyReason)
      testcases <- cursor.downField("testcases").as[List[JudgeTestcaseResult]]
    yield JudgeSubtaskResult(index, label, baseResult, worstResult, testcases)
  }
