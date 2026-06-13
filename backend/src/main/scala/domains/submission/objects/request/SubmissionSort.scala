package domains.submission.objects.request

import io.circe.{Decoder, Encoder}


/** 提交列表排序字段。 */
enum SubmissionSort:
  case Submitted
  case Time
  case Memory
  case CodeLength

/** 提交列表排序字段的 JSON 字符串编解码与默认方向规则。 */
object SubmissionSort:
  given Encoder[SubmissionSort] = Encoder.encodeString.contramap(encode)
  given Decoder[SubmissionSort] = Decoder.decodeString.emap(parse)

  /** 将外部字符串解析为排序字段。 */
  def parse(value: String): Either[String, SubmissionSort] =
    value.trim match
      case "submitted" => Right(SubmissionSort.Submitted)
      case "time" => Right(SubmissionSort.Time)
      case "memory" => Right(SubmissionSort.Memory)
      case "code_length" => Right(SubmissionSort.CodeLength)
      case _ =>
        Left("Submission sort must be one of: submitted, time, memory, code_length.")

  /** 将排序字段编码为 query/JSON 字符串。 */
  def encode(value: SubmissionSort): String =
    value match
      case SubmissionSort.Submitted => "submitted"
      case SubmissionSort.Time => "time"
      case SubmissionSort.Memory => "memory"
      case SubmissionSort.CodeLength => "code_length"

  /** 为排序字段选择默认方向；提交时间默认倒序，其它数值默认正序。 */
  def defaultDirection(value: SubmissionSort): SubmissionSortDirection =
    value match
      case SubmissionSort.Submitted => SubmissionSortDirection.Desc
      case SubmissionSort.Time => SubmissionSortDirection.Asc
      case SubmissionSort.Memory => SubmissionSortDirection.Asc
      case SubmissionSort.CodeLength => SubmissionSortDirection.Asc
