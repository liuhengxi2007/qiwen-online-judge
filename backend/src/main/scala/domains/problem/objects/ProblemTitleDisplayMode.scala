package domains.problem.objects

import io.circe.{Decoder, Encoder}


/** 题目标题展示策略；决定 UI 使用标题、slug 或二者组合。 */
enum ProblemTitleDisplayMode:
  case Title
  case Slug
  case TitleWithSlug

/** 题目标题展示策略的 JSON/数据库字符串编解码器。 */
object ProblemTitleDisplayMode:
  given Encoder[ProblemTitleDisplayMode] = Encoder.encodeString.contramap(encode)
  given Decoder[ProblemTitleDisplayMode] = Decoder.decodeString.emap(parse)

  /** 将外部字符串解析为展示策略。 */
  def parse(value: String): Either[String, ProblemTitleDisplayMode] =
    value.trim match
      case "title" => Right(ProblemTitleDisplayMode.Title)
      case "slug" => Right(ProblemTitleDisplayMode.Slug)
      case "title_with_slug" => Right(ProblemTitleDisplayMode.TitleWithSlug)
      case _ => Left("Problem title display mode must be one of: title, slug, title_with_slug.")

  private def encode(value: ProblemTitleDisplayMode): String =
    value match
      case ProblemTitleDisplayMode.Title => "title"
      case ProblemTitleDisplayMode.Slug => "slug"
      case ProblemTitleDisplayMode.TitleWithSlug => "title_with_slug"
