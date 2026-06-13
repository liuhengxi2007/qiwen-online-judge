package domains.judger.objects.response

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

import java.time.Instant
import scala.util.Try

/** 管理端 judger 列表项；展示注册前缀、实际 id、主机、进程和心跳时间。 */
final case class RegisteredJudgerListItem(
  judgerId: String,
  requestedPrefix: String,
  host: String,
  processId: Option[String],
  supportedLanguages: List[String],
  registeredAt: Instant,
  lastHeartbeatAt: Instant
)

/** RegisteredJudgerListItem 的 JSON 编解码器，Instant 以 ISO-8601 字符串表示。 */
object RegisteredJudgerListItem:
  private given Encoder[Instant] = Encoder.encodeString.contramap(_.toString)
  private given Decoder[Instant] = Decoder.decodeString.emap { value =>
    Try(Instant.parse(value)).toEither.left.map(_.getMessage)
  }

  given Encoder[RegisteredJudgerListItem] = deriveEncoder[RegisteredJudgerListItem]
  given Decoder[RegisteredJudgerListItem] = deriveDecoder[RegisteredJudgerListItem]
