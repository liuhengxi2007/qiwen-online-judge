package domains.blog.objects

import io.circe.{Decoder, Encoder}


/** 博客可见性枚举，Public 可被所有登录用户读取，Private 仅作者可见。 */
enum BlogVisibility:
  case Public
  case Private

/** 提供博客可见性的线格式 codec。 */
object BlogVisibility:
  given Encoder[BlogVisibility] = Encoder.encodeString.contramap(encode)
  given Decoder[BlogVisibility] = Decoder.decodeString.emap(parse)

  /** 解析数据库/API 线值，只接受 public 或 private。 */
  def parse(raw: String): Either[String, BlogVisibility] =
    raw match
      case "public" => Right(BlogVisibility.Public)
      case "private" => Right(BlogVisibility.Private)
      case _ => Left("Blog visibility must be public or private.")

  private def encode(value: BlogVisibility): String =
    value match
      case BlogVisibility.Public => "public"
      case BlogVisibility.Private => "private"
