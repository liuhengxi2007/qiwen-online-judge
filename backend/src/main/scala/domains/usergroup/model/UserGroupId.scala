package domains.usergroup.model



import io.circe.{Decoder, Encoder}

import java.util.UUID
import scala.util.Try

final case class UserGroupId(value: UUID)

object UserGroupId:

  given Encoder[UserGroupId] = Encoder.encodeString.contramap(_.value.toString)
  given Decoder[UserGroupId] = Decoder.decodeString.emap { value =>
    Try(UUID.fromString(value)).toEither.left.map(_.getMessage).map(UserGroupId(_))
  }
