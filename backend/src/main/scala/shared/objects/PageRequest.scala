package shared.objects

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

/** 通用分页请求，允许调用方传入页码和页大小并在使用前做下限归一化。 */
final case class PageRequest(page: Int = 1, pageSize: Int = 20):
  /** 将非法的非正页码/页大小压到最小值 1，不做上限裁剪。 */
  def normalized: PageRequest =
    PageRequest(
      page = math.max(1, page),
      pageSize = math.max(1, pageSize)
    )

/** 提供分页请求的 JSON 编解码。 */
object PageRequest:
  given Encoder[PageRequest] = deriveEncoder[PageRequest]
  given Decoder[PageRequest] = deriveDecoder[PageRequest]
