package domains.user.objects.request

import io.circe.{Decoder, Encoder, Json}
import io.circe.syntax.*

import shared.objects.PageRequest

/** 管理端用户列表查询请求，包含可选搜索词和分页参数。 */
final case class UserListRequest(
  query: Option[UserSearchQuery],
  pageRequest: PageRequest
)

/** 提供用户列表查询请求的扁平 JSON 编解码。 */
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
