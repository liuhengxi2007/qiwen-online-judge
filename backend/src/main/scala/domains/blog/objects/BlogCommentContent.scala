package domains.blog.objects



final case class BlogCommentContent(value: String)

object BlogCommentContent:
  def parse(raw: String): Either[String, BlogCommentContent] =
    val normalized = raw.trim
    if normalized.isEmpty then Left("Comment content is required.")
    else if normalized.length > 20000 then Left("Comment content must be at most 20000 characters.")
    else Right(BlogCommentContent(normalized))
