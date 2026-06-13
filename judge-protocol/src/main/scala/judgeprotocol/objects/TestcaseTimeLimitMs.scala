package judgeprotocol.objects

import io.circe.{Decoder, Encoder}

/** 单个测试点或工具的 CPU 时间限制，单位为毫秒。 */
final case class TestcaseTimeLimitMs(value: Int)

/** 校验时间限制，避免无界或非正资源配置进入 judger。 */
object TestcaseTimeLimitMs:
  /** 从协议整数构造时间限制；上限与问题配置校验保持一致。 */
  def parse(raw: Int): Either[String, TestcaseTimeLimitMs] =
    if raw < 1 then Left("Testcase time limit must be greater than 0.")
    else if raw > 600000 then Left("Testcase time limit must be at most 600000 ms.")
    else Right(TestcaseTimeLimitMs(raw))

  given Encoder[TestcaseTimeLimitMs] = Encoder.encodeInt.contramap(_.value)
  given Decoder[TestcaseTimeLimitMs] = Decoder.decodeInt.emap(parse)
