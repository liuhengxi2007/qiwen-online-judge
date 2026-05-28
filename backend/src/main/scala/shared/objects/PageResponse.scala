package shared.objects

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class PageResponse[A](items: List[A], page: Int, pageSize: Int, totalItems: Long)

object PageResponse:
  given [A: Encoder]: Encoder[PageResponse[A]] = deriveEncoder[PageResponse[A]]
  given [A: Decoder]: Decoder[PageResponse[A]] = deriveDecoder[PageResponse[A]]
