package judgeprotocol.objects.response

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.deriveEncoder
import judgeprotocol.objects.SubmissionVerdict

final case class JudgeResult(
  score: BigDecimal,
  lowestScore: BigDecimal,
  verdict: SubmissionVerdict,
  reason: Option[JudgeFailureReason],
  timeUsedMs: Option[Long],
  memoryUsedKb: Option[Long],
  subtasks: List[JudgeSubtaskResult],
  baseResult: Option[JudgeResult]
)

object JudgeResult:
  given Encoder[JudgeResult] = deriveEncoder[JudgeResult]
  given Decoder[JudgeResult] = Decoder.instance { cursor =>
    for
      score <- cursor.downField("score").as[BigDecimal]
      verdict <- cursor.downField("verdict").as[SubmissionVerdict]
      reason <- cursor.downField("reason").as[Option[JudgeFailureReason]]
      timeUsedMs <- cursor.downField("timeUsedMs").as[Option[Long]]
      memoryUsedKb <- cursor.downField("memoryUsedKb").as[Option[Long]]
      subtasks <- cursor.downField("subtasks").as[List[JudgeSubtaskResult]]
      baseResult <- cursor.downField("baseResult").as[Option[JudgeResult]]
      lowestScore <- cursor
        .downField("lowestScore")
        .as[Option[BigDecimal]]
        .map(_.getOrElse(score))
    yield JudgeResult(score, lowestScore, verdict, reason, timeUsedMs, memoryUsedKb, subtasks, baseResult)
  }
