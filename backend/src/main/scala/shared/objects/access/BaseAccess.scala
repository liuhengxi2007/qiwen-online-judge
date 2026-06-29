package shared.objects.access

import io.circe.{Decoder, Encoder}

/** 资源基础可见性策略，决定没有显式授权时资源是否公开可见。 */
enum BaseAccess:
  case Restricted
  case Public

/** 负责基础访问策略与传输字符串之间的编解码。 */
object BaseAccess:
  given Encoder[BaseAccess] = Encoder.encodeString.contramap(encode)
  given Decoder[BaseAccess] = Decoder.decodeString.emap(parse)

  /** 解析 API 输入中的基础访问策略，兼容旧的 owner_only 表示但统一编码为 restricted。 */
  def parse(value: String): Either[String, BaseAccess] =
    value.trim match
      case "restricted" | "owner_only" => Right(BaseAccess.Restricted)
      case "public" => Right(BaseAccess.Public)
      case _ => Left("Base access must be one of: restricted, public.")

  private def encode(value: BaseAccess): String =
    value match
      case BaseAccess.Restricted => "restricted"
      case BaseAccess.Public => "public"
