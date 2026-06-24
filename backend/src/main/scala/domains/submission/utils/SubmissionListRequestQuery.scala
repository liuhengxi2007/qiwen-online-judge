package domains.submission.utils

import domains.submission.objects.request.*
import shared.objects.PageRequest

/** 提交列表 query 参数解析器；展示筛选参数尽量回退默认值，文本筛选仍按领域规则校验。 */
object SubmissionListRequestQuery:
  /** 从 HTTP query 参数构造提交列表请求；分页、排序、方向和判题过滤器非法时回退默认值。 */
  def parse(queryParams: Map[String, String]): Either[String, SubmissionListRequest] =
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
