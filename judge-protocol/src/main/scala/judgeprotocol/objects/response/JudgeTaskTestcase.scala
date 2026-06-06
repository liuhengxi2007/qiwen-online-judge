package judgeprotocol.objects.response

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.deriveEncoder

final case class JudgeTaskTestcase(
  index: Int,
  label: Option[String],
  testcaseType: JudgeTestcaseType,
  scoreRatio: BigDecimal,
  limits: JudgeTaskLimits,
  checker: JudgeTaskChecker,
  input: JudgeTaskFileRef,
  answer: Option[JudgeTaskFileRef],
  strategyProvider: Option[JudgeTaskTool]
)

object JudgeTaskTestcase:
  given Encoder[JudgeTaskTestcase] = deriveEncoder[JudgeTaskTestcase]
  given Decoder[JudgeTaskTestcase] = Decoder.instance { cursor =>
    for
      index <- cursor.downField("index").as[Int]
      label <- cursor.downField("label").as[Option[String]]
      testcaseType <- cursor.downField("testcaseType").as[Option[JudgeTestcaseType]].map(_.getOrElse(JudgeTestcaseType.Main))
      scoreRatio <- cursor.downField("scoreRatio").as[BigDecimal]
      limits <- cursor.downField("limits").as[JudgeTaskLimits]
      checker <- cursor.downField("checker").as[JudgeTaskChecker]
      input <- cursor.downField("input").as[JudgeTaskFileRef]
      answer <- cursor.downField("answer").as[Option[JudgeTaskFileRef]]
      strategyProvider <- cursor.downField("strategyProvider").as[Option[JudgeTaskTool]]
    yield JudgeTaskTestcase(index, label, testcaseType, scoreRatio, limits, checker, input, answer, strategyProvider)
  }
