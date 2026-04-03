package domains.problem.model

import domains.shared.model.{PageResponse, ResourceStatus, ResourceVisibility}
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

import java.time.Instant
import java.util.UUID
import scala.util.Try

final case class ProblemId(value: UUID)

object ProblemId:
  def random(): ProblemId = ProblemId(UUID.randomUUID())

  given Encoder[ProblemId] = Encoder.encodeString.contramap(_.value.toString)
  given Decoder[ProblemId] = Decoder.decodeString.emap { value =>
    Try(UUID.fromString(value)).toEither.left.map(_.getMessage).map(ProblemId(_))
  }

final case class ProblemSlug(value: String)

object ProblemSlug:
  private val slugPattern = "^[a-z0-9]+(?:-[a-z0-9]+)*$".r

  def parse(raw: String): Either[String, ProblemSlug] =
    val normalized = raw.trim
    if normalized.isEmpty then Left("Problem slug is required.")
    else if normalized.length < 3 || normalized.length > 64 then Left("Problem slug must be between 3 and 64 characters.")
    else if !slugPattern.matches(normalized) then Left("Problem slug may contain only lowercase letters, numbers, and hyphens.")
    else Right(ProblemSlug(normalized))

  def unsafe(raw: String): ProblemSlug =
    parse(raw).fold(message => throw IllegalStateException(s"Invalid problem slug: $message"), identity)

  given Encoder[ProblemSlug] = Encoder.encodeString.contramap(_.value)
  given Decoder[ProblemSlug] = Decoder.decodeString.emap(parse)

final case class ProblemTitle(value: String)

object ProblemTitle:
  def parse(raw: String): Either[String, ProblemTitle] =
    val normalized = raw.trim
    if normalized.isEmpty then Left("Problem title is required.")
    else if normalized.length > 120 then Left("Problem title must be at most 120 characters.")
    else Right(ProblemTitle(normalized))

  def unsafe(raw: String): ProblemTitle =
    parse(raw).fold(message => throw IllegalStateException(s"Invalid problem title: $message"), identity)

  given Encoder[ProblemTitle] = Encoder.encodeString.contramap(_.value)
  given Decoder[ProblemTitle] = Decoder.decodeString.emap(parse)

final case class ProblemStatementText(value: String)

object ProblemStatementText:
  def parse(raw: String): Either[String, ProblemStatementText] =
    val normalized = raw.trim
    if normalized.isEmpty then Left("Problem statement is required.")
    else if normalized.length > 20000 then Left("Problem statement must be at most 20000 characters.")
    else Right(ProblemStatementText(normalized))

  def unsafe(raw: String): ProblemStatementText =
    parse(raw).fold(message => throw IllegalStateException(s"Invalid problem statement: $message"), identity)

  given Encoder[ProblemStatementText] = Encoder.encodeString.contramap(_.value)
  given Decoder[ProblemStatementText] = Decoder.decodeString.emap(parse)

final case class ProblemSummary(
  id: ProblemId,
  slug: ProblemSlug,
  title: ProblemTitle,
  visibility: ResourceVisibility,
  status: ResourceStatus,
  ownerUsername: domains.auth.model.Username,
  createdAt: Instant,
  updatedAt: Instant
)

final case class Problem(
  id: ProblemId,
  slug: ProblemSlug,
  title: ProblemTitle,
  statement: ProblemStatementText,
  visibility: ResourceVisibility,
  status: ResourceStatus,
  ownerUsername: domains.auth.model.Username,
  createdAt: Instant,
  updatedAt: Instant
)

final case class CreateProblemRequest(
  slug: ProblemSlug,
  title: ProblemTitle,
  statement: ProblemStatementText,
  visibility: ResourceVisibility
)

object CreateProblemRequest:
  given Encoder[CreateProblemRequest] = deriveEncoder[CreateProblemRequest]
  given Decoder[CreateProblemRequest] = deriveDecoder[CreateProblemRequest]

final case class UpdateProblemRequest(
  title: ProblemTitle,
  statement: ProblemStatementText,
  visibility: ResourceVisibility
)

object UpdateProblemRequest:
  given Encoder[UpdateProblemRequest] = deriveEncoder[UpdateProblemRequest]
  given Decoder[UpdateProblemRequest] = deriveDecoder[UpdateProblemRequest]

final case class ProblemListItem(
  id: ProblemId,
  slug: ProblemSlug,
  title: ProblemTitle,
  visibility: ResourceVisibility,
  status: ResourceStatus,
  ownerUsername: domains.auth.model.Username,
  createdAt: Instant,
  updatedAt: Instant
)

object ProblemListItem:
  private given instantEncoder: Encoder[Instant] = Encoder.encodeString.contramap(_.toString)
  private given instantDecoder: Decoder[Instant] = Decoder.decodeString.emap { value =>
    Try(Instant.parse(value)).toEither.left.map(_.getMessage)
  }

  given Encoder[ProblemListItem] = deriveEncoder[ProblemListItem]
  given Decoder[ProblemListItem] = deriveDecoder[ProblemListItem]

final case class ProblemDetail(
  id: ProblemId,
  slug: ProblemSlug,
  title: ProblemTitle,
  statement: ProblemStatementText,
  visibility: ResourceVisibility,
  status: ResourceStatus,
  ownerUsername: domains.auth.model.Username,
  createdAt: Instant,
  updatedAt: Instant
)

object ProblemDetail:
  private given Encoder[Instant] = Encoder.encodeString.contramap(_.toString)
  private given Decoder[Instant] = Decoder.decodeString.emap { value =>
    Try(Instant.parse(value)).toEither.left.map(_.getMessage)
  }
  given Encoder[ProblemDetail] = deriveEncoder[ProblemDetail]
  given Decoder[ProblemDetail] = deriveDecoder[ProblemDetail]

type ProblemListResponse = PageResponse[ProblemListItem]
