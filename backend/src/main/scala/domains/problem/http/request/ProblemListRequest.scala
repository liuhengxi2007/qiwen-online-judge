package domains.problem.http.request

import domains.problem.model.*

import domains.shared.model.PageRequest
import io.circe.{Decoder, Encoder, Json}
import io.circe.syntax.*

final case class ProblemListRequest(
  query: Option[ProblemSearchQuery],
  pageRequest: PageRequest
)

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
