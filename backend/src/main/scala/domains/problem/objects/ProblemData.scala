package domains.problem.objects

import io.circe.{Decoder, Encoder}


/** 题目数据摘要文件名；None 表示当前题目没有登记可展示的数据文件。 */
final case class ProblemData(value: Option[ProblemDataFilename])

/** 题目数据摘要字段的 JSON 编解码与规范化入口。 */
object ProblemData:
  given Encoder[ProblemData] = Encoder.encodeOption[ProblemDataFilename].contramap(_.value)
  given Decoder[ProblemData] = Decoder.decodeOption[String].emap(parse)

  /** 将可空数据库/JSON 字符串解析为题目数据摘要，空白字符串会被视为无数据。 */
  def parse(raw: Option[String]): Either[String, ProblemData] =
    raw match
      case None => Right(ProblemData(None))
      case Some(value) =>
        val normalized = value.trim
        if normalized.isEmpty then Right(ProblemData(None))
        else ProblemDataFilename.parse(normalized).map(filename => ProblemData(Some(filename)))
