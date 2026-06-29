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

  /** 从 HTTP query 参数构造提交列表请求；非法分页和枚举过滤器回退默认值。 */
  def fromQueryParams(queryParams: Map[String, String]): Either[String, SubmissionListRequest] =
    val sort = parseOrDefault(queryParams, "sort", SubmissionSort.parse, SubmissionSort.Submitted)
    val direction = parseOrDefault(queryParams, "direction", SubmissionSortDirection.parse, SubmissionSort.defaultDirection(sort))
    val verdict = parseOrDefault(queryParams, "verdict", SubmissionVerdictFilter.parse, SubmissionVerdictFilter.All)
    val pageRequest = parsePageRequest(queryParams)

    for
      userQuery <- parseOptional(queryParams, "username", SubmissionUserQuery.parse)
      problemQuery <- parseOptional(queryParams, "problem", SubmissionProblemQuery.parse)
    yield SubmissionListRequest(
      userQuery = userQuery,
      problemQuery = problemQuery,
      verdict = verdict,
      sort = sort,
      direction = direction,
      pageRequest = pageRequest
    )

  private def parseOrDefault[A](
    queryParams: Map[String, String],
    key: String,
    parse: String => Either[String, A],
    defaultValue: A
  ): A =
    queryParams.get(key).flatMap(rawValue => parse(rawValue).toOption).getOrElse(defaultValue)

  private def parsePageRequest(queryParams: Map[String, String]): PageRequest =
    PageRequest(
      page = parsePositiveInt(queryParams.get("page"), defaultValue = 1),
      pageSize = parsePositiveInt(queryParams.get("pageSize"), defaultValue = 10)
    )

  private def parsePositiveInt(rawValue: Option[String], defaultValue: Int): Int =
    rawValue.flatMap(_.trim.toIntOption).filter(_ > 0).getOrElse(defaultValue)

  private def parseOptional[A](
    queryParams: Map[String, String],
    key: String,
    parse: String => Either[String, A]
  ): Either[String, Option[A]] =
    queryParams.get(key).map(rawValue => parse(rawValue).map(Some(_))).getOrElse(Right(None))
