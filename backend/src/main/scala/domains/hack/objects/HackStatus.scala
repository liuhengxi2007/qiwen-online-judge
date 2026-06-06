package domains.hack.objects

import io.circe.{Decoder, Encoder}

enum HackStatus:
  case Queued
  case Running
  case Success
  case NoEffect
  case Invalid
  case Failed

object HackStatus:
  given Encoder[HackStatus] = Encoder.encodeString.contramap(encode)
  given Decoder[HackStatus] = Decoder.decodeString.emap(parse)

  def parse(value: String): Either[String, HackStatus] =
    value.trim match
      case "queued" => Right(HackStatus.Queued)
      case "running" => Right(HackStatus.Running)
      case "success" => Right(HackStatus.Success)
      case "no_effect" => Right(HackStatus.NoEffect)
      case "invalid" => Right(HackStatus.Invalid)
      case "failed" => Right(HackStatus.Failed)
      case _ => Left("Hack status must be one of: queued, running, success, no_effect, invalid, failed.")

  def encode(value: HackStatus): String =
    value match
      case HackStatus.Queued => "queued"
      case HackStatus.Running => "running"
      case HackStatus.Success => "success"
      case HackStatus.NoEffect => "no_effect"
      case HackStatus.Invalid => "invalid"
      case HackStatus.Failed => "failed"
