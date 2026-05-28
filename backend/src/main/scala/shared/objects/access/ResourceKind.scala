package shared.objects.access

import io.circe.{Decoder, Encoder}

enum ResourceKind:
  case Problem
  case ProblemSet

object ResourceKind:
  given Encoder[ResourceKind] = Encoder.encodeString.contramap(encode)
  given Decoder[ResourceKind] = Decoder.decodeString.emap(parse)

  def parse(value: String): Either[String, ResourceKind] =
    value.trim match
      case "problem" => Right(ResourceKind.Problem)
      case "problem_set" => Right(ResourceKind.ProblemSet)
      case _ => Left("Resource kind must be one of: problem, problem_set.")

  private def encode(value: ResourceKind): String =
    value match
      case ResourceKind.Problem => "problem"
      case ResourceKind.ProblemSet => "problem_set"
