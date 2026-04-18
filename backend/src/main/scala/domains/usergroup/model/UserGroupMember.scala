package domains.usergroup.model

import domains.auth.model.{DisplayName, UserPreferences, Username}
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

import java.time.Instant
import scala.util.Try

final case class UserGroupMember(
  username: Username,
  displayName: DisplayName,
  preferences: UserPreferences,
  role: UserGroupRole,
  joinedAt: Instant
)

object UserGroupMember:
  given Encoder[Instant] = Encoder.encodeString.contramap(_.toString)
  given Decoder[Instant] = Decoder.decodeString.emap { value =>
    Try(Instant.parse(value)).toEither.left.map(_.getMessage)
  }

  given Encoder[UserGroupMember] = deriveEncoder[UserGroupMember]
  given Decoder[UserGroupMember] = deriveDecoder[UserGroupMember]
