package domains.problem.http.codec

import domains.problem.model.*
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

import java.util.UUID
import scala.util.Try

object ProblemModelHttpCodecs:
  given Encoder[ProblemId] = Encoder.encodeString.contramap(_.value.toString)
  given Decoder[ProblemId] = Decoder.decodeString.emap { value =>
    Try(UUID.fromString(value)).toEither.left.map(_.getMessage).map(ProblemId(_))
  }

  given Encoder[ProblemSlug] = Encoder.encodeString.contramap(_.value)
  given Decoder[ProblemSlug] = Decoder.decodeString.emap(ProblemSlug.parse)

  given Encoder[ProblemTitle] = Encoder.encodeString.contramap(_.value)
  given Decoder[ProblemTitle] = Decoder.decodeString.emap(ProblemTitle.parse)

  given Encoder[ProblemStatementText] = Encoder.encodeString.contramap(_.value)
  given Decoder[ProblemStatementText] = Decoder.decodeString.emap(ProblemStatementText.parse)

  given Encoder[ProblemTimeLimitMs] = Encoder.encodeInt.contramap(_.value)
  given Decoder[ProblemTimeLimitMs] = Decoder.decodeInt.emap(ProblemTimeLimitMs.parse)

  given Encoder[ProblemSpaceLimitMb] = Encoder.encodeInt.contramap(_.value)
  given Decoder[ProblemSpaceLimitMb] = Decoder.decodeInt.emap(ProblemSpaceLimitMb.parse)

  given Encoder[ProblemTitleDisplayMode] = Encoder.encodeString.contramap(ProblemTitleDisplayMode.toDatabase)
  given Decoder[ProblemTitleDisplayMode] = Decoder.decodeString.emap(ProblemTitleDisplayMode.parse)

  given Encoder[OthersSubmissionAccess] = Encoder.encodeString.contramap(OthersSubmissionAccess.toDatabase)
  given Decoder[OthersSubmissionAccess] = Decoder.decodeString.emap(OthersSubmissionAccess.parse)

  given Encoder[ProblemDataFilename] = Encoder.encodeString.contramap(_.value)
  given Decoder[ProblemDataFilename] = Decoder.decodeString.emap(ProblemDataFilename.parse)

  given Encoder[ProblemDataPath] = Encoder.encodeString.contramap(_.value)
  given Decoder[ProblemDataPath] = Decoder.decodeString.emap(ProblemDataPath.parse)

  given Encoder[ProblemData] = Encoder.encodeOption[ProblemDataFilename].contramap(_.value)
  given Decoder[ProblemData] = Decoder.decodeOption[String].emap(ProblemData.parse)

  given Encoder[ProblemDataTreeNodeKind] = Encoder.encodeString.contramap {
    case ProblemDataTreeNodeKind.File => "file"
    case ProblemDataTreeNodeKind.Directory => "directory"
  }

  given Decoder[ProblemDataTreeNodeKind] = Decoder.decodeString.emap {
    case "file" => Right(ProblemDataTreeNodeKind.File)
    case "directory" => Right(ProblemDataTreeNodeKind.Directory)
    case other => Left(s"Unsupported problem data tree node kind: $other")
  }

  given Encoder[ProblemDataTreeNode] = deriveEncoder[ProblemDataTreeNode]
  given Decoder[ProblemDataTreeNode] = deriveDecoder[ProblemDataTreeNode]
