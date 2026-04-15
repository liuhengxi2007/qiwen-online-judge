package objects

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

import java.time.Instant
import java.util.UUID
import scala.util.Try

final case class NoteId(value: UUID)

object NoteId:
  def random(): NoteId = NoteId(UUID.randomUUID())

  given Encoder[NoteId] = Encoder.encodeString.contramap(_.value.toString)

  given Decoder[NoteId] = Decoder.decodeString.emap { value =>
    Try(UUID.fromString(value)).toEither.left.map(_.getMessage).map(NoteId(_))
  }

final case class NoteTitle(value: String)

object NoteTitle:
  given Encoder[NoteTitle] = Encoder.encodeString.contramap(_.value)
  given Decoder[NoteTitle] = Decoder.decodeString.map(NoteTitle(_))

final case class NoteBody(value: String)

object NoteBody:
  given Encoder[NoteBody] = Encoder.encodeString.contramap(_.value)
  given Decoder[NoteBody] = Decoder.decodeString.map(NoteBody(_))

enum NoteStatus:
  case Draft
  case Published

object NoteStatus:

  def toDatabase(status: NoteStatus): String =
    status match
      case NoteStatus.Draft => "draft"
      case NoteStatus.Published => "published"

  def fromDatabase(value: String): Either[String, NoteStatus] =
    value.trim.toLowerCase match
      case "draft" => Right(NoteStatus.Draft)
      case "published" => Right(NoteStatus.Published)
      case other => Left(s"Unsupported NoteStatus value: $other")

  def fromDatabaseUnsafe(value: String): NoteStatus =
    fromDatabase(value).fold(message => throw new IllegalArgumentException(message), identity)

  given Encoder[NoteStatus] = Encoder.encodeString.contramap(toDatabase)
  given Decoder[NoteStatus] = Decoder.decodeString.emap(fromDatabase)

final case class DemoNote(
  id: NoteId,
  title: NoteTitle,
  body: NoteBody,
  status: NoteStatus,
  createdAt: Instant
)

object DemoNote:
  given Encoder[Instant] = Encoder.encodeString.contramap(_.toString)
  given Decoder[Instant] = Decoder.decodeString.emap { value =>
    Try(Instant.parse(value)).toEither.left.map(_.getMessage)
  }

  given Encoder[DemoNote] = deriveEncoder[DemoNote]
  given Decoder[DemoNote] = deriveDecoder[DemoNote]
