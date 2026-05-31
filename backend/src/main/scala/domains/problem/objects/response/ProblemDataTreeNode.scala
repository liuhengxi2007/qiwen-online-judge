package domains.problem.objects.response

import domains.problem.objects.ProblemDataPath
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class ProblemDataTreeNode(
  path: ProblemDataPath,
  kind: ProblemDataTreeNodeKind,
  sizeBytes: Option[Long]
)

object ProblemDataTreeNode:
  given Encoder[ProblemDataTreeNode] = deriveEncoder[ProblemDataTreeNode]
  given Decoder[ProblemDataTreeNode] = deriveDecoder[ProblemDataTreeNode]
