package domains.blog.objects



final case class BlogCommentId(value: Long)

object BlogCommentId:
  def parse(raw: String): Either[String, BlogCommentId] =
    raw.toLongOption match
      case Some(value) if value > 0 => Right(BlogCommentId(value))
      case _ => Left("Blog comment id must be a positive integer.")
