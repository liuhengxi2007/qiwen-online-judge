package domains.problem.objects

final case class ProblemDataTreeNode(
  path: ProblemDataPath,
  kind: ProblemDataTreeNodeKind,
  sizeBytes: Option[Long]
)
