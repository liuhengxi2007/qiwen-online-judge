package domains.problem.model



enum ProblemTitleDisplayMode:
  case Title
  case Slug
  case TitleWithSlug

object ProblemTitleDisplayMode:
  def parse(value: String): Either[String, ProblemTitleDisplayMode] =
    value.trim match
      case "title" => Right(ProblemTitleDisplayMode.Title)
      case "slug" => Right(ProblemTitleDisplayMode.Slug)
      case "title_with_slug" => Right(ProblemTitleDisplayMode.TitleWithSlug)
      case _ => Left("Problem title display mode must be one of: title, slug, title_with_slug.")

  def fromDatabase(value: String): Option[ProblemTitleDisplayMode] =
    value match
      case "title" => Some(ProblemTitleDisplayMode.Title)
      case "slug" => Some(ProblemTitleDisplayMode.Slug)
      case "title_with_slug" => Some(ProblemTitleDisplayMode.TitleWithSlug)
      case _ => None

  def toDatabase(value: ProblemTitleDisplayMode): String =
    value match
      case ProblemTitleDisplayMode.Title => "title"
      case ProblemTitleDisplayMode.Slug => "slug"
      case ProblemTitleDisplayMode.TitleWithSlug => "title_with_slug"
