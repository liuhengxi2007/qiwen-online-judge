package domains.problem.objects

import io.circe.{Decoder, Encoder}


enum OtherUserSubmissionAccess:
  case None
  case Summary
  case Detail

object OtherUserSubmissionAccess:
  given Encoder[OtherUserSubmissionAccess] = Encoder.encodeString.contramap(encode)
  given Decoder[OtherUserSubmissionAccess] = Decoder.decodeString.emap(parse)

  def parse(raw: String): Either[String, OtherUserSubmissionAccess] =
    raw.trim match
      case "none" => Right(OtherUserSubmissionAccess.None)
      case "summary" => Right(OtherUserSubmissionAccess.Summary)
      case "detail" => Right(OtherUserSubmissionAccess.Detail)
      case _ => Left("Other-user submission access must be one of: none, summary, detail.")

  private def encode(value: OtherUserSubmissionAccess): String =
    value match
      case OtherUserSubmissionAccess.None => "none"
      case OtherUserSubmissionAccess.Summary => "summary"
      case OtherUserSubmissionAccess.Detail => "detail"
