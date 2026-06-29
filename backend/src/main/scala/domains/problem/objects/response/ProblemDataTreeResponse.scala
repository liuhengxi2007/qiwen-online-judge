package domains.problem.objects.response

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

/** 题目数据树响应；items 已经包含目录和文件节点，由后端排序生成。 */
final case class ProblemDataTreeResponse(items: List[ProblemDataTreeNode])

/** ProblemDataTreeResponse 的 JSON 编解码器。 */
object ProblemDataTreeResponse:
  given Encoder[ProblemDataTreeResponse] = deriveEncoder[ProblemDataTreeResponse]
  given Decoder[ProblemDataTreeResponse] = deriveDecoder[ProblemDataTreeResponse]
