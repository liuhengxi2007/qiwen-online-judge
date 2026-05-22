package domains.problem.model



final case class ProblemTimeLimitMs(value: Int)

object ProblemTimeLimitMs:
  def parse(raw: Int): Either[String, ProblemTimeLimitMs] =
    if raw < 1 then Left("Problem time limit must be at least 1 ms.")
    else if raw > 600000 then Left("Problem time limit must be at most 600000 ms.")
    else Right(ProblemTimeLimitMs(raw))
