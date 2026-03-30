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
  given Encoder[ProblemSetSlug] = Encoder.encodeString.contramap(_.value)
  given Decoder[ProblemSetSlug] = Decoder.decodeString.map(ProblemSetSlug(_))

final case class ProblemSetTitle(value: String)

object ProblemSetTitle:
  given Encoder[ProblemSetTitle] = Encoder.encodeString.contramap(_.value)
  given Decoder[ProblemSetTitle] = Decoder.decodeString.map(ProblemSetTitle(_))

final case class ProblemSetDescription(value: String)

object ProblemSetDescription:
  given Encoder[ProblemSetDescription] = Encoder.encodeString.contramap(_.value)
  given Decoder[ProblemSetDescription] = Decoder.decodeString.map(ProblemSetDescription(_))

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
