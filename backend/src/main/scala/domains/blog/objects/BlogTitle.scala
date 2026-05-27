package domains.blog.objects



final case class BlogTitle(value: String)

object BlogTitle:
  def parse(raw: String): Either[String, BlogTitle] =
    val normalized = raw.trim
    if normalized.isEmpty then Left("Blog title is required.")
    else if normalized.length > 160 then Left("Blog title must be at most 160 characters.")
    else Right(BlogTitle(normalized))
