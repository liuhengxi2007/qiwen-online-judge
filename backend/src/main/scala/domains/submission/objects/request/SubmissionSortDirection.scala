package domains.submission.objects.request

import io.circe.{Decoder, Encoder}


enum SubmissionSortDirection:
  case Asc
  case Desc

object SubmissionSortDirection:
  given Encoder[SubmissionSortDirection] = Encoder.encodeString.contramap(encode)
  given Decoder[SubmissionSortDirection] = Decoder.decodeString.emap(parse)

  def parse(value: String): Either[String, SubmissionSortDirection] =
    value.trim match
      case "asc" => Right(SubmissionSortDirection.Asc)
      case "desc" => Right(SubmissionSortDirection.Desc)
      case _ => Left("Submission sort direction must be one of: asc, desc.")

  def encode(value: SubmissionSortDirection): String =
    value match
      case SubmissionSortDirection.Asc => "asc"
      case SubmissionSortDirection.Desc => "desc"
