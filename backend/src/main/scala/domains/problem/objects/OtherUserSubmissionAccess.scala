package domains.problem.objects

import io.circe.{Decoder, Encoder}


/** 题目对非本人提交的可见级别；用于控制提交列表摘要和详情是否对其他用户开放。 */
enum OtherUserSubmissionAccess:
  case None
  case Summary
  case Detail

/** 非本人提交可见级别的 JSON/数据库字符串编解码器。 */
object OtherUserSubmissionAccess:
  given Encoder[OtherUserSubmissionAccess] = Encoder.encodeString.contramap(encode)
  given Decoder[OtherUserSubmissionAccess] = Decoder.decodeString.emap(parse)

  /** 将外部字符串解析为访问级别；未知值返回业务错误而不是抛异常。 */
  def parse(raw: String): Either[String, OtherUserSubmissionAccess] =
    raw.trim match
      case "none" => Right(OtherUserSubmissionAccess.None)
      case "summary" => Right(OtherUserSubmissionAccess.Summary)
      case "detail" => Right(OtherUserSubmissionAccess.Detail)
      case _ => Left("Other-user submission access must be one of: none, summary, detail.")

  private def encode(value: OtherUserSubmissionAccess): String =
    value match
      case OtherUserSubmissionAccess.None => "none"
      case OtherUserSubmissionAccess.Summary => "summary"
      case OtherUserSubmissionAccess.Detail => "detail"
