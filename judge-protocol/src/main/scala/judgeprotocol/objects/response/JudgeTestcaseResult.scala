package judgeprotocol.objects.response

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.deriveEncoder
import judgeprotocol.objects.SubmissionVerdict

final case class JudgeTestcaseResult(
  index: Int,
  label: Option[String],
  testcaseType: JudgeTestcaseType,
  score: BigDecimal,
  verdict: SubmissionVerdict,
  message: Option[String],
  reason: Option[JudgeFailureReason],
  timeUsedMs: Option[Long],
  memoryUsedKb: Option[Long]
)

object JudgeTestcaseResult:
  given Encoder[JudgeTestcaseResult] = deriveEncoder[JudgeTestcaseResult]
  given Decoder[JudgeTestcaseResult] = Decoder.instance { cursor =>
    for
      index <- cursor.downField("index").as[Int]
      label <- cursor.downField("label").as[Option[String]]
      testcaseType <- cursor.downField("testcaseType").as[Option[JudgeTestcaseType]].map(_.getOrElse(JudgeTestcaseType.Main))
      score <- cursor.downField("score").as[BigDecimal]
      verdict <- cursor.downField("verdict").as[SubmissionVerdict]
      message <- cursor.downField("message").as[Option[String]]
      reason <- cursor.downField("reason").as[Option[JudgeFailureReason]]
      timeUsedMs <- cursor.downField("timeUsedMs").as[Option[Long]]
      memoryUsedKb <- cursor.downField("memoryUsedKb").as[Option[Long]]
    yield JudgeTestcaseResult(index, label, testcaseType, score, verdict, message, reason, timeUsedMs, memoryUsedKb)
  }
