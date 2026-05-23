package shared.http.utils

import shared.model.PageRequest

object PageRequestQuerySupport:

  def parsePageRequest(
    queryParams: Map[String, String],
    defaultPage: Int = 1,
    defaultPageSize: Int = 10
  ): PageRequest =
    PageRequest(
      page = parsePositiveInt(queryParams.get("page"), defaultPage),
      pageSize = parsePositiveInt(queryParams.get("pageSize"), defaultPageSize)
    )

  private def parsePositiveInt(rawValue: Option[String], defaultValue: Int): Int =
    rawValue.flatMap(_.toIntOption).filter(_ > 0).getOrElse(defaultValue)
