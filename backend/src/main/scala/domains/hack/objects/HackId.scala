package domains.hack.objects

import io.circe.{Decoder, Encoder}

import scala.util.Try

final case class HackId(value: Long)

object HackId:
  given Encoder[HackId] = Encoder.encodeLong.contramap(_.value)
  given Decoder[HackId] = Decoder.decodeLong.emap { value =>
    if value < 1 then Left("Hack id is invalid.") else Right(HackId(value))
  }

  def parse(raw: String): Either[String, HackId] =
    Try(raw.trim.toLong)
      .toEither
      .left
      .map(_ => "Hack id is invalid.")
      .flatMap { value =>
        if value < 1 then Left("Hack id is invalid.")
        else Right(HackId(value))
      }
