package domains.submission.utils

import domains.submission.objects.request.*
import shared.api.utils.PageRequestQuerySupport

/** 提交列表 query 参数解析器；缺失参数使用默认排序、方向或过滤器，非法参数返回错误。 */
object SubmissionListRequestQuery:
  /** 从 HTTP query 参数构造提交列表请求；分页解析委托给共享分页工具。 */
  def parse(queryParams: Map[String, String]): Either[String, SubmissionListRequest] =
    for
      sort <- parseOptional(queryParams, "sort", SubmissionSort.parse).map(_.getOrElse(SubmissionSort.Submitted))
      direction <- parseOptional(queryParams, "direction", SubmissionSortDirection.parse).map(_.getOrElse(SubmissionSort.defaultDirection(sort)))
      verdict <- parseOptional(queryParams, "verdict", SubmissionVerdictFilter.parse).map(_.getOrElse(SubmissionVerdictFilter.All))
      userQuery <- parseOptional(queryParams, "username", SubmissionUserQuery.parse)
      problemQuery <- parseOptional(queryParams, "problem", SubmissionProblemQuery.parse)
      pageRequest <- PageRequestQuerySupport.parsePageRequest(queryParams)
    yield SubmissionListRequest(
      userQuery = userQuery,
      problemQuery = problemQuery,
      verdict = verdict,
      sort = sort,
      direction = direction,
      pageRequest = pageRequest
    )

  private def parseOptional[A](
    queryParams: Map[String, String],
    key: String,
    parse: String => Either[String, A]
  ): Either[String, Option[A]] =
    queryParams.get(key).map(rawValue => parse(rawValue).map(Some(_))).getOrElse(Right(None))
