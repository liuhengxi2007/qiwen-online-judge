package domains.problem.objects

import io.circe.{Decoder, Encoder}

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
