package domains.problem.objects

import io.circe.{Decoder, Encoder}


import shared.application.upload.StoredFilePath

final case class ProblemDataPath(value: String):
  def fileName: String =
    value.split('/').lastOption.getOrElse(value)

  def toStoredFilePath: StoredFilePath =
    StoredFilePath(value)

object ProblemDataPath:
  given Encoder[ProblemDataPath] = Encoder.encodeString.contramap(_.value)
  given Decoder[ProblemDataPath] = Decoder.decodeString.emap(parse)

  def parse(raw: String): Either[String, ProblemDataPath] =
    StoredFilePath.parse(raw).map(path => ProblemDataPath(path.value))

  def fromFilename(filename: ProblemDataFilename): ProblemDataPath =
    ProblemDataPath(filename.value)
