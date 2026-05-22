package domains.problem.model



final case class ProblemDataFilename(value: String)

object ProblemDataFilename:
  def parse(raw: String): Either[String, ProblemDataFilename] =
    ProblemDataPath.parse(raw).flatMap { path =>
      if path.value.contains('/') then Left("Problem data file name must not contain directory separators.")
      else Right(ProblemDataFilename(path.value))
    }
