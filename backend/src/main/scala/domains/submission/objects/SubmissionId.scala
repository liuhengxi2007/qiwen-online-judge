package domains.submission.objects

import io.circe.{Decoder, Encoder}


import scala.util.Try

/** 提交公开 id；用于 URL 和对外 API，数据库内部仍有 UUID 主键。 */
final case class SubmissionId(value: Long)

/** SubmissionId 的 JSON 编解码与路径参数解析入口。 */
object SubmissionId:
  given Encoder[SubmissionId] = Encoder.encodeLong.contramap(_.value)
  given Decoder[SubmissionId] = Decoder.decodeLong.emap { value =>
    if value < 1 then Left("Submission id is invalid.") else Right(SubmissionId(value))
  }

  /** 从字符串解析正整数提交 id；非法值返回业务错误。 */
  def parse(raw: String): Either[String, SubmissionId] =
    Try(raw.trim.toLong)
      .toEither
      .left
      .map(_ => "Submission id is invalid.")
      .flatMap { value =>
        if value < 1 then Left("Submission id is invalid.")
        else Right(SubmissionId(value))
      }
