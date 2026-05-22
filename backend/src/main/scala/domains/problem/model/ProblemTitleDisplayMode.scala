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
