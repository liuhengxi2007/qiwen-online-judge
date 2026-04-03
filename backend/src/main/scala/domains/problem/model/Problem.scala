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
  given Encoder[ProblemSlug] = Encoder.encodeString.contramap(_.value)
  given Decoder[ProblemSlug] = Decoder.decodeString.map(ProblemSlug(_))

final case class ProblemTitle(value: String)

object ProblemTitle:
  given Encoder[ProblemTitle] = Encoder.encodeString.contramap(_.value)
  given Decoder[ProblemTitle] = Decoder.decodeString.map(ProblemTitle(_))

final case class ProblemStatementText(value: String)

object ProblemStatementText:
  given Encoder[ProblemStatementText] = Encoder.encodeString.contramap(_.value)
  given Decoder[ProblemStatementText] = Decoder.decodeString.map(ProblemStatementText(_))

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
