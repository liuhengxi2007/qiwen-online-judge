package domains.shared.model

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class PageRequest(page: Int = 1, pageSize: Int = 20):
  def normalized: PageRequest =
    PageRequest(
      page = math.max(1, page),
      pageSize = math.max(1, pageSize)
    )

object PageRequest:
  given Encoder[PageRequest] = deriveEncoder[PageRequest]
  given Decoder[PageRequest] = deriveDecoder[PageRequest]

final case class PageResponse[A](items: List[A], page: Int, pageSize: Int, totalItems: Long)

object PageResponse:
  given [A: Encoder]: Encoder[PageResponse[A]] = deriveEncoder[PageResponse[A]]
  given [A: Decoder]: Decoder[PageResponse[A]] = deriveDecoder[PageResponse[A]]
