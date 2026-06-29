package domains.contest.objects

import io.circe.{Decoder, Encoder}

import java.util.UUID
import scala.util.Try

/** 比赛数据库主键的领域值，封装 UUID。 */
final case class ContestId(value: UUID)

/** 提供比赛 id 的字符串 JSON 编解码。 */
object ContestId:
  given Encoder[ContestId] = Encoder.encodeString.contramap(_.value.toString)
  given Decoder[ContestId] = Decoder.decodeString.emap { value =>
    Try(UUID.fromString(value)).toEither.left.map(_.getMessage).map(ContestId(_))
  }
