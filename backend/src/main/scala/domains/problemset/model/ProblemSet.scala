package domains.problemset.model

import domains.shared.model.{PageResponse, ResourceStatus, ResourceVisibility}
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

import java.time.Instant
import java.util.UUID
import scala.util.Try

final case class ProblemSetId(value: UUID)

object ProblemSetId:
  def random(): ProblemSetId = ProblemSetId(UUID.randomUUID())

  given Encoder[ProblemSetId] = Encoder.encodeString.contramap(_.value.toString)
  given Decoder[ProblemSetId] = Decoder.decodeString.emap { value =>
    Try(UUID.fromString(value)).toEither.left.map(_.getMessage).map(ProblemSetId(_))
  }

final case class ProblemSetSlug(value: String)

object ProblemSetSlug:
  private val slugPattern = "^[a-z0-9]+(?:-[a-z0-9]+)*$".r

  def parse(raw: String): Either[String, ProblemSetSlug] =
    val normalized = raw.trim
    if normalized.isEmpty then Left("Problem set slug is required.")
    else if normalized.length < 3 || normalized.length > 64 then Left("Problem set slug must be between 3 and 64 characters.")
    else if !slugPattern.matches(normalized) then Left("Problem set slug may contain only lowercase letters, numbers, and hyphens.")
    else Right(ProblemSetSlug(normalized))

  def unsafe(raw: String): ProblemSetSlug =
    parse(raw).fold(message => throw IllegalStateException(s"Invalid problem set slug: $message"), identity)

  given Encoder[ProblemSetSlug] = Encoder.encodeString.contramap(_.value)
  given Decoder[ProblemSetSlug] = Decoder.decodeString.emap(parse)

final case class ProblemSetTitle(value: String)

object ProblemSetTitle:
  def parse(raw: String): Either[String, ProblemSetTitle] =
    val normalized = raw.trim
    if normalized.isEmpty then Left("Problem set title is required.")
    else if normalized.length > 120 then Left("Problem set title must be at most 120 characters.")
    else Right(ProblemSetTitle(normalized))

  def unsafe(raw: String): ProblemSetTitle =
    parse(raw).fold(message => throw IllegalStateException(s"Invalid problem set title: $message"), identity)

  given Encoder[ProblemSetTitle] = Encoder.encodeString.contramap(_.value)
  given Decoder[ProblemSetTitle] = Decoder.decodeString.emap(parse)

final case class ProblemSetDescription(value: String)

object ProblemSetDescription:
  def parse(raw: String): Either[String, ProblemSetDescription] =
    val normalized = raw.trim
    if normalized.length > 2000 then Left("Problem set description must be at most 2000 characters.")
    else Right(ProblemSetDescription(normalized))

  def unsafe(raw: String): ProblemSetDescription =
    parse(raw).fold(message => throw IllegalStateException(s"Invalid problem set description: $message"), identity)

  given Encoder[ProblemSetDescription] = Encoder.encodeString.contramap(_.value)
  given Decoder[ProblemSetDescription] = Decoder.decodeString.emap(parse)

final case class ProblemSetProblem(
  id: domains.problem.model.ProblemId,
  slug: domains.problem.model.ProblemSlug,
  title: domains.problem.model.ProblemTitle,
  position: Int
)

final case class ProblemSetSummaryView(
  id: ProblemSetId,
  slug: ProblemSetSlug,
  title: ProblemSetTitle,
  description: ProblemSetDescription,
  visibility: ResourceVisibility,
  status: ResourceStatus,
  ownerUsername: domains.auth.model.Username,
  createdAt: Instant,
  updatedAt: Instant
)

final case class ProblemSet(
  id: ProblemSetId,
  slug: ProblemSetSlug,
  title: ProblemSetTitle,
  description: ProblemSetDescription,
  problems: List[ProblemSetProblem],
  visibility: ResourceVisibility,
  status: ResourceStatus,
  ownerUsername: domains.auth.model.Username,
  createdAt: Instant,
  updatedAt: Instant
)

final case class CreateProblemSetRequest(
  slug: ProblemSetSlug,
  title: ProblemSetTitle,
  description: ProblemSetDescription,
  visibility: ResourceVisibility
)

object CreateProblemSetRequest:
  given Encoder[CreateProblemSetRequest] = deriveEncoder[CreateProblemSetRequest]
  given Decoder[CreateProblemSetRequest] = deriveDecoder[CreateProblemSetRequest]

final case class AddProblemToProblemSetRequest(
  problemSlug: domains.problem.model.ProblemSlug
)

object AddProblemToProblemSetRequest:
  given Encoder[AddProblemToProblemSetRequest] = deriveEncoder[AddProblemToProblemSetRequest]
  given Decoder[AddProblemToProblemSetRequest] = deriveDecoder[AddProblemToProblemSetRequest]

final case class UpdateProblemSetRequest(
  title: ProblemSetTitle,
  description: ProblemSetDescription,
  visibility: ResourceVisibility
)

object UpdateProblemSetRequest:
  given Encoder[UpdateProblemSetRequest] = deriveEncoder[UpdateProblemSetRequest]
  given Decoder[UpdateProblemSetRequest] = deriveDecoder[UpdateProblemSetRequest]

final case class ProblemSetProblemSummary(
  id: domains.problem.model.ProblemId,
  slug: domains.problem.model.ProblemSlug,
  title: domains.problem.model.ProblemTitle,
  position: Int
)

object ProblemSetProblemSummary:
  given Encoder[ProblemSetProblemSummary] = deriveEncoder[ProblemSetProblemSummary]
  given Decoder[ProblemSetProblemSummary] = deriveDecoder[ProblemSetProblemSummary]

final case class ProblemSetSummary(
  id: ProblemSetId,
  slug: ProblemSetSlug,
  title: ProblemSetTitle,
  description: ProblemSetDescription,
  visibility: ResourceVisibility,
  status: ResourceStatus,
  ownerUsername: domains.auth.model.Username,
  createdAt: Instant,
  updatedAt: Instant
)

object ProblemSetSummary:
  given Encoder[Instant] = Encoder.encodeString.contramap(_.toString)
  given Decoder[Instant] = Decoder.decodeString.emap { value =>
    Try(Instant.parse(value)).toEither.left.map(_.getMessage)
  }

  given Encoder[ProblemSetSummary] = deriveEncoder[ProblemSetSummary]
  given Decoder[ProblemSetSummary] = deriveDecoder[ProblemSetSummary]

final case class ProblemSetDetail(
  id: ProblemSetId,
  slug: ProblemSetSlug,
  title: ProblemSetTitle,
  description: ProblemSetDescription,
  problems: List[ProblemSetProblemSummary],
  visibility: ResourceVisibility,
  status: ResourceStatus,
  ownerUsername: domains.auth.model.Username,
  createdAt: Instant,
  updatedAt: Instant
)

object ProblemSetDetail:
  given Encoder[Instant] = Encoder.encodeString.contramap(_.toString)
  given Decoder[Instant] = Decoder.decodeString.emap { value =>
    Try(Instant.parse(value)).toEither.left.map(_.getMessage)
  }

  given Encoder[ProblemSetDetail] = deriveEncoder[ProblemSetDetail]
  given Decoder[ProblemSetDetail] = deriveDecoder[ProblemSetDetail]

type ProblemSetListResponse = PageResponse[ProblemSetSummary]
