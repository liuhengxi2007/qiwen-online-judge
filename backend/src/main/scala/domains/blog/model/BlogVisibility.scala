package domains.blog.model



enum BlogVisibility:
  case Public
  case Private

object BlogVisibility:
  def parse(raw: String): Either[String, BlogVisibility] =
    raw match
      case "public" => Right(BlogVisibility.Public)
      case "private" => Right(BlogVisibility.Private)
      case _ => Left("Blog visibility must be public or private.")
