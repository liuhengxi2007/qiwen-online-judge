package domains.problem.model



enum ProblemDataTreeNodeKind:
  case File
  case Directory

final case class ProblemDataTreeNode(
  path: ProblemDataPath,
  kind: ProblemDataTreeNodeKind,
  sizeBytes: Option[Long]
)
