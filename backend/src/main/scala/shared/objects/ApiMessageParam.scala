package shared.objects

import io.circe.{Decoder, DecodingFailure, Encoder, Json}



/** API 消息参数的封闭类型，用于把可本地化错误或成功消息的动态值传给前端。 */
sealed trait ApiMessageParam

/** 提供 API 消息参数的带 kind 字段 JSON 编解码。 */
object ApiMessageParam:
  /** 文本型消息参数。 */
  final case class Text(value: String) extends ApiMessageParam
  /** Int 数值消息参数。 */
  final case class IntValue(value: Int) extends ApiMessageParam
  /** Long 数值消息参数。 */
  final case class LongValue(value: Long) extends ApiMessageParam
  /** Boolean 消息参数。 */
  final case class BoolValue(value: Boolean) extends ApiMessageParam

  given Encoder[ApiMessageParam] = Encoder.instance {
    case ApiMessageParam.Text(value) =>
      Json.obj("kind" -> Json.fromString("text"), "value" -> Json.fromString(value))
    case ApiMessageParam.IntValue(value) =>
      Json.obj("kind" -> Json.fromString("int"), "value" -> Json.fromInt(value))
    case ApiMessageParam.LongValue(value) =>
      Json.obj("kind" -> Json.fromString("long"), "value" -> Json.fromLong(value))
    case ApiMessageParam.BoolValue(value) =>
      Json.obj("kind" -> Json.fromString("bool"), "value" -> Json.fromBoolean(value))
  }

  given Decoder[ApiMessageParam] = Decoder.instance { cursor =>
    cursor.downField("kind").as[String].flatMap {
      case "text" => cursor.downField("value").as[String].map(ApiMessageParam.Text(_))
      case "int" => cursor.downField("value").as[Int].map(ApiMessageParam.IntValue(_))
      case "long" => cursor.downField("value").as[Long].map(ApiMessageParam.LongValue(_))
      case "bool" => cursor.downField("value").as[Boolean].map(ApiMessageParam.BoolValue(_))
      case other => Left(DecodingFailure(s"Unsupported ApiMessageParam kind: $other", cursor.history))
    }
  }
