package shared.access

enum ResourceKind:
  case Problem
  case ProblemSet

object ResourceKind:
  def parse(value: String): Either[String, ResourceKind] =
    value.trim match
      case "problem" => Right(ResourceKind.Problem)
      case "problem_set" => Right(ResourceKind.ProblemSet)
      case _ => Left("Resource kind must be one of: problem, problem_set.")
