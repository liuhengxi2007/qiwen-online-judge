package domains.problem.objects.request

import domains.problem.objects.*
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

/** 删除题目数据路径的请求体；path 可以指向单文件或目录前缀。 */
final case class DeleteProblemDataPathRequest(path: ProblemDataPath)

/** DeleteProblemDataPathRequest 的 JSON 编解码器。 */
object DeleteProblemDataPathRequest:
  given Encoder[DeleteProblemDataPathRequest] = deriveEncoder[DeleteProblemDataPathRequest]
  given Decoder[DeleteProblemDataPathRequest] = deriveDecoder[DeleteProblemDataPathRequest]
