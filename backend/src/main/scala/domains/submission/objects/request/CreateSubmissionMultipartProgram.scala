package domains.submission.objects.request

import domains.submission.objects.SubmissionLanguage
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

/** multipart 提交中的程序元数据；sourcePart 指向 multipart 表单中的源码字段。role 和 sourcePart 是动态 multipart 协议字段，后端按字符串校验去重，不是固定状态枚举。 */
final case class CreateSubmissionMultipartProgram(
  role: String,
  language: SubmissionLanguage,
  sourcePart: String
)

/** CreateSubmissionMultipartProgram 的 JSON 编解码器。 */
object CreateSubmissionMultipartProgram:
  given Encoder[CreateSubmissionMultipartProgram] = deriveEncoder[CreateSubmissionMultipartProgram]
  given Decoder[CreateSubmissionMultipartProgram] = deriveDecoder[CreateSubmissionMultipartProgram]
