package domains.hack.objects

import io.circe.{Decoder, Encoder}

/** 可被 hack 的目标子任务运行模式；镜像判题任务 mode.type 的公开取值。 */
enum HackMode:
  case Traditional
  case Interactive

/** Hack 模式的 JSON 字符串编解码器。 */
object HackMode:
  given Encoder[HackMode] = Encoder.encodeString.contramap(encode)
  given Decoder[HackMode] = Decoder.decodeString.emap(parse)

  /** 将判题任务 mode.type 解析为受支持的 hack 展示模式。 */
  def parse(value: String): Either[String, HackMode] =
    value.trim match
      case "traditional" => Right(HackMode.Traditional)
      case "interactive" => Right(HackMode.Interactive)
      case _ => Left("Hack mode must be one of: traditional, interactive.")

  /** 将 Hack 模式编码为 JSON 字符串。 */
  def encode(value: HackMode): String =
    value match
      case HackMode.Traditional => "traditional"
      case HackMode.Interactive => "interactive"

  /** 构造已由 JudgeTaskBuilder 校验过的模式；未知值表示内部任务构建违规。 */
  def unsafeFromJudgeTaskMode(value: String): HackMode =
    parse(value).fold(message => throw IllegalArgumentException(message), identity)
