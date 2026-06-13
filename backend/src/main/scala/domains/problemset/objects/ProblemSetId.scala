package domains.problemset.objects

import io.circe.{Decoder, Encoder}


import java.util.UUID
import scala.util.Try

/** 题单数据库主键领域值，封装 UUID。 */
final case class ProblemSetId(value: UUID)

/** 提供题单 id 的字符串 JSON 编解码。 */
object ProblemSetId:
  given Encoder[ProblemSetId] = Encoder.encodeString.contramap(_.value.toString)
  given Decoder[ProblemSetId] = Decoder.decodeString.emap { value =>
    Try(UUID.fromString(value)).toEither.left.map(_.getMessage).map(ProblemSetId(_))
  }
