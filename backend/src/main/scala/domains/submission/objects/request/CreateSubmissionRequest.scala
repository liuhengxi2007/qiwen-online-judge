package domains.submission.objects.request

import domains.submission.objects.*

import domains.problem.objects.ProblemSlug
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

/** 创建提交的请求体；programs 按角色名保存语言和源码。 */
final case class CreateSubmissionRequest(
  problemSlug: ProblemSlug,
  programs: Map[String, CreateSubmissionRequest.Program]
)

/** CreateSubmissionRequest 的 JSON 编解码器和嵌套程序结构。 */
object CreateSubmissionRequest:
  /** 单个提交角色的语言和源码。 */
  final case class Program(
    language: SubmissionLanguage,
    sourceCode: SubmissionSourceCode
  )

  /** Program 的 JSON 编解码器。 */
  object Program:
    given Encoder[Program] = deriveEncoder[Program]
    given Decoder[Program] = deriveDecoder[Program]

  given Encoder[CreateSubmissionRequest] = deriveEncoder[CreateSubmissionRequest]
  given Decoder[CreateSubmissionRequest] = deriveDecoder[CreateSubmissionRequest]
