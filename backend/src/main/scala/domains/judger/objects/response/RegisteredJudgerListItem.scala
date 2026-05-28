package domains.judger.objects.response

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

import java.time.Instant
import scala.util.Try

final case class RegisteredJudgerListItem(
  judgerId: String,
  requestedPrefix: String,
  host: String,
  processId: Option[String],
  supportedLanguages: List[String],
  registeredAt: Instant,
  lastHeartbeatAt: Instant
)

object RegisteredJudgerListItem:
  private given Encoder[Instant] = Encoder.encodeString.contramap(_.toString)
  private given Decoder[Instant] = Decoder.decodeString.emap { value =>
    Try(Instant.parse(value)).toEither.left.map(_.getMessage)
  }

  given Encoder[RegisteredJudgerListItem] = deriveEncoder[RegisteredJudgerListItem]
  given Decoder[RegisteredJudgerListItem] = deriveDecoder[RegisteredJudgerListItem]
