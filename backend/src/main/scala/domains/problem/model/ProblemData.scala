package domains.problem.model



import io.circe.{Decoder, Encoder}

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
