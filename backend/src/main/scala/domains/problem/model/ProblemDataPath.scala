package domains.problem.model



import shared.upload.StoredFilePath
import io.circe.{Decoder, Encoder}

final case class ProblemDataPath(value: String):
  def fileName: String =
    value.split('/').lastOption.getOrElse(value)

  def toStoredFilePath: StoredFilePath =
    StoredFilePath(value)

object ProblemDataPath:
  def parse(raw: String): Either[String, ProblemDataPath] =
    StoredFilePath.parse(raw).map(path => ProblemDataPath(path.value))

  def fromFilename(filename: ProblemDataFilename): ProblemDataPath =
    ProblemDataPath(filename.value)

  given Encoder[ProblemDataPath] = Encoder.encodeString.contramap(_.value)
  given Decoder[ProblemDataPath] = Decoder.decodeString.emap(parse)
