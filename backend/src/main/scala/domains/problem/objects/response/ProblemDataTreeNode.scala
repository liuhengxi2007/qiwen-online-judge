package domains.problem.objects.response

import domains.problem.objects.ProblemDataPath
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

/** 题目数据树节点；文件节点携带大小，目录节点 sizeBytes 为空。 */
final case class ProblemDataTreeNode(
  path: ProblemDataPath,
  kind: ProblemDataTreeNodeKind,
  sizeBytes: Option[Long]
)

/** ProblemDataTreeNode 的 JSON 编解码器。 */
object ProblemDataTreeNode:
  given Encoder[ProblemDataTreeNode] = deriveEncoder[ProblemDataTreeNode]
  given Decoder[ProblemDataTreeNode] = deriveDecoder[ProblemDataTreeNode]
