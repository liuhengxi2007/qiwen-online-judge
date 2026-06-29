package domains.submission.objects.request

import io.circe.{Decoder, Encoder}


/** 提交列表排序方向。 */
enum SubmissionSortDirection:
  case Asc
  case Desc

/** 排序方向的 JSON/query 字符串编解码器。 */
object SubmissionSortDirection:
  given Encoder[SubmissionSortDirection] = Encoder.encodeString.contramap(encode)
  given Decoder[SubmissionSortDirection] = Decoder.decodeString.emap(parse)

  /** 将外部字符串解析为排序方向。 */
  def parse(value: String): Either[String, SubmissionSortDirection] =
    value.trim match
      case "asc" => Right(SubmissionSortDirection.Asc)
      case "desc" => Right(SubmissionSortDirection.Desc)
      case _ => Left("Submission sort direction must be one of: asc, desc.")

  /** 将排序方向编码为字符串。 */
  def encode(value: SubmissionSortDirection): String =
    value match
      case SubmissionSortDirection.Asc => "asc"
      case SubmissionSortDirection.Desc => "desc"
