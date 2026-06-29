package domains.problem.objects

import io.circe.{Decoder, Encoder}


import java.util.UUID
import scala.util.Try

/** 题目的内部 UUID 标识；用于数据库主键和跨域内部引用。 */
final case class ProblemId(value: UUID)

/** ProblemId 的 JSON 编解码器，外部形态为 UUID 字符串。 */
object ProblemId:
  given Encoder[ProblemId] = Encoder.encodeString.contramap(_.value.toString)
  given Decoder[ProblemId] = Decoder.decodeString.emap { value =>
    Try(UUID.fromString(value)).toEither.left.map(_.getMessage).map(ProblemId(_))
  }
