package domains.problem.model

import io.circe.{Decoder, Encoder}

final case class ProblemDataFilename(value: String)

object ProblemDataFilename:
  def parse(raw: String): Either[String, ProblemDataFilename] =
    ProblemDataPath.parse(raw).flatMap { path =>
      if path.value.contains('/') then Left("Problem data file name must not contain directory separators.")
      else Right(ProblemDataFilename(path.value))
    }

  given Encoder[ProblemDataFilename] = Encoder.encodeString.contramap(_.value)
  given Decoder[ProblemDataFilename] = Decoder.decodeString.emap(parse)
