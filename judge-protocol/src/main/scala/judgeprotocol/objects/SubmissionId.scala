package judgeprotocol.objects

import io.circe.{Decoder, Encoder}

/** backend 中提交记录的正整数标识，worker 用它回报判题结果。 */
final case class SubmissionId(value: Long)

/** 负责提交标识的协议编解码，并拒绝非正数。 */
object SubmissionId:
  given Encoder[SubmissionId] = Encoder.encodeLong.contramap(_.value)
  given Decoder[SubmissionId] = Decoder.decodeLong.emap { value =>
    if value < 1 then Left("Submission id is invalid.") else Right(SubmissionId(value))
  }
