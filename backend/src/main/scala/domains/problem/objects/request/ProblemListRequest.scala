package domains.problem.objects.request

import io.circe.{Decoder, Encoder, Json}
import io.circe.syntax.*

import shared.objects.PageRequest

/** 题目列表请求；包含可选搜索词和分页参数。 */
final case class ProblemListRequest(
  query: Option[ProblemSearchQuery],
  pageRequest: PageRequest
)

/** ProblemListRequest 的扁平 JSON 编解码器，page/pageSize 保持顶层字段。 */
object ProblemListRequest:
  given Encoder[ProblemListRequest] = Encoder.instance(request =>
    Json.obj(
      "query" -> request.query.asJson,
      "page" -> request.pageRequest.page.asJson,
      "pageSize" -> request.pageRequest.pageSize.asJson
    )
  )

  given Decoder[ProblemListRequest] = Decoder.instance { cursor =>
    for
      query <- cursor.downField("query").as[Option[ProblemSearchQuery]]
      page <- cursor.downField("page").as[Int]
      pageSize <- cursor.downField("pageSize").as[Int]
    yield ProblemListRequest(query = query, pageRequest = PageRequest(page = page, pageSize = pageSize))
  }
