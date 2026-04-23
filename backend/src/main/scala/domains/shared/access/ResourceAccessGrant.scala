package domains.shared.access

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

import java.time.Instant
import scala.util.Try

final case class ResourceAccessGrant(
  resourceKind: ResourceKind,
  resourceId: ResourceId,
  grantRole: GrantRole,
  subject: AccessSubject,
  createdAt: Instant
)

object ResourceAccessGrant:
  private given instantEncoder: Encoder[Instant] = Encoder.encodeString.contramap(_.toString)
  private given instantDecoder: Decoder[Instant] = Decoder.decodeString.emap { value =>
    Try(Instant.parse(value)).toEither.left.map(_.getMessage)
  }

  given Encoder[ResourceAccessGrant] = deriveEncoder[ResourceAccessGrant]
  given Decoder[ResourceAccessGrant] = deriveDecoder[ResourceAccessGrant]
