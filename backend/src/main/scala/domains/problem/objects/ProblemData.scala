package domains.problem.objects



final case class ProblemData(value: Option[ProblemDataFilename])

object ProblemData:
  def parse(raw: Option[String]): Either[String, ProblemData] =
    raw match
      case None => Right(ProblemData(None))
      case Some(value) =>
        val normalized = value.trim
        if normalized.isEmpty then Right(ProblemData(None))
        else ProblemDataFilename.parse(normalized).map(filename => ProblemData(Some(filename)))
