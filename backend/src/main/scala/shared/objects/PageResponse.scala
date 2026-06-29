package shared.objects

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

/** 通用分页响应，返回当前页项目、页码、页大小和全量数量。 */
final case class PageResponse[A](items: List[A], page: Int, pageSize: Int, totalItems: Long)

/** 提供分页响应的泛型 JSON 编解码。 */
object PageResponse:
  given [A: Encoder]: Encoder[PageResponse[A]] = deriveEncoder[PageResponse[A]]
  given [A: Decoder]: Decoder[PageResponse[A]] = deriveDecoder[PageResponse[A]]
