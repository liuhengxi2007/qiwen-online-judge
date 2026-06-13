package judgeprotocol.objects.response

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import judgeprotocol.objects.SubmissionLanguage

/** hack 答案生成器配置，指向标准程序源文件及其语言。 */
final case class JudgeTaskStandard(
  language: SubmissionLanguage,
  source: JudgeTaskFileRef
)

/** 负责标准程序配置的协议编解码。 */
object JudgeTaskStandard:
  given Encoder[JudgeTaskStandard] = deriveEncoder[JudgeTaskStandard]
  given Decoder[JudgeTaskStandard] = deriveDecoder[JudgeTaskStandard]
