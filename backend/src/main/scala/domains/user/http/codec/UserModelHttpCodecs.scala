package domains.user.http.codec

import domains.problem.http.codec.ProblemModelHttpCodecs.given
import domains.problem.model.{ProblemSlug, ProblemTitle, ProblemTitleDisplayMode}
import domains.user.model.*
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

import java.time.Instant
import scala.util.Try

object UserModelHttpCodecs:
  private given Encoder[Instant] = Encoder.encodeString.contramap(_.toString)
  private given Decoder[Instant] = Decoder.decodeString.emap { value =>
    Try(Instant.parse(value)).toEither.left.map(_.getMessage)
  }

  given Encoder[Username] = Encoder.encodeString.contramap(_.value)
  given Decoder[Username] = Decoder.decodeString.map(Username.canonical)

  given Encoder[DisplayName] = Encoder.encodeString.contramap(_.value)
  given Decoder[DisplayName] = Decoder.decodeString.map(value => DisplayName(value))

  given Encoder[UserContribution] = Encoder.encodeBigDecimal.contramap(_.value)
  given Decoder[UserContribution] = Decoder.decodeBigDecimal.map(UserContribution(_))

  given Encoder[UserLocale] = Encoder.encodeString.contramap(encodeUserLocale)
  given Decoder[UserLocale] = Decoder.decodeString.emap(UserLocale.parse)

  given Encoder[UserDisplayMode] = Encoder.encodeString.contramap(encodeUserDisplayMode)
  given Decoder[UserDisplayMode] = Decoder.decodeString.emap(UserDisplayMode.parse)

  given Encoder[UserIdentity] = deriveEncoder[UserIdentity]
  given Decoder[UserIdentity] = deriveDecoder[UserIdentity]

  given Encoder[UserPreferences] = deriveEncoder[UserPreferences]
  given Decoder[UserPreferences] = deriveDecoder[UserPreferences]

  given Encoder[UserAcceptedProblem] = deriveEncoder[UserAcceptedProblem]
  given Decoder[UserAcceptedProblem] = deriveDecoder[UserAcceptedProblem]

  private def encodeUserLocale(value: UserLocale): String =
    value match
      case UserLocale.En => "en"
      case UserLocale.ZhCn => "zh-CN"

  private def encodeUserDisplayMode(value: UserDisplayMode): String =
    value match
      case UserDisplayMode.DisplayName => "display_name"
      case UserDisplayMode.Username => "username"
      case UserDisplayMode.DisplayNameWithUsername => "display_name_with_username"
