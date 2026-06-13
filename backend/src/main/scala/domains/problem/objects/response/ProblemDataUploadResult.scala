package domains.problem.objects.response

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

/** 题目数据上传响应；返回刷新后的题目详情和本次成功写入的文件数量。 */
final case class ProblemDataUploadResult(
  problem: ProblemDetail,
  uploadedFileCount: Int
)

/** ProblemDataUploadResult 的 JSON 编解码器。 */
object ProblemDataUploadResult:
  given Encoder[ProblemDataUploadResult] = deriveEncoder[ProblemDataUploadResult]
  given Decoder[ProblemDataUploadResult] = deriveDecoder[ProblemDataUploadResult]
