package domains.blog.model



final case class BlogId(value: Long)

object BlogId:
  def parse(raw: String): Either[String, BlogId] =
    raw.toLongOption match
      case Some(value) if value > 0 => Right(BlogId(value))
      case _ => Left("Blog id must be a positive integer.")
