package domains.shared.access



import io.circe.{Decoder, Encoder}

enum ResourceKind:
  case Problem
  case ProblemSet

object ResourceKind:
  def fromDatabase(value: String): Option[ResourceKind] =
    value match
      case "problem" => Some(ResourceKind.Problem)
      case "problem_set" => Some(ResourceKind.ProblemSet)
      case _ => None

  def toDatabase(value: ResourceKind): String =
    value match
      case ResourceKind.Problem => "problem"
      case ResourceKind.ProblemSet => "problem_set"

  given Encoder[ResourceKind] = Encoder.encodeString.contramap(toDatabase)
  given Decoder[ResourceKind] = Decoder.decodeString.emap { value =>
    fromDatabase(value).toRight(s"Unknown resource kind: $value")
  }
