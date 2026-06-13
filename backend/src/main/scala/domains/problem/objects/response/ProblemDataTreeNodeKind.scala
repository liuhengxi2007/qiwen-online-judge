package domains.problem.objects.response

import io.circe.{Decoder, Encoder}

/** 题目数据树节点类型；区分实际文件和由路径推导出的目录。 */
enum ProblemDataTreeNodeKind:
  case File
  case Directory

/** ProblemDataTreeNodeKind 的 JSON 字符串编解码器。 */
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
