package domains.problem.objects.response

import domains.problem.objects.*
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

/** 旧版题目数据文件列表响应；只包含根级文件名列表。 */
final case class ProblemDataFileListResponse(items: List[ProblemDataFilename])

/** ProblemDataFileListResponse 的 JSON 编解码器。 */
object ProblemDataFileListResponse:
  given Encoder[ProblemDataFileListResponse] = deriveEncoder[ProblemDataFileListResponse]
  given Decoder[ProblemDataFileListResponse] = deriveDecoder[ProblemDataFileListResponse]
