package judgeprotocol.objects.response

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.deriveEncoder
import judgeprotocol.objects.SubmissionVerdict

final case class JudgeSubtaskResult(
  index: Int,
  label: Option[String],
  score: BigDecimal,
  lowestScore: BigDecimal,
  verdict: SubmissionVerdict,
  timeUsedMs: Option[Long],
  memoryUsedKb: Option[Long],
  reason: Option[JudgeFailureReason],
  testcases: List[JudgeTestcaseResult],
  baseResult: Option[JudgeSubtaskResult]
)

object JudgeSubtaskResult:
  given Encoder[JudgeSubtaskResult] = deriveEncoder[JudgeSubtaskResult]
  given Decoder[JudgeSubtaskResult] = Decoder.instance { cursor =>
    for
      index <- cursor.downField("index").as[Int]
      label <- cursor.downField("label").as[Option[String]]
      score <- cursor.downField("score").as[BigDecimal]
      verdict <- cursor.downField("verdict").as[SubmissionVerdict]
      timeUsedMs <- cursor.downField("timeUsedMs").as[Option[Long]]
      memoryUsedKb <- cursor.downField("memoryUsedKb").as[Option[Long]]
      reason <- cursor.downField("reason").as[Option[JudgeFailureReason]]
      testcases <- cursor.downField("testcases").as[List[JudgeTestcaseResult]]
      baseResult <- cursor.downField("baseResult").as[Option[JudgeSubtaskResult]]
      lowestScore <- cursor
        .downField("lowestScore")
        .as[Option[BigDecimal]]
        .map(_.getOrElse(testcases.map(_.score).minOption.getOrElse(score)))
    yield JudgeSubtaskResult(index, label, score, lowestScore, verdict, timeUsedMs, memoryUsedKb, reason, testcases, baseResult)
  }
