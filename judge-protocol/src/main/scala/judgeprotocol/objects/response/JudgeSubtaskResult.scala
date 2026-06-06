package judgeprotocol.objects.response

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.deriveEncoder
import judgeprotocol.objects.SubmissionVerdict

final case class JudgeSubtaskResult(
  index: Int,
  label: Option[String],
  baseResult: JudgeResultSummary,
  worstResult: JudgeResultSummary,
  testcases: List[JudgeTestcaseResult]
)

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
