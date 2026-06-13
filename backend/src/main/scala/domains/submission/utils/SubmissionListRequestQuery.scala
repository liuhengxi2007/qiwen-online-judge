package domains.submission.utils

import domains.submission.objects.request.*
import shared.api.utils.PageRequestQuerySupport

/** 提交列表 query 参数解析器；非法枚举值使用默认排序、方向或过滤器。 */
object SubmissionListRequestQuery:
  /** 从 HTTP query 参数构造提交列表请求；分页解析委托给共享分页工具。 */
  def parse(queryParams: Map[String, String]): SubmissionListRequest =
    // FIXME-CN: sort/direction/verdict/username/problem 的非法值会静默回默认或丢弃，调用方无法区分错误输入和默认筛选。
    val sort = queryParams
      .get("sort")
      .flatMap(rawSort => SubmissionSort.parse(rawSort).toOption)
      .getOrElse(SubmissionSort.Submitted)
    val direction = queryParams
      .get("direction")
      .flatMap(rawDirection => SubmissionSortDirection.parse(rawDirection).toOption)
      .getOrElse(SubmissionSort.defaultDirection(sort))
    val verdict = queryParams
      .get("verdict")
      .flatMap(rawVerdict => SubmissionVerdictFilter.parse(rawVerdict).toOption)
      .getOrElse(SubmissionVerdictFilter.All)

    SubmissionListRequest(
      userQuery = queryParams
        .get("username")
        .flatMap(rawQuery => SubmissionUserQuery.parse(rawQuery).toOption),
      problemQuery = queryParams
        .get("problem")
        .flatMap(rawQuery => SubmissionProblemQuery.parse(rawQuery).toOption),
      verdict = verdict,
      sort = sort,
      direction = direction,
      pageRequest = PageRequestQuerySupport.parsePageRequest(queryParams)
    )
