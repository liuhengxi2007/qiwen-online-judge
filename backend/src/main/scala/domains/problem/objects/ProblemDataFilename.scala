package domains.problem.objects

import io.circe.{Decoder, Encoder}


/** 题目数据的单个文件名，不允许包含目录分隔符。 */
final case class ProblemDataFilename(value: String)

/** 题目数据文件名的 JSON 编解码与校验入口。 */
object ProblemDataFilename:
  given Encoder[ProblemDataFilename] = Encoder.encodeString.contramap(_.value)
  given Decoder[ProblemDataFilename] = Decoder.decodeString.emap(parse)

  /** 校验并解析文件名；会复用 ProblemDataPath 的路径安全规则并额外拒绝目录。 */
  def parse(raw: String): Either[String, ProblemDataFilename] =
    ProblemDataPath.parse(raw).flatMap { path =>
      if path.value.contains('/') then Left("Problem data file name must not contain directory separators.")
      else Right(ProblemDataFilename(path.value))
    }
