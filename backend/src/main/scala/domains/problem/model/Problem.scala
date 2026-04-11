package domains.problem.model

import domains.shared.access.ResourceAccessPolicy
import domains.shared.model.PageResponse
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

import java.time.Instant
import java.util.UUID
import java.util.Base64
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

  given Encoder[ProblemSlug] = Encoder.encodeString.contramap(_.value)
  given Decoder[ProblemSlug] = Decoder.decodeString.emap(parse)

final case class ProblemTitle(value: String)

object ProblemTitle:
  def parse(raw: String): Either[String, ProblemTitle] =
    val normalized = raw.trim
    if normalized.isEmpty then Left("Problem title is required.")
    else if normalized.length > 120 then Left("Problem title must be at most 120 characters.")
    else Right(ProblemTitle(normalized))

  given Encoder[ProblemTitle] = Encoder.encodeString.contramap(_.value)
  given Decoder[ProblemTitle] = Decoder.decodeString.emap(parse)

final case class ProblemStatementText(value: String)

object ProblemStatementText:
  def parse(raw: String): Either[String, ProblemStatementText] =
    val normalized = raw.trim
    if normalized.isEmpty then Left("Problem statement is required.")
    else if normalized.length > 20000 then Left("Problem statement must be at most 20000 characters.")
    else Right(ProblemStatementText(normalized))

  given Encoder[ProblemStatementText] = Encoder.encodeString.contramap(_.value)
  given Decoder[ProblemStatementText] = Decoder.decodeString.emap(parse)

final case class ProblemDataFilename(value: String)

object ProblemDataFilename:
  def parse(raw: String): Either[String, ProblemDataFilename] =
    val normalized = raw.trim
    if normalized.isEmpty then Left("Problem data file name is required.")
    else if normalized.length > 255 then Left("Problem data file name must be at most 255 characters.")
    else Right(ProblemDataFilename(normalized))

  given Encoder[ProblemDataFilename] = Encoder.encodeString.contramap(_.value)
  given Decoder[ProblemDataFilename] = Decoder.decodeString.emap(parse)

final case class ProblemData(value: Option[ProblemDataFilename])

object ProblemData:
  def parse(raw: Option[String]): Either[String, ProblemData] =
    raw match
      case None => Right(ProblemData(None))
      case Some(value) =>
        val normalized = value.trim
        if normalized.isEmpty then Right(ProblemData(None))
        else ProblemDataFilename.parse(normalized).map(filename => ProblemData(Some(filename)))

  given Encoder[ProblemData] = Encoder.encodeOption[ProblemDataFilename].contramap(_.value)
  given Decoder[ProblemData] = Decoder.decodeOption[String].emap(parse)

final case class ProblemTimeLimitMs(value: Int)

object ProblemTimeLimitMs:
  def parse(raw: Int): Either[String, ProblemTimeLimitMs] =
    if raw < 1 then Left("Problem time limit must be at least 1 ms.")
    else if raw > 600000 then Left("Problem time limit must be at most 600000 ms.")
    else Right(ProblemTimeLimitMs(raw))

  given Encoder[ProblemTimeLimitMs] = Encoder.encodeInt.contramap(_.value)
  given Decoder[ProblemTimeLimitMs] = Decoder.decodeInt.emap(parse)

final case class ProblemSpaceLimitMb(value: Int)

object ProblemSpaceLimitMb:
  def parse(raw: Int): Either[String, ProblemSpaceLimitMb] =
    if raw < 1 then Left("Problem space limit must be at least 1 MB.")
    else if raw > 65536 then Left("Problem space limit must be at most 65536 MB.")
    else Right(ProblemSpaceLimitMb(raw))

  given Encoder[ProblemSpaceLimitMb] = Encoder.encodeInt.contramap(_.value)
  given Decoder[ProblemSpaceLimitMb] = Decoder.decodeInt.emap(parse)

final case class ProblemSummary(
  id: ProblemId,
  slug: ProblemSlug,
  title: ProblemTitle,
  data: ProblemData,
  timeLimitMs: ProblemTimeLimitMs,
  spaceLimitMb: ProblemSpaceLimitMb,
  accessPolicy: ResourceAccessPolicy,
  creatorUsername: domains.auth.model.Username,
  createdAt: Instant,
  updatedAt: Instant
)

final case class ProblemDetail(
  id: ProblemId,
  slug: ProblemSlug,
  title: ProblemTitle,
  statement: ProblemStatementText,
  data: ProblemData,
  timeLimitMs: ProblemTimeLimitMs,
  spaceLimitMb: ProblemSpaceLimitMb,
  accessPolicy: ResourceAccessPolicy,
  creatorUsername: domains.auth.model.Username,
  canManage: Boolean,
  createdAt: Instant,
  updatedAt: Instant
)

final case class CreateProblemRequest(
  slug: ProblemSlug,
  title: ProblemTitle,
  statement: ProblemStatementText,
  timeLimitMs: ProblemTimeLimitMs,
  spaceLimitMb: ProblemSpaceLimitMb,
  accessPolicy: ResourceAccessPolicy
)

object CreateProblemRequest:
  given Encoder[CreateProblemRequest] = deriveEncoder[CreateProblemRequest]
  given Decoder[CreateProblemRequest] = deriveDecoder[CreateProblemRequest]

final case class UpdateProblemRequest(
  title: ProblemTitle,
  statement: ProblemStatementText,
  timeLimitMs: ProblemTimeLimitMs,
  spaceLimitMb: ProblemSpaceLimitMb,
  accessPolicy: ResourceAccessPolicy
)

object UpdateProblemRequest:
  given Encoder[UpdateProblemRequest] = deriveEncoder[UpdateProblemRequest]
  given Decoder[UpdateProblemRequest] = deriveDecoder[UpdateProblemRequest]

object ProblemSummary:
  private given instantEncoder: Encoder[Instant] = Encoder.encodeString.contramap(_.toString)
  private given instantDecoder: Decoder[Instant] = Decoder.decodeString.emap { value =>
    Try(Instant.parse(value)).toEither.left.map(_.getMessage)
  }

  given Encoder[ProblemSummary] = deriveEncoder[ProblemSummary]
  given Decoder[ProblemSummary] = deriveDecoder[ProblemSummary]

object ProblemDetail:
  private given Encoder[Instant] = Encoder.encodeString.contramap(_.toString)
  private given Decoder[Instant] = Decoder.decodeString.emap { value =>
    Try(Instant.parse(value)).toEither.left.map(_.getMessage)
  }
  given Encoder[ProblemDetail] = deriveEncoder[ProblemDetail]
  given Decoder[ProblemDetail] = deriveDecoder[ProblemDetail]

final case class UpdateProblemDataRequest(
  filename: ProblemDataFilename,
  contentBase64: String
)

object UpdateProblemDataRequest:
  given Encoder[UpdateProblemDataRequest] = deriveEncoder[UpdateProblemDataRequest]
  given Decoder[UpdateProblemDataRequest] = deriveDecoder[UpdateProblemDataRequest]

  extension (request: UpdateProblemDataRequest)
    def decodedBytes: Either[String, Array[Byte]] =
      Try(Base64.getDecoder.decode(request.contentBase64))
        .toEither
        .left
        .map(_ => "Problem data content is not valid base64.")

final case class ProblemDataFileListResponse(items: List[ProblemDataFilename])

object ProblemDataFileListResponse:
  given Encoder[ProblemDataFileListResponse] = deriveEncoder[ProblemDataFileListResponse]
  given Decoder[ProblemDataFileListResponse] = deriveDecoder[ProblemDataFileListResponse]

type ProblemListResponse = PageResponse[ProblemSummary]
