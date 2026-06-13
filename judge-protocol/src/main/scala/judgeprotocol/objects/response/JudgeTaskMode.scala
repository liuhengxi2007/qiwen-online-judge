package judgeprotocol.objects.response

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

/** 子任务运行模式，传统题使用单 role，交互题使用多 role 与 interactor。 */
final case class JudgeTaskMode(
  `type`: String,
  role: Option[String],
  roles: List[String],
  interactor: Option[JudgeTaskTool]
)

/** 提供传统题和交互题模式的协议构造函数。 */
object JudgeTaskMode:
  /** 构造传统题模式；judger 会用 role 选择被测程序。 */
  def traditional(role: String): JudgeTaskMode =
    JudgeTaskMode("traditional", Some(role), Nil, None)

  /** 构造交互题模式；roles 决定参与程序列表，interactor 是必需工具。 */
  def interactive(roles: List[String], interactor: JudgeTaskTool): JudgeTaskMode =
    JudgeTaskMode("interactive", None, roles, Some(interactor))

  given Encoder[JudgeTaskMode] = deriveEncoder[JudgeTaskMode]
  given Decoder[JudgeTaskMode] = deriveDecoder[JudgeTaskMode]
