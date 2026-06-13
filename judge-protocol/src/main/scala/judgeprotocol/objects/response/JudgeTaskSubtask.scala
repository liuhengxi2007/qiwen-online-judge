package judgeprotocol.objects.response

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.deriveEncoder

/** 判题任务中的子任务节点，定义分数权重、模式、工具和测试点集合。 */
final case class JudgeTaskSubtask(
  index: Int,
  label: Option[String],
  scoreRatio: BigDecimal,
  mode: JudgeTaskMode,
  validator: Option[JudgeTaskTool],
  standard: Option[JudgeTaskStandard],
  hack: JudgeTaskHackConfig = JudgeTaskHackConfig.Disabled,
  aggregation: JudgeTaskAggregation,
  testcases: List[JudgeTaskTestcase]
)

/** 负责子任务协议编解码，并兼容早期缺失 hack 配置的 payload。 */
object JudgeTaskSubtask:
  given Encoder[JudgeTaskSubtask] = deriveEncoder[JudgeTaskSubtask]
  given Decoder[JudgeTaskSubtask] = Decoder.instance { cursor =>
    for
      index <- cursor.downField("index").as[Int]
      label <- cursor.downField("label").as[Option[String]]
      scoreRatio <- cursor.downField("scoreRatio").as[BigDecimal]
      mode <- cursor.downField("mode").as[JudgeTaskMode]
      validator <- cursor.downField("validator").as[Option[JudgeTaskTool]]
      standard <- cursor.downField("standard").as[Option[JudgeTaskStandard]]
      hack <- cursor.downField("hack").as[Option[JudgeTaskHackConfig]].map(_.getOrElse(JudgeTaskHackConfig.Disabled))
      aggregation <- cursor.downField("aggregation").as[JudgeTaskAggregation]
      testcases <- cursor.downField("testcases").as[List[JudgeTaskTestcase]]
    yield JudgeTaskSubtask(index, label, scoreRatio, mode, validator, standard, hack, aggregation, testcases)
  }
