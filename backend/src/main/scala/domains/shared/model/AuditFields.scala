package domains.shared.model

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

import java.time.Instant

final case class AuditFields(createdAt: Instant, updatedAt: Instant)

object AuditFields:
  given Encoder[AuditFields] = deriveEncoder[AuditFields]
  given Decoder[AuditFields] = deriveDecoder[AuditFields]
