package domains.problem.objects

import io.circe.{Decoder, Encoder}


enum OthersSubmissionAccess:
  case None
  case Summary
  case Detail

object OthersSubmissionAccess:
  given Encoder[OthersSubmissionAccess] = Encoder.encodeString.contramap(encode)
  given Decoder[OthersSubmissionAccess] = Decoder.decodeString.emap(parse)

  def parse(raw: String): Either[String, OthersSubmissionAccess] =
    raw.trim match
      case "none" => Right(OthersSubmissionAccess.None)
      case "summary" => Right(OthersSubmissionAccess.Summary)
      case "detail" => Right(OthersSubmissionAccess.Detail)
      case _ => Left("Other-user submission access must be one of: none, summary, detail.")

  private def encode(value: OthersSubmissionAccess): String =
    value match
      case OthersSubmissionAccess.None => "none"
      case OthersSubmissionAccess.Summary => "summary"
      case OthersSubmissionAccess.Detail => "detail"
