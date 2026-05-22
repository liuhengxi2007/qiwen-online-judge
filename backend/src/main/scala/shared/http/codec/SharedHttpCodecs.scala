package shared.http.codec

import shared.model.*
import shared.upload.StoredFilePath
import io.circe.{Decoder, DecodingFailure, Encoder, Json}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

import java.time.Instant
import scala.util.Try

object SharedHttpCodecs:
  private given Encoder[Instant] = Encoder.encodeString.contramap(_.toString)
  private given Decoder[Instant] = Decoder.decodeString.emap { value =>
    Try(Instant.parse(value)).toEither.left.map(_.getMessage)
  }

  given Encoder[ApiMessageParam] = Encoder.instance {
    case ApiMessageParam.Text(value) =>
      Json.obj("kind" -> Json.fromString("text"), "value" -> Json.fromString(value))
    case ApiMessageParam.IntValue(value) =>
      Json.obj("kind" -> Json.fromString("int"), "value" -> Json.fromInt(value))
    case ApiMessageParam.LongValue(value) =>
      Json.obj("kind" -> Json.fromString("long"), "value" -> Json.fromLong(value))
    case ApiMessageParam.BoolValue(value) =>
      Json.obj("kind" -> Json.fromString("bool"), "value" -> Json.fromBoolean(value))
  }

  given Decoder[ApiMessageParam] = Decoder.instance { cursor =>
    cursor.downField("kind").as[String].flatMap {
      case "text" => cursor.downField("value").as[String].map(ApiMessageParam.Text(_))
      case "int" => cursor.downField("value").as[Int].map(ApiMessageParam.IntValue(_))
      case "long" => cursor.downField("value").as[Long].map(ApiMessageParam.LongValue(_))
      case "bool" => cursor.downField("value").as[Boolean].map(ApiMessageParam.BoolValue(_))
      case other => Left(DecodingFailure(s"Unsupported ApiMessageParam kind: $other", cursor.history))
    }
  }

  given Encoder[AuditFields] = deriveEncoder[AuditFields]
  given Decoder[AuditFields] = deriveDecoder[AuditFields]

  given Encoder[PageRequest] = deriveEncoder[PageRequest]
  given Decoder[PageRequest] = deriveDecoder[PageRequest]
  given [A: Encoder]: Encoder[PageResponse[A]] = deriveEncoder[PageResponse[A]]
  given [A: Decoder]: Decoder[PageResponse[A]] = deriveDecoder[PageResponse[A]]

  given Encoder[ResourceVisibility] = Encoder.encodeString.contramap(ResourceVisibility.toDatabase)
  given Decoder[ResourceVisibility] = Decoder.decodeString.emap { value =>
    ResourceVisibility.fromDatabase(value).toRight(s"Unknown resource visibility: $value")
  }

  given Encoder[StoredFilePath] = Encoder.encodeString.contramap(_.value)
  given Decoder[StoredFilePath] = Decoder.decodeString.emap(StoredFilePath.parse)
