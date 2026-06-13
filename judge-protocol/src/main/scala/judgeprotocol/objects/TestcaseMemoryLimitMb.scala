package judgeprotocol.objects

import io.circe.{Decoder, Encoder}

/** 单个测试点或工具的内存限制，单位为 MB。 */
final case class TestcaseMemoryLimitMb(value: Int)

/** 校验内存限制，确保传给 sandbox 的资源配置在预期范围内。 */
object TestcaseMemoryLimitMb:
  /** 从协议整数构造内存限制；失败时返回边界校验信息。 */
  def parse(raw: Int): Either[String, TestcaseMemoryLimitMb] =
    if raw < 1 then Left("Testcase memory limit must be greater than 0.")
    else if raw > 65536 then Left("Testcase memory limit must be at most 65536 MB.")
    else Right(TestcaseMemoryLimitMb(raw))

  given Encoder[TestcaseMemoryLimitMb] = Encoder.encodeInt.contramap(_.value)
  given Decoder[TestcaseMemoryLimitMb] = Decoder.decodeInt.emap(parse)
