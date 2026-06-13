package domains.submission.objects.request

import io.circe.{Decoder, Encoder, Json}
import io.circe.syntax.*

import shared.objects.PageRequest

/** 提交列表请求；包含用户/题目过滤、结论过滤、排序和分页。 */
final case class SubmissionListRequest(
  userQuery: Option[SubmissionUserQuery],
  problemQuery: Option[SubmissionProblemQuery],
  verdict: SubmissionVerdictFilter,
  sort: SubmissionSort,
  direction: SubmissionSortDirection,
  pageRequest: PageRequest
)

/** SubmissionListRequest 的扁平 JSON 编解码器，page/pageSize 保持顶层字段。 */
object SubmissionListRequest:
  given Encoder[SubmissionListRequest] = Encoder.instance(request =>
    Json.obj(
      "userQuery" -> request.userQuery.asJson,
      "problemQuery" -> request.problemQuery.asJson,
      "verdict" -> request.verdict.asJson,
      "sort" -> request.sort.asJson,
      "direction" -> request.direction.asJson,
      "page" -> request.pageRequest.page.asJson,
      "pageSize" -> request.pageRequest.pageSize.asJson
    )
  )

  given Decoder[SubmissionListRequest] = Decoder.instance { cursor =>
    for
      userQuery <- cursor.downField("userQuery").as[Option[SubmissionUserQuery]]
      problemQuery <- cursor.downField("problemQuery").as[Option[SubmissionProblemQuery]]
      verdict <- cursor.downField("verdict").as[SubmissionVerdictFilter]
      sort <- cursor.downField("sort").as[SubmissionSort]
      direction <- cursor.downField("direction").as[SubmissionSortDirection]
      page <- cursor.downField("page").as[Int]
      pageSize <- cursor.downField("pageSize").as[Int]
    yield SubmissionListRequest(
      userQuery = userQuery,
      problemQuery = problemQuery,
      verdict = verdict,
      sort = sort,
      direction = direction,
      pageRequest = PageRequest(page = page, pageSize = pageSize)
    )
  }
