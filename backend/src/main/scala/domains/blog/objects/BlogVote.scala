package domains.blog.objects



enum BlogVote:
  case Up
  case Down

object BlogVote:
  def parse(raw: String): Either[String, BlogVote] =
    raw match
      case "up" => Right(BlogVote.Up)
      case "down" => Right(BlogVote.Down)
      case _ => Left("Blog vote must be up or down.")
