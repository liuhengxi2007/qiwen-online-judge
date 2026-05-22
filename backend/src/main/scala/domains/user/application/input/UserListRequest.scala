package domains.user.application.input

import domains.user.model.*

import domains.shared.model.PageRequest
import io.circe.{Decoder, Encoder, Json}
import io.circe.syntax.*

final case class UserListRequest(
  query: Option[UserSearchQuery],
  pageRequest: PageRequest
)

object UserListRequest:
  given Encoder[UserListRequest] = Encoder.instance(request =>
    Json.obj(
      "query" -> request.query.asJson,
      "page" -> request.pageRequest.page.asJson,
      "pageSize" -> request.pageRequest.pageSize.asJson
    )
  )

  given Decoder[UserListRequest] = Decoder.instance { cursor =>
    for
      query <- cursor.downField("query").as[Option[UserSearchQuery]]
      page <- cursor.downField("page").as[Int]
      pageSize <- cursor.downField("pageSize").as[Int]
    yield UserListRequest(query = query, pageRequest = PageRequest(page = page, pageSize = pageSize))
  }
