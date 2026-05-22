package domains.problem.model



import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

enum ProblemDataTreeNodeKind:
  case File
  case Directory

object ProblemDataTreeNodeKind:
  given Encoder[ProblemDataTreeNodeKind] = Encoder.encodeString.contramap {
    case ProblemDataTreeNodeKind.File => "file"
    case ProblemDataTreeNodeKind.Directory => "directory"
  }

  given Decoder[ProblemDataTreeNodeKind] = Decoder.decodeString.emap {
    case "file" => Right(ProblemDataTreeNodeKind.File)
    case "directory" => Right(ProblemDataTreeNodeKind.Directory)
    case other => Left(s"Unsupported problem data tree node kind: $other")
  }

final case class ProblemDataTreeNode(
  path: ProblemDataPath,
  kind: ProblemDataTreeNodeKind,
  sizeBytes: Option[Long]
)

object ProblemDataTreeNode:
  given Encoder[ProblemDataTreeNode] = deriveEncoder[ProblemDataTreeNode]
  given Decoder[ProblemDataTreeNode] = deriveDecoder[ProblemDataTreeNode]
