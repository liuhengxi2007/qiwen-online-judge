package domains.contest.objects

import io.circe.{Decoder, Encoder}

final case class ContestProblemAlias(value: String)

object ContestProblemAlias:
  given Encoder[ContestProblemAlias] = Encoder.encodeString.contramap(_.value)
  given Decoder[ContestProblemAlias] = Decoder.decodeString.emap(parse)

  private val aliasPattern = "^[A-Z][A-Z0-9]{0,7}$".r

  def parse(raw: String): Either[String, ContestProblemAlias] =
    val normalized = raw.trim.toUpperCase
    if normalized.isEmpty then Left("Contest problem alias is required.")
    else if !aliasPattern.matches(normalized) then Left("Contest problem alias must start with A-Z and contain at most 8 uppercase letters or digits.")
    else Right(ContestProblemAlias(normalized))

  def fromPosition(position: Int): ContestProblemAlias =
    ContestProblemAlias(toLetters(position))

  private def toLetters(position: Int): String =
    val zeroBased = math.max(position, 1) - 1
    def loop(value: Int, acc: String): String =
      val letter = ('A' + (value % 26)).toChar.toString
      val next = value / 26 - 1
      if next < 0 then letter + acc else loop(next, letter + acc)
    loop(zeroBased, "")
